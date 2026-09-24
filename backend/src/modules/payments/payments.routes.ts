import { Router } from 'express';
import { z } from 'zod';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { MockPaymentGateway } from '../../abstractions/payments/PaymentGateway.js';

export const paymentsRouter = Router();

paymentsRouter.use(authenticateJwt);
paymentsRouter.use(requireTenant);

const gateway = new MockPaymentGateway();

const createPaymentLinkSchema = z.object({
  body: z.object({
    amount: z.number().positive(),
    description: z.string().min(1),
    customerId: z.string().optional(),
  }),
});

// POST /api/v1/payments/create-link
paymentsRouter.post('/create-link', validateRequest(createPaymentLinkSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const paymentLink = await gateway.createPaymentLink({
      businessId: req.businessId!,
      amount: req.body.amount,
      description: req.body.description,
    });

    res.status(201).json({
      success: true,
      data: paymentLink,
    });
  } catch (err) {
    next(err);
  }
});
