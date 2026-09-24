import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { env } from '../config/env.js';

export interface AuthUser {
  userId: string;
  phone: string;
  email?: string;
  activeBusinessId?: string;
  role?: string;
}

export interface AuthenticatedRequest extends Request {
  user?: AuthUser;
  businessId?: string;
}

export const authenticateJwt = (req: AuthenticatedRequest, res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401).json({ success: false, error: 'Unauthorized: Missing or invalid token' });
    return;
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, env.JWT_SECRET) as AuthUser;
    req.user = decoded;

    // Check if x-business-id header is provided, or default to activeBusinessId from JWT
    const headerBusinessId = req.headers['x-business-id'] as string;
    req.businessId = headerBusinessId || decoded.activeBusinessId;

    next();
  } catch (err) {
    res.status(401).json({ success: false, error: 'Unauthorized: Invalid or expired token' });
  }
};
