import { Router } from 'express';
import { z } from 'zod';
import multer from 'multer';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { ProductionOCRService } from '../../abstractions/ocr/OCRService.js';
import { LocalStorageProvider } from '../../abstractions/storage/StorageService.js';
import { pool } from '../../database/db.js';
import { inMemorySuppliers } from '../suppliers/suppliers.routes.js';

export const billsRouter = Router();

billsRouter.use(authenticateJwt);
billsRouter.use(requireTenant);

const ocrService = new ProductionOCRService();
const storageService = new LocalStorageProvider();
const upload = multer({ limits: { fileSize: 10 * 1024 * 1024 } }); // 10MB limit

export interface BillRecord {
  id: string;
  businessId: string;
  supplierId?: string;
  supplierName: string;
  invoiceNumber: string;
  invoiceDate: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  attachmentUrl?: string;
  verificationStatus: string;
  isDuplicate: boolean;
  notes?: string;
  items: { name: string; quantity: number; unitPrice: number; totalPrice: number }[];
  createdAt: string;
}

export const inMemoryBills = new Map<string, BillRecord[]>(); // businessId -> BillRecord[]

const createBillSchema = z.object({
  body: z.object({
    supplierName: z.string().min(1, 'Supplier name is required'),
    supplierId: z.string().optional(),
    invoiceNumber: z.string().min(1, 'Invoice number is required'),
    invoiceDate: z.string().min(10, 'Valid invoice date is required'),
    subtotal: z.number().min(0),
    taxAmount: z.number().min(0),
    totalAmount: z.number().positive('Total amount must be positive'),
    attachmentUrl: z.string().optional(),
    notes: z.string().optional(),
    items: z.array(
      z.object({
        name: z.string().min(1),
        quantity: z.number().positive(),
        unitPrice: z.number().min(0),
        totalPrice: z.number().min(0),
      })
    ).optional().default([]),
  }),
});

// GET /api/v1/bills
billsRouter.get('/', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let list: BillRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, supplier_id, supplier_name, invoice_number, invoice_date, subtotal, tax_amount, total_amount, attachment_url, verification_status, is_duplicate, notes, created_at FROM bills WHERE business_id = $1 ORDER BY invoice_date DESC, created_at DESC`,
          [businessId]
        );
        list = result.rows.map((row) => ({
          id: row.id,
          businessId: row.business_id,
          supplierId: row.supplier_id,
          supplierName: row.supplier_name || 'Supplier',
          invoiceNumber: row.invoice_number,
          invoiceDate: row.invoice_date,
          subtotal: parseFloat(row.subtotal),
          taxAmount: parseFloat(row.tax_amount),
          totalAmount: parseFloat(row.total_amount),
          attachmentUrl: row.attachment_url,
          verificationStatus: row.verification_status,
          isDuplicate: row.is_duplicate,
          notes: row.notes || '',
          items: [],
          createdAt: row.created_at,
        }));
      } catch (dbErr) {}
    }

    if (list.length === 0 && inMemoryBills.has(businessId)) {
      list = inMemoryBills.get(businessId)!;
    }

    res.json({ success: true, data: list });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/bills/scan-ocr
billsRouter.post('/scan-ocr', upload.single('billImage'), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let imageBuffer: Buffer = Buffer.from([]);
    let fileName = 'bill.jpg';
    let mimeType = 'image/jpeg';

    if (req.file) {
      imageBuffer = req.file.buffer;
      fileName = req.file.originalname;
      mimeType = req.file.mimetype;
    } else if (req.body && req.body.imageBase64) {
      imageBuffer = Buffer.from(req.body.imageBase64, 'base64');
    }

    // Upload & get attachment URL
    let attachmentUrl = '';
    if (imageBuffer.length > 0) {
      const uploadRes = await storageService.uploadFile(imageBuffer, fileName, mimeType);
      attachmentUrl = uploadRes.url;
    }

    // Get tenant suppliers for fuzzy matching
    const tenantSuppliers: { id: string; name: string }[] = [];
    if (inMemorySuppliers.has(businessId)) {
      inMemorySuppliers.get(businessId)!.forEach((s) => tenantSuppliers.push({ id: s.id, name: s.name }));
    }
    if (pool) {
      try {
        const suppRes = await pool.query(`SELECT id, name FROM suppliers WHERE business_id = $1`, [businessId]);
        suppRes.rows.forEach((r) => tenantSuppliers.push({ id: r.id, name: r.name }));
      } catch (dbErr) {}
    }

    // Process OCR
    const ocrResult = await ocrService.processBillImage(imageBuffer, fileName, tenantSuppliers);

    res.json({
      success: true,
      data: {
        ...ocrResult,
        attachmentUrl,
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/bills
billsRouter.post('/', validateRequest(createBillSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const userId = req.user?.userId;
    const { supplierName, supplierId, invoiceNumber, invoiceDate, subtotal, taxAmount, totalAmount, attachmentUrl, notes, items } = req.body;

    // 1. DUPLICATE INVOICE CHECK (business_id, supplier_name/id, invoice_number)
    let duplicateBill: BillRecord | undefined;
    if (inMemoryBills.has(businessId)) {
      duplicateBill = inMemoryBills.get(businessId)!.find(
        (b) => b.invoiceNumber.toLowerCase() === invoiceNumber.toLowerCase() && (b.supplierId === supplierId || b.supplierName.toLowerCase() === supplierName.toLowerCase())
      );
    }

    if (!duplicateBill && pool) {
      try {
        const dupRes = await pool.query(
          `SELECT id FROM bills WHERE business_id = $1 AND LOWER(invoice_number) = LOWER($2) AND (supplier_id = $3 OR LOWER(supplier_name) = LOWER($4)) LIMIT 1`,
          [businessId, invoiceNumber, supplierId || null, supplierName]
        );
        if (dupRes.rows.length > 0) {
          res.status(409).json({
            success: false,
            error: `409 DUPLICATE_INVOICE: Invoice '${invoiceNumber}' has already been saved for supplier '${supplierName}'.`,
            duplicateBillId: dupRes.rows[0].id,
          });
          return;
        }
      } catch (dbErr) {}
    }

    if (duplicateBill) {
      res.status(409).json({
        success: false,
        error: `409 DUPLICATE_INVOICE: Invoice '${invoiceNumber}' has already been saved for supplier '${supplierName}'.`,
        duplicateBillId: duplicateBill.id,
      });
      return;
    }

    const billId = `bill_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const now = new Date().toISOString();

    const newBill: BillRecord = {
      id: billId,
      businessId,
      supplierId,
      supplierName,
      invoiceNumber,
      invoiceDate,
      subtotal,
      taxAmount,
      totalAmount,
      attachmentUrl,
      verificationStatus: 'VERIFIED',
      isDuplicate: false,
      notes,
      items: items || [],
      createdAt: now,
    };

    if (!inMemoryBills.has(businessId)) {
      inMemoryBills.set(businessId, []);
    }
    inMemoryBills.get(businessId)!.unshift(newBill);

    // Save to DB in transaction & update supplier pending amount
    if (pool) {
      let client;
      try {
        client = await pool.connect();
        await client.query('BEGIN');

        let targetSupplierId = supplierId;
        if (!targetSupplierId) {
          const suppRes = await client.query(`SELECT id FROM suppliers WHERE business_id = $1 AND LOWER(name) = LOWER($2) LIMIT 1`, [businessId, supplierName]);
          if (suppRes.rows.length > 0) {
            targetSupplierId = suppRes.rows[0].id;
          } else {
            const newSuppRes = await client.query(
              `INSERT INTO suppliers (business_id, name, phone, total_purchases, pending_amount) VALUES ($1, $2, $3, $4, $4) RETURNING id`,
              [businessId, supplierName, '+91 98000 00000', totalAmount]
            );
            targetSupplierId = newSuppRes.rows[0].id;
          }
        } else {
          await client.query(
            `UPDATE suppliers SET total_purchases = total_purchases + $1, pending_amount = pending_amount + $1, last_purchase_at = NOW() WHERE id = $2 AND business_id = $3`,
            [totalAmount, targetSupplierId, businessId]
          );
        }

        // Insert Bill
        await client.query(
          `INSERT INTO bills (id, business_id, supplier_id, supplier_name, invoice_number, invoice_date, subtotal, tax_amount, total_amount, attachment_url, verification_status, is_duplicate, notes, created_by)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'VERIFIED', false, $11, $12)`,
          [billId, businessId, targetSupplierId, supplierName, invoiceNumber, invoiceDate, subtotal, taxAmount, totalAmount, attachmentUrl || null, notes || null, userId || null]
        );

        // Insert Bill Items
        for (const item of items) {
          await client.query(
            `INSERT INTO bill_items (bill_id, name, quantity, unit_price, total_price) VALUES ($1, $2, $3, $4, $5)`,
            [billId, item.name, item.quantity, item.unitPrice, item.totalPrice]
          );
        }

        // Insert Supplier Purchase Transaction
        const txId = `tx_bill_${billId}`;
        await client.query(
          `INSERT INTO transactions (id, business_id, type, amount, category_name, payment_method, transaction_date, note, supplier_id, bill_id, created_by)
           VALUES ($1, $2, 'SUPPLIER_PURCHASE', $3, 'Purchase', 'CASH', $4, $5, $6, $7, $8)`,
          [txId, businessId, totalAmount, invoiceDate, `Scanned Bill: ${invoiceNumber}`, targetSupplierId, billId, userId || null]
        );

        await client.query('COMMIT');
      } catch (dbTxErr) {
        if (client) await client.query('ROLLBACK');
      } finally {
        if (client) client.release();
      }
    }

    res.status(201).json({ success: true, data: newBill });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/bills/:id
billsRouter.get('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const billId = req.params.id;

    let bill: BillRecord | undefined;
    if (inMemoryBills.has(businessId)) {
      bill = inMemoryBills.get(businessId)!.find((b) => b.id === billId);
    }

    if (!bill && pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, supplier_id, supplier_name, invoice_number, invoice_date, subtotal, tax_amount, total_amount, attachment_url, verification_status, is_duplicate, notes, created_at FROM bills WHERE id = $1 AND business_id = $2`,
          [billId, businessId]
        );
        if (result.rows.length > 0) {
          const row = result.rows[0];
          bill = {
            id: row.id,
            businessId: row.business_id,
            supplierId: row.supplier_id,
            supplierName: row.supplier_name,
            invoiceNumber: row.invoice_number,
            invoiceDate: row.invoice_date,
            subtotal: parseFloat(row.subtotal),
            taxAmount: parseFloat(row.tax_amount),
            totalAmount: parseFloat(row.total_amount),
            attachmentUrl: row.attachment_url,
            verificationStatus: row.verification_status,
            isDuplicate: row.is_duplicate,
            notes: row.notes || '',
            items: [],
            createdAt: row.created_at,
          };
        }
      } catch (dbErr) {}
    }

    if (!bill) {
      res.status(404).json({ success: false, error: 'Bill not found or access denied' });
      return;
    }

    res.json({ success: true, data: bill });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/v1/bills/:id
billsRouter.delete('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const billId = req.params.id;

    if (inMemoryBills.has(businessId)) {
      const list = inMemoryBills.get(businessId)!;
      const idx = list.findIndex((b) => b.id === billId);
      if (idx !== -1) list.splice(idx, 1);
    }

    if (pool) {
      try {
        await pool.query(`DELETE FROM bills WHERE id = $1 AND business_id = $2`, [billId, businessId]);
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Bill deleted successfully' });
  } catch (err) {
    next(err);
  }
});
