import { Router } from 'express';
import { z } from 'zod';
import jwt from 'jsonwebtoken';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant, inMemoryMemberships } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { inMemoryBusinesses, userBusinessList, StoredBusiness } from '../auth/auth.routes.js';
import { env } from '../../config/env.js';
import { pool } from '../../database/db.js';

export const businessRouter = Router();

businessRouter.use(authenticateJwt);

const updateProfileSchema = z.object({
  body: z.object({
    businessName: z.string().min(1, 'Business name is required'),
    owner: z.string().optional(),
    mobile: z.string().optional(),
    address: z.string().optional(),
    GSTIN: z.string().optional().or(z.literal('')), // Optional GSTIN for small businesses!
    logo: z.string().optional(),
    businessType: z.string().optional(),
  }),
});

const switchBusinessSchema = z.object({
  body: z.object({
    businessId: z.string().min(1, 'Target businessId is required'),
  }),
});

// GET /api/v1/business/profile
businessRouter.get('/profile', requireTenant, async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let profile = inMemoryBusinesses.get(businessId);

    if (!profile && pool) {
      try {
        const result = await pool.query(
          `SELECT id, name, owner_id, phone, address, gstin, logo_url, business_type FROM businesses WHERE id = $1`,
          [businessId]
        );
        if (result.rows.length > 0) {
          const row = result.rows[0];
          profile = {
            id: row.id,
            name: row.name,
            ownerId: row.owner_id,
            ownerName: 'Owner',
            phone: row.phone,
            address: row.address,
            gstin: row.gstin,
            businessType: row.business_type,
            logoUrl: row.logo_url,
          };
        }
      } catch (dbErr) {}
    }

    res.json({
      success: true,
      data: {
        id: businessId,
        businessName: profile?.name || 'Sharma General Store',
        owner: profile?.ownerName || 'Rahul Sharma',
        mobile: profile?.phone || '+91 98765 43210',
        address: profile?.address || 'Shop No. 12, Main Market, Jaipur',
        GSTIN: profile?.gstin || null, // Optional GSTIN
        logo: profile?.logoUrl || null,
        businessType: profile?.businessType || 'RETAIL',
        currency: 'INR',
      },
    });
  } catch (err) {
    next(err);
  }
});

// PUT /api/v1/business/profile
businessRouter.put('/profile', requireTenant, validateRequest(updateProfileSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const { businessName, owner, mobile, address, GSTIN, logo, businessType } = req.body;

    // Update in-memory
    let profile = inMemoryBusinesses.get(businessId);
    if (!profile) {
      profile = {
        id: businessId,
        name: businessName,
        ownerId: req.user?.userId || 'usr_default',
        ownerName: owner || 'Owner',
        phone: mobile,
        address,
        gstin: GSTIN || undefined,
        businessType: businessType || 'RETAIL',
        logoUrl: logo,
      };
      inMemoryBusinesses.set(businessId, profile);
    } else {
      profile.name = businessName;
      if (owner) profile.ownerName = owner;
      if (mobile) profile.phone = mobile;
      if (address) profile.address = address;
      profile.gstin = GSTIN || undefined;
      if (logo) profile.logoUrl = logo;
      if (businessType) profile.businessType = businessType;
    }

    // Update in DB if pool active
    if (pool) {
      try {
        await pool.query(
          `UPDATE businesses SET name = $1, phone = $2, address = $3, gstin = $4, logo_url = $5, business_type = $6, updated_at = NOW() WHERE id = $7`,
          [businessName, mobile || null, address || null, GSTIN || null, logo || null, businessType || 'RETAIL', businessId]
        );
      } catch (dbErr) {}
    }

    res.json({
      success: true,
      data: {
        id: businessId,
        businessName: profile.name,
        owner: profile.ownerName,
        mobile: profile.phone,
        address: profile.address,
        GSTIN: profile.gstin || null,
        logo: profile.logoUrl || null,
        businessType: profile.businessType,
      },
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/business/my-businesses
businessRouter.get('/my-businesses', async (req: AuthenticatedRequest, res, next) => {
  try {
    const userId = req.user?.userId!;
    const userBusinesses: any[] = [];

    const bIds = userBusinessList.get(userId) || [];
    for (const bId of bIds) {
      const b = inMemoryBusinesses.get(bId);
      if (b) {
        userBusinesses.push({ id: b.id, name: b.name, role: 'OWNER' });
      }
    }

    if (userBusinesses.length === 0 && pool) {
      try {
        const result = await pool.query(
          `SELECT b.id, b.name, bm.role FROM businesses b JOIN business_members bm ON b.id = bm.business_id WHERE bm.user_id = $1 AND bm.is_active = true`,
          [userId]
        );
        for (const row of result.rows) {
          userBusinesses.push({ id: row.id, name: row.name, role: row.role });
        }
      } catch (dbErr) {}
    }

    // If still empty, return active business
    if (userBusinesses.length === 0) {
      userBusinesses.push({
        id: req.user?.activeBusinessId || 'biz_default',
        name: 'Sharma General Store',
        role: req.user?.role || 'OWNER',
      });
    }

    res.json({
      success: true,
      data: userBusinesses,
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/business/switch
businessRouter.post('/switch', validateRequest(switchBusinessSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const userId = req.user?.userId!;
    const targetBusinessId = req.body.businessId;

    // Check membership
    const userMemberships = inMemoryMemberships.get(userId);
    let isMember = userMemberships && userMemberships.has(targetBusinessId);

    if (!isMember && pool) {
      try {
        const result = await pool.query(
          `SELECT role FROM business_members WHERE user_id = $1 AND business_id = $2 AND is_active = true`,
          [userId, targetBusinessId]
        );
        if (result.rows.length > 0) {
          isMember = true;
        }
      } catch (dbErr) {}
    }

    if (!isMember && targetBusinessId !== req.user?.activeBusinessId) {
      res.status(403).json({
        success: false,
        error: 'Access denied: You are not an active member of this business.',
      });
      return;
    }

    // Issue new JWT for active business
    const newToken = jwt.sign(
      {
        userId,
        phone: req.user?.phone,
        activeBusinessId: targetBusinessId,
        role: 'OWNER',
      },
      env.JWT_SECRET,
      { expiresIn: '7d' }
    );

    res.json({
      success: true,
      data: {
        token: newToken,
        activeBusinessId: targetBusinessId,
      },
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/business/staff
businessRouter.get('/staff', requireTenant, (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: [
      {
        id: 'mem_1',
        userId: req.user?.userId,
        fullName: 'Rahul Sharma',
        phone: req.user?.phone,
        role: 'OWNER',
        permissions: ['ALL'],
      },
    ],
  });
});
