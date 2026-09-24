import { Router } from 'express';
import { z } from 'zod';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { pool } from '../../database/db.js';
import { inMemoryTransactions, TransactionRecord } from '../transactions/transactions.routes.js';

export const suppliersRouter = Router();

suppliersRouter.use(authenticateJwt);
suppliersRouter.use(requireTenant);

export interface SupplierRecord {
  id: string;
  businessId: string;
  name: string;
  phone: string;
  address?: string;
  gstin?: string;
  totalPurchases: number;
  totalPaid: number;
  pendingAmount: number;
  lastPurchaseAt?: string;
  createdAt: string;
}

export const inMemorySuppliers = new Map<string, SupplierRecord[]>(); // businessId -> SupplierRecord[]

const createSupplierSchema = z.object({
  body: z.object({
    name: z.string().min(1, 'Supplier name is required'),
    phone: z.string().min(10, 'Valid 10-digit mobile number is required'),
    address: z.string().optional(),
    gstin: z.string().optional(),
  }),
});

const updateSupplierSchema = z.object({
  body: z.object({
    name: z.string().optional(),
    phone: z.string().optional(),
    address: z.string().optional(),
    gstin: z.string().optional(),
  }),
});

// GET /api/v1/suppliers
suppliersRouter.get('/', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let list: SupplierRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, name, phone, address, gstin, total_purchases, total_paid, pending_amount, last_purchase_at, created_at FROM suppliers WHERE business_id = $1 ORDER BY name ASC`,
          [businessId]
        );
        list = result.rows.map((row) => ({
          id: row.id,
          businessId: row.business_id,
          name: row.name,
          phone: row.phone,
          address: row.address || '',
          gstin: row.gstin || '',
          totalPurchases: parseFloat(row.total_purchases),
          totalPaid: parseFloat(row.total_paid),
          pendingAmount: parseFloat(row.pending_amount),
          lastPurchaseAt: row.last_purchase_at,
          createdAt: row.created_at,
        }));
      } catch (dbErr) {}
    }

    if (list.length === 0 && inMemorySuppliers.has(businessId)) {
      list = inMemorySuppliers.get(businessId)!;
    }

    res.json({ success: true, data: list });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/suppliers
suppliersRouter.post('/', validateRequest(createSupplierSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const { name, phone, address, gstin } = req.body;

    const suppId = `supp_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const now = new Date().toISOString();

    const newSupp: SupplierRecord = {
      id: suppId,
      businessId,
      name,
      phone,
      address: address || '',
      gstin: gstin || '',
      totalPurchases: 0,
      totalPaid: 0,
      pendingAmount: 0,
      createdAt: now,
    };

    if (!inMemorySuppliers.has(businessId)) {
      inMemorySuppliers.set(businessId, []);
    }
    inMemorySuppliers.get(businessId)!.unshift(newSupp);

    if (pool) {
      try {
        await pool.query(
          `INSERT INTO suppliers (id, business_id, name, phone, address, gstin) VALUES ($1, $2, $3, $4, $5, $6)`,
          [suppId, businessId, name, phone, address || null, gstin || null]
        );
      } catch (dbErr) {}
    }

    res.status(201).json({ success: true, data: newSupp });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/suppliers/:id
suppliersRouter.get('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const suppId = req.params.id;

    let supp: SupplierRecord | undefined;
    if (inMemorySuppliers.has(businessId)) {
      supp = inMemorySuppliers.get(businessId)!.find((s) => s.id === suppId);
    }

    if (!supp && pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, name, phone, address, gstin, total_purchases, total_paid, pending_amount, last_purchase_at, created_at FROM suppliers WHERE id = $1 AND business_id = $2`,
          [suppId, businessId]
        );
        if (result.rows.length > 0) {
          const row = result.rows[0];
          supp = {
            id: row.id,
            businessId: row.business_id,
            name: row.name,
            phone: row.phone,
            address: row.address || '',
            gstin: row.gstin || '',
            totalPurchases: parseFloat(row.total_purchases),
            totalPaid: parseFloat(row.total_paid),
            pendingAmount: parseFloat(row.pending_amount),
            lastPurchaseAt: row.last_purchase_at,
            createdAt: row.created_at,
          };
        }
      } catch (dbErr) {}
    }

    if (!supp) {
      res.status(404).json({ success: false, error: 'Supplier not found or access denied' });
      return;
    }

    res.json({ success: true, data: supp });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/suppliers/:id/transactions
suppliersRouter.get('/:id/transactions', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const suppId = req.params.id;

    let history: TransactionRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, type, amount, category_name, payment_method, transaction_date, transaction_time, note, supplier_id, created_at FROM transactions WHERE supplier_id = $1 AND business_id = $2 ORDER BY transaction_date DESC, created_at DESC`,
          [suppId, businessId]
        );
        history = result.rows.map((row) => ({
          id: row.id,
          businessId: row.business_id,
          type: row.type,
          amount: parseFloat(row.amount),
          categoryName: row.category_name,
          paymentMethod: row.payment_method,
          transactionDate: row.transaction_date,
          transactionTime: row.transaction_time || '10:30 AM',
          note: row.note || '',
          supplierId: row.supplier_id,
          createdAt: row.created_at,
        }));
      } catch (dbErr) {}
    }

    if (history.length === 0 && inMemoryTransactions.has(businessId)) {
      history = inMemoryTransactions.get(businessId)!.filter((t) => t.supplierId === suppId);
    }

    res.json({ success: true, data: history });
  } catch (err) {
    next(err);
  }
});

// PUT /api/v1/suppliers/:id
suppliersRouter.put('/:id', validateRequest(updateSupplierSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const suppId = req.params.id;
    const { name, phone, address, gstin } = req.body;

    if (inMemorySuppliers.has(businessId)) {
      const supp = inMemorySuppliers.get(businessId)!.find((s) => s.id === suppId);
      if (supp) {
        if (name) supp.name = name;
        if (phone) supp.phone = phone;
        if (address !== undefined) supp.address = address;
        if (gstin !== undefined) supp.gstin = gstin;
      }
    }

    if (pool) {
      try {
        await pool.query(
          `UPDATE suppliers SET name = COALESCE($1, name), phone = COALESCE($2, phone), address = COALESCE($3, address), gstin = COALESCE($4, gstin), updated_at = NOW() WHERE id = $5 AND business_id = $6`,
          [name || null, phone || null, address || null, gstin || null, suppId, businessId]
        );
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Supplier updated successfully' });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/v1/suppliers/:id
suppliersRouter.delete('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const suppId = req.params.id;

    if (inMemorySuppliers.has(businessId)) {
      const list = inMemorySuppliers.get(businessId)!;
      const idx = list.findIndex((s) => s.id === suppId);
      if (idx !== -1) list.splice(idx, 1);
    }

    if (pool) {
      try {
        await pool.query(`DELETE FROM suppliers WHERE id = $1 AND business_id = $2`, [suppId, businessId]);
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Supplier deleted successfully' });
  } catch (err) {
    next(err);
  }
});
