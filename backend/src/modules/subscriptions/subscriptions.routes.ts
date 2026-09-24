import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';

export const subscriptionsRouter = Router();

subscriptionsRouter.use(authenticateJwt);
subscriptionsRouter.use(requireTenant);

// GET /api/v1/subscriptions/current
subscriptionsRouter.get('/current', (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: {
      businessId: req.businessId,
      planCode: 'FREE',
      planName: 'Free Plan',
      status: 'ACTIVE',
      limits: {
        maxStaffMembers: 1,
        maxScannedBillsPerMonth: 10,
        scannedBillsUsedThisMonth: 4,
      },
      startDate: new Date().toISOString(),
      endDate: null,
    },
  });
});
