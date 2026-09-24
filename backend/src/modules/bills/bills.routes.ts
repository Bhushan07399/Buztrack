import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { MockOCRProvider } from '../../abstractions/ocr/OCRService.js';

export const billsRouter = Router();

billsRouter.use(authenticateJwt);
billsRouter.use(requireTenant);

const ocrProvider = new MockOCRProvider();

// GET /api/v1/bills
billsRouter.get('/', (req: AuthenticatedRequest, res) => {
  res.json({
    success: true,
    data: [
      {
        id: 'bill_1',
        supplierName: 'Gupta Wholesale Mart',
        invoiceNumber: 'INV-9821',
        invoiceDate: '2026-03-01',
        subtotal: 4000.00,
        taxAmount: 500.00,
        totalAmount: 4500.00,
        verificationStatus: 'VERIFIED',
        isDuplicate: false,
        items: [
          { name: 'Wheat Flour 50kg', quantity: 2, unitPrice: 2000.00, totalPrice: 4000.00 },
        ],
      },
    ],
  });
});

// POST /api/v1/bills/scan-ocr
billsRouter.post('/scan-ocr', async (req: AuthenticatedRequest, res, next) => {
  try {
    const mockParsedBill = await ocrProvider.processBillImage(Buffer.from([]), 'bill.jpg');
    res.json({
      success: true,
      data: mockParsedBill,
    });
  } catch (err) {
    next(err);
  }
});
