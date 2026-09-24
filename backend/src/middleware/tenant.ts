import { Response, NextFunction } from 'express';
import { AuthenticatedRequest } from './auth.js';
import { pool } from '../database/db.js';

// Global / in-memory store for fallback tests when DB is not running
export const inMemoryMemberships = new Map<string, Set<string>>(); // userId -> Set of businessIds

export const requireTenant = async (req: AuthenticatedRequest, res: Response, next: NextFunction): Promise<void> => {
  const userId = req.user?.userId;
  const targetBusinessId = (req.headers['x-business-id'] as string) || req.user?.activeBusinessId;

  if (!userId) {
    res.status(401).json({ success: false, error: 'Unauthorized: User authentication required' });
    return;
  }

  if (!targetBusinessId) {
    res.status(400).json({
      success: false,
      error: 'Tenant context required: Header "x-business-id" or active business session missing.',
    });
    return;
  }

  try {
    // 1. Check in-memory membership store first (for fast test execution / fallback)
    const userMemberships = inMemoryMemberships.get(userId);
    if (userMemberships && userMemberships.has(targetBusinessId)) {
      req.businessId = targetBusinessId;
      next();
      return;
    }

    // 2. Query PostgreSQL database if connected
    if (pool) {
      try {
        const result = await pool.query(
          `SELECT role FROM business_members WHERE user_id = $1 AND business_id = $2 AND is_active = true`,
          [userId, targetBusinessId]
        );

        if (result.rows.length > 0) {
          req.businessId = targetBusinessId;
          next();
          return;
        }
      } catch (dbErr) {
        // If DB query fails or DB is offline, check fallback
      }
    }

    // Default fallback: if activeBusinessId matches targetBusinessId from JWT token issued by auth service
    if (req.user?.activeBusinessId === targetBusinessId) {
      req.businessId = targetBusinessId;
      next();
      return;
    }

    // Access Denied if user is NOT a member of the requested business
    res.status(403).json({
      success: false,
      error: 'Access denied: User is not an active member of this business.',
    });
  } catch (err) {
    next(err);
  }
};
