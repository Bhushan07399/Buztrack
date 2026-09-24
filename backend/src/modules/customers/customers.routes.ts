import { Router } from 'express';
import { z } from 'zod';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { pool } from '../../database/db.js';
import { inMemoryTransactions, TransactionRecord } from '../transactions/transactions.routes.js';

export const customersRouter = Router();

customersRouter.use(authenticateJwt);
customersRouter.use(requireTenant);

export interface CustomerRecord {
  id: string;
  businessId: string;
  name: string;
  phone: string;
  address?: string;
  gstin?: string;
  totalCreditGiven: number;
  totalPaymentsReceived: number;
  pendingAmount: number;
  lastTransactionAt?: string;
  createdAt: string;
}

export const inMemoryCustomers = new Map<string, CustomerRecord[]>(); // businessId -> CustomerRecord[]

const createCustomerSchema = z.object({
  body: z.object({
    name: z.string().min(1, 'Customer name is required'),
    phone: z.string().min(10, 'Valid 10-digit mobile number is required'),
    address: z.string().optional(),
    gstin: z.string().optional(),
  }),
});

const updateCustomerSchema = z.object({
  body: z.object({
    name: z.string().optional(),
    phone: z.string().optional(),
    address: z.string().optional(),
    gstin: z.string().optional(),
  }),
});

// GET /api/v1/customers
customersRouter.get('/', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let list: CustomerRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, name, phone, address, gstin, total_credit_given, total_payments_received, pending_amount, last_transaction_at, created_at
           FROM customers WHERE business_id = $1 ORDER BY name ASC`,
          [businessId]
        );
        list = result.rows.map((row) => ({
          id: row.id,
          businessId: row.business_id,
          name: row.name,
          phone: row.phone,
          address: row.address || '',
          gstin: row.gstin || '',
          totalCreditGiven: parseFloat(row.total_credit_given),
          totalPaymentsReceived: parseFloat(row.total_payments_received),
          pendingAmount: parseFloat(row.pending_amount),
          lastTransactionAt: row.last_transaction_at,
          createdAt: row.created_at,
        }));
      } catch (dbErr) {}
    }

    if (list.length === 0 && inMemoryCustomers.has(businessId)) {
      list = inMemoryCustomers.get(businessId)!;
    }

    res.json({ success: true, data: list });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/customers
customersRouter.post('/', validateRequest(createCustomerSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const { name, phone, address, gstin } = req.body;

    const custId = `cust_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const now = new Date().toISOString();

    const newCust: CustomerRecord = {
      id: custId,
      businessId,
      name,
      phone,
      address: address || '',
      gstin: gstin || '',
      totalCreditGiven: 0,
      totalPaymentsReceived: 0,
      pendingAmount: 0,
      createdAt: now,
    };

    if (!inMemoryCustomers.has(businessId)) {
      inMemoryCustomers.set(businessId, []);
    }
    inMemoryCustomers.get(businessId)!.unshift(newCust);

    if (pool) {
      try {
        await pool.query(
          `INSERT INTO customers (id, business_id, name, phone, address, gstin) VALUES ($1, $2, $3, $4, $5, $6)`,
          [custId, businessId, name, phone, address || null, gstin || null]
        );
      } catch (dbErr) {}
    }

    res.status(201).json({ success: true, data: newCust });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/customers/:id
customersRouter.get('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const custId = req.params.id;

    let cust: CustomerRecord | undefined;
    if (inMemoryCustomers.has(businessId)) {
      cust = inMemoryCustomers.get(businessId)!.find((c) => c.id === custId);
    }

    if (!cust && pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, name, phone, address, gstin, total_credit_given, total_payments_received, pending_amount, last_transaction_at, created_at FROM customers WHERE id = $1 AND business_id = $2`,
          [custId, businessId]
        );
        if (result.rows.length > 0) {
          const row = result.rows[0];
          cust = {
            id: row.id,
            businessId: row.business_id,
            name: row.name,
            phone: row.phone,
            address: row.address || '',
            gstin: row.gstin || '',
            totalCreditGiven: parseFloat(row.total_credit_given),
            totalPaymentsReceived: parseFloat(row.total_payments_received),
            pendingAmount: parseFloat(row.pending_amount),
            lastTransactionAt: row.last_transaction_at,
            createdAt: row.created_at,
          };
        }
      } catch (dbErr) {}
    }

    if (!cust) {
      res.status(404).json({ success: false, error: 'Customer not found or access denied' });
      return;
    }

    res.json({ success: true, data: cust });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/customers/:id/transactions
customersRouter.get('/:id/transactions', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const custId = req.params.id;

    let history: TransactionRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, type, amount, category_name, payment_method, transaction_date, transaction_time, note, customer_id, created_at FROM transactions WHERE customer_id = $1 AND business_id = $2 ORDER BY transaction_date DESC, created_at DESC`,
          [custId, businessId]
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
          customerId: row.customer_id,
          createdAt: row.created_at,
        }));
      } catch (dbErr) {}
    }

    if (history.length === 0 && inMemoryTransactions.has(businessId)) {
      history = inMemoryTransactions.get(businessId)!.filter((t) => t.customerId === custId);
    }

    res.json({ success: true, data: history });
  } catch (err) {
    next(err);
  }
});

// PUT /api/v1/customers/:id
customersRouter.put('/:id', validateRequest(updateCustomerSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const custId = req.params.id;
    const { name, phone, address, gstin } = req.body;

    if (inMemoryCustomers.has(businessId)) {
      const cust = inMemoryCustomers.get(businessId)!.find((c) => c.id === custId);
      if (cust) {
        if (name) cust.name = name;
        if (phone) cust.phone = phone;
        if (address !== undefined) cust.address = address;
        if (gstin !== undefined) cust.gstin = gstin;
      }
    }

    if (pool) {
      try {
        await pool.query(
          `UPDATE customers SET name = COALESCE($1, name), phone = COALESCE($2, phone), address = COALESCE($3, address), gstin = COALESCE($4, gstin), updated_at = NOW() WHERE id = $5 AND business_id = $6`,
          [name || null, phone || null, address || null, gstin || null, custId, businessId]
        );
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Customer updated successfully' });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/v1/customers/:id
customersRouter.delete('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const custId = req.params.id;

    if (inMemoryCustomers.has(businessId)) {
      const list = inMemoryCustomers.get(businessId)!;
      const idx = list.findIndex((c) => c.id === custId);
      if (idx !== -1) list.splice(idx, 1);
    }

    if (pool) {
      try {
        await pool.query(`DELETE FROM customers WHERE id = $1 AND business_id = $2`, [custId, businessId]);
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Customer deleted successfully' });
  } catch (err) {
    next(err);
  }
});
