import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';

export const accountsRouter = Router();

accountsRouter.use(authenticateJwt);
accountsRouter.use(requireTenant);

// GET /api/v1/accounts
accountsRouter.get('/', (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: [
      { id: 'acc_cash', name: 'Cash in Hand', accountType: 'CASH', balance: 15450.00, isDefault: true },
      { id: 'acc_upi', name: 'GPay / PhonePe UPI', accountType: 'UPI', balance: 28500.00, isDefault: false },
      { id: 'acc_bank', name: 'HDFC Bank Account', accountType: 'BANK', balance: 145000.00, isDefault: false },
      { id: 'acc_card', name: 'POS Card Machine', accountType: 'CARD', balance: 8200.00, isDefault: false },
    ],
  });
});
