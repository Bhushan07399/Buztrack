import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';

export const remindersRouter = Router();

remindersRouter.use(authenticateJwt);
remindersRouter.use(requireTenant);

// GET /api/v1/reminders
remindersRouter.get('/', (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: [
      {
        id: 'rem_1',
        type: 'CUSTOMER_DUE',
        title: 'Collect Udhari from Suresh Patel',
        message: 'Pending balance Rs. 8,000 due today',
        dueDate: new Date().toISOString().split('T')[0],
        status: 'PENDING',
      },
    ],
  });
});
