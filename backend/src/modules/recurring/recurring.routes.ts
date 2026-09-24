import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';

export const recurringRouter = Router();

recurringRouter.use(authenticateJwt);
recurringRouter.use(requireTenant);

// GET /api/v1/recurring
recurringRouter.get('/', (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: [
      {
        id: 'rec_1',
        title: 'Shop Rent',
        type: 'EXPENSE',
        categoryName: 'Rent',
        amount: 15000.00,
        frequency: 'MONTHLY',
        nextDueDate: '2026-04-01',
        isActive: true,
      },
      {
        id: 'rec_2',
        title: 'Electricity Bill',
        type: 'EXPENSE',
        categoryName: 'Electricity',
        amount: 3200.00,
        frequency: 'MONTHLY',
        nextDueDate: '2026-04-05',
        isActive: true,
      },
    ],
  });
});
