import { Router } from 'express';
import { z } from 'zod';
import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';
import { env } from '../../config/env.js';
import { validateRequest } from '../../middleware/validate.js';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { pool } from '../../database/db.js';
import { inMemoryMemberships } from '../../middleware/tenant.js';

export const authRouter = Router();

// In-memory fallback user & business store for instant testing / offline mode
export interface StoredUser {
  id: string;
  fullName: string;
  phone: string;
  email?: string;
  passwordHash: string;
}

export interface StoredBusiness {
  id: string;
  name: string;
  ownerId: string;
  ownerName: string;
  phone?: string;
  address?: string;
  gstin?: string;
  businessType: string;
  logoUrl?: string;
}

export const inMemoryUsers = new Map<string, StoredUser>(); // phone -> StoredUser
export const inMemoryBusinesses = new Map<string, StoredBusiness>(); // businessId -> StoredBusiness
export const userBusinessList = new Map<string, string[]>(); // userId -> businessIds[]

const registerSchema = z.object({
  body: z.object({
    fullName: z.string().min(2, 'Full name is required'),
    phone: z.string().min(10, 'Valid 10-digit phone number is required'),
    email: z.string().email().optional().or(z.literal('')),
    password: z.string().min(6, 'Password must be at least 6 characters'),
    businessName: z.string().min(2, 'Business name is required'),
    businessType: z.string().optional().default('RETAIL'),
  }),
});

const loginSchema = z.object({
  body: z.object({
    phone: z.string().min(10, 'Valid 10-digit phone number is required'),
    password: z.string().min(1, 'Password is required'),
  }),
});

// POST /api/v1/auth/register
authRouter.post('/register', validateRequest(registerSchema), async (req, res, next) => {
  try {
    const { fullName, phone, email, password, businessName, businessType } = req.body;

    // 1. Check duplicate user in DB or in-memory
    if (inMemoryUsers.has(phone)) {
      res.status(409).json({ success: false, error: 'User with this phone number already exists' });
      return;
    }

    if (pool) {
      try {
        const existing = await pool.query('SELECT id FROM users WHERE phone = $1', [phone]);
        if (existing.rows.length > 0) {
          res.status(409).json({ success: false, error: 'User with this phone number already exists' });
          return;
        }
      } catch (dbErr) {
        // Fallback if DB is not active
      }
    }

    // 2. Hash password securely
    const passwordHash = await bcrypt.hash(password, 10);
    const userId = `usr_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const businessId = `biz_${Date.now()}_${Math.floor(Math.random() * 1000)}`;

    // 3. Save to In-Memory store
    const newUser: StoredUser = {
      id: userId,
      fullName,
      phone,
      email: email || undefined,
      passwordHash,
    };
    inMemoryUsers.set(phone, newUser);

    const newBusiness: StoredBusiness = {
      id: businessId,
      name: businessName,
      ownerId: userId,
      ownerName: fullName,
      phone,
      businessType: businessType || 'RETAIL',
    };
    inMemoryBusinesses.set(businessId, newBusiness);

    // Track user -> business membership
    if (!userBusinessList.has(userId)) {
      userBusinessList.set(userId, []);
    }
    userBusinessList.get(userId)!.push(businessId);

    if (!inMemoryMemberships.has(userId)) {
      inMemoryMemberships.set(userId, new Set());
    }
    inMemoryMemberships.get(userId)!.add(businessId);

    // 4. Save to PostgreSQL inside Transaction if DB available
    if (pool) {
      let client;
      try {
        client = await pool.connect();
        await client.query('BEGIN');

        // Insert User
        await client.query(
          `INSERT INTO users (id, phone, email, password_hash, full_name) VALUES ($1, $2, $3, $4, $5)`,
          [userId, phone, email || null, passwordHash, fullName]
        );

        // Insert Business
        await client.query(
          `INSERT INTO businesses (id, name, owner_id, phone, business_type) VALUES ($1, $2, $3, $4, $5)`,
          [businessId, businessName, userId, phone, businessType || 'RETAIL']
        );

        // Insert Business Member OWNER
        await client.query(
          `INSERT INTO business_members (business_id, user_id, role, permissions) VALUES ($1, $2, 'OWNER', '{"all": true}'::jsonb)`,
          [businessId, userId]
        );

        // Insert Default Accounts (Cash, UPI, Bank, Card)
        const accounts = [
          { name: 'Cash in Hand', type: 'CASH', isDefault: true },
          { name: 'UPI / Online', type: 'UPI', isDefault: false },
          { name: 'Bank Account', type: 'BANK', isDefault: false },
          { name: 'Card POS', type: 'CARD', isDefault: false },
        ];

        for (const acc of accounts) {
          await client.query(
            `INSERT INTO accounts (business_id, name, account_type, balance, is_default) VALUES ($1, $2, $3, 0.00, $4)`,
            [businessId, acc.name, acc.type, acc.isDefault]
          );
        }

        // Insert Default FREE Subscription
        const planResult = await client.query(`SELECT id FROM subscription_plans WHERE code = 'FREE' LIMIT 1`);
        if (planResult.rows.length > 0) {
          const planId = planResult.rows[0].id;
          await client.query(
            `INSERT INTO subscriptions (business_id, plan_id, status) VALUES ($1, $2, 'ACTIVE')`,
            [businessId, planId]
          );
        }

        await client.query('COMMIT');
      } catch (dbTxErr) {
        if (client) await client.query('ROLLBACK');
      } finally {
        if (client) client.release();
      }
    }

    // 5. Generate JWT Token
    const token = jwt.sign(
      {
        userId,
        phone,
        activeBusinessId: businessId,
        role: 'OWNER',
      },
      env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.status(201).json({
      success: true,
      data: {
        token,
        user: {
          id: userId,
          fullName,
          phone,
          email: email || null,
        },
        business: {
          id: businessId,
          name: businessName,
          role: 'OWNER',
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/auth/login
authRouter.post('/login', validateRequest(loginSchema), async (req, res, next) => {
  try {
    const { phone, password } = req.body;

    let user: StoredUser | undefined = inMemoryUsers.get(phone);
    let userId = user?.id;
    let fullName = user?.fullName;
    let passwordHash = user?.passwordHash;

    // Check DB if user not in memory
    if (!user && pool) {
      try {
        const result = await pool.query(`SELECT id, phone, email, password_hash, full_name FROM users WHERE phone = $1`, [phone]);
        if (result.rows.length > 0) {
          const row = result.rows[0];
          userId = row.id;
          fullName = row.full_name;
          passwordHash = row.password_hash;
        }
      } catch (dbErr) {
        // Fallback
      }
    }

    if (!passwordHash || !userId) {
      res.status(401).json({ success: false, error: 'Invalid phone number or password' });
      return;
    }

    // Verify Password Hash
    const isPasswordValid = await bcrypt.compare(password, passwordHash);
    if (!isPasswordValid) {
      res.status(401).json({ success: false, error: 'Invalid phone number or password' });
      return;
    }

    // Determine Active Business
    const bIds = userBusinessList.get(userId) || [];
    let activeBusinessId = bIds[0] || `biz_default_${userId}`;
    let activeBusinessName = 'My Business';

    if (inMemoryBusinesses.has(activeBusinessId)) {
      activeBusinessName = inMemoryBusinesses.get(activeBusinessId)!.name;
    } else if (pool) {
      try {
        const bRes = await pool.query(
          `SELECT b.id, b.name FROM businesses b JOIN business_members bm ON b.id = bm.business_id WHERE bm.user_id = $1 LIMIT 1`,
          [userId]
        );
        if (bRes.rows.length > 0) {
          activeBusinessId = bRes.rows[0].id;
          activeBusinessName = bRes.rows[0].name;
        }
      } catch (dbErr) {}
    }

    // Ensure user-business association is updated
    if (!inMemoryMemberships.has(userId)) {
      inMemoryMemberships.set(userId, new Set());
    }
    inMemoryMemberships.get(userId)!.add(activeBusinessId);

    // Issue JWT Token
    const token = jwt.sign(
      {
        userId,
        phone,
        activeBusinessId,
        role: 'OWNER',
      },
      env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      success: true,
      data: {
        token,
        user: {
          id: userId,
          fullName: fullName || 'Shop Owner',
          phone,
        },
        business: {
          id: activeBusinessId,
          name: activeBusinessName,
          role: 'OWNER',
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/auth/me
authRouter.get('/me', authenticateJwt, async (req: AuthenticatedRequest, res, next) => {
  try {
    const userId = req.user?.userId;
    const phone = req.user?.phone;
    const activeBusinessId = req.businessId || req.user?.activeBusinessId;

    let businessName = 'My Business';
    if (activeBusinessId && inMemoryBusinesses.has(activeBusinessId)) {
      businessName = inMemoryBusinesses.get(activeBusinessId)!.name;
    }

    res.json({
      success: true,
      data: {
        user: {
          id: userId,
          phone,
        },
        activeBusiness: {
          id: activeBusinessId,
          name: businessName,
          role: req.user?.role || 'OWNER',
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/auth/logout
authRouter.post('/logout', authenticateJwt, (_req, res) => {
  res.json({
    success: true,
    message: 'Logged out successfully',
  });
});
