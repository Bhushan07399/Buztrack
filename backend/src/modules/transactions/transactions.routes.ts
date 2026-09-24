import { Router } from 'express';
import { z } from 'zod';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { pool } from '../../database/db.js';

export const transactionsRouter = Router();

transactionsRouter.use(authenticateJwt);
transactionsRouter.use(requireTenant);

export interface TransactionRecord {
  id: string;
  businessId: string;
  type: string;
  amount: number;
  categoryName: string;
  paymentMethod: string;
  transactionDate: string;
  transactionTime?: string;
  note?: string;
  customerId?: string;
  customerName?: string;
  supplierId?: string;
  supplierName?: string;
  accountId?: string;
  createdBy?: string;
  createdAt: string;
}

export const inMemoryTransactions = new Map<string, TransactionRecord[]>(); // businessId -> TransactionRecord[]

const createTransactionSchema = z.object({
  body: z.object({
    type: z.enum([
      'INCOME',
      'EXPENSE',
      'CUSTOMER_CREDIT',
      'CUSTOMER_PAYMENT',
      'SUPPLIER_PURCHASE',
      'SUPPLIER_PAYMENT',
      'TRANSFER_IN',
      'TRANSFER_OUT',
    ]),
    amount: z.number().positive('Amount must be positive'),
    categoryName: z.string().min(1, 'Category name is required'),
    paymentMethod: z.enum(['CASH', 'UPI', 'BANK', 'CARD']),
    transactionDate: z.string().min(10, 'Valid transaction date (YYYY-MM-DD) is required'),
    note: z.string().optional(),
    customerId: z.string().optional(),
    supplierId: z.string().optional(),
    accountId: z.string().optional(),
  }),
});

const updateTransactionSchema = z.object({
  body: z.object({
    amount: z.number().positive().optional(),
    categoryName: z.string().optional(),
    paymentMethod: z.enum(['CASH', 'UPI', 'BANK', 'CARD']).optional(),
    transactionDate: z.string().optional(),
    note: z.string().optional(),
  }),
});

// GET /api/v1/transactions
transactionsRouter.get('/', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const {
      page = '1',
      limit = '20',
      type,
      category,
      paymentMethod,
      customerId,
      supplierId,
      dateFrom,
      dateTo,
      search,
    } = req.query;

    const pageNum = parseInt(page as string, 10) || 1;
    const limitNum = parseInt(limit as string, 10) || 20;

    let items: TransactionRecord[] = [];
    let totalCount = 0;

    // Check DB first
    if (pool) {
      try {
        let whereClauses = ['business_id = $1'];
        let queryParams: any[] = [businessId];
        let paramIdx = 2;

        if (type) {
          whereClauses.push(`type = $${paramIdx++}`);
          queryParams.push(type);
        }
        if (category) {
          whereClauses.push(`category_name = $${paramIdx++}`);
          queryParams.push(category);
        }
        if (paymentMethod) {
          whereClauses.push(`payment_method = $${paramIdx++}`);
          queryParams.push(paymentMethod);
        }
        if (customerId) {
          whereClauses.push(`customer_id = $${paramIdx++}`);
          queryParams.push(customerId);
        }
        if (supplierId) {
          whereClauses.push(`supplier_id = $${paramIdx++}`);
          queryParams.push(supplierId);
        }
        if (dateFrom) {
          whereClauses.push(`transaction_date >= $${paramIdx++}`);
          queryParams.push(dateFrom);
        }
        if (dateTo) {
          whereClauses.push(`transaction_date <= $${paramIdx++}`);
          queryParams.push(dateTo);
        }
        if (search) {
          whereClauses.push(`(note ILIKE $${paramIdx} OR category_name ILIKE $${paramIdx})`);
          queryParams.push(`%${search}%`);
          paramIdx++;
        }

        const whereSql = whereClauses.join(' AND ');
        const countRes = await pool.query(`SELECT COUNT(*) FROM transactions WHERE ${whereSql}`, queryParams);
        totalCount = parseInt(countRes.rows[0].count, 10);

        const offset = (pageNum - 1) * limitNum;
        const dataRes = await pool.query(
          `SELECT t.id, t.business_id, t.type, t.amount, t.category_name, t.payment_method, t.transaction_date, t.transaction_time, t.note, t.customer_id, t.supplier_id, t.created_by, t.created_at, c.name as customer_name, s.name as supplier_name
           FROM transactions t
           LEFT JOIN customers c ON t.customer_id = c.id
           LEFT JOIN suppliers s ON t.supplier_id = s.id
           WHERE ${whereSql}
           ORDER BY t.transaction_date DESC, t.created_at DESC
           LIMIT $${paramIdx++} OFFSET $${paramIdx++}`,
          [...queryParams, limitNum, offset]
        );

        items = dataRes.rows.map((row) => ({
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
          customerName: row.customer_name,
          supplierId: row.supplier_id,
          supplierName: row.supplier_name,
          createdBy: row.created_by,
          createdAt: row.created_at,
        }));
      } catch (dbErr) {
        // Fallback to in-memory store
      }
    }

    if (items.length === 0 && inMemoryTransactions.has(businessId)) {
      let store = inMemoryTransactions.get(businessId)!;

      if (type) store = store.filter((t) => t.type === type);
      if (category) store = store.filter((t) => t.categoryName === category);
      if (paymentMethod) store = store.filter((t) => t.paymentMethod === paymentMethod);
      if (customerId) store = store.filter((t) => t.customerId === customerId);
      if (supplierId) store = store.filter((t) => t.supplierId === supplierId);
      if (dateFrom) store = store.filter((t) => t.transactionDate >= (dateFrom as string));
      if (dateTo) store = store.filter((t) => t.transactionDate <= (dateTo as string));
      if (search) {
        const q = (search as string).toLowerCase();
        store = store.filter((t) => t.note?.toLowerCase().includes(q) || t.categoryName.toLowerCase().includes(q));
      }

      totalCount = store.length;
      const start = (pageNum - 1) * limitNum;
      items = store.slice(start, start + limitNum);
    }

    res.json({
      success: true,
      data: items,
      pagination: {
        total: totalCount,
        page: pageNum,
        limit: limitNum,
        totalPages: Math.ceil(totalCount / limitNum) || 1,
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/transactions
transactionsRouter.post('/', validateRequest(createTransactionSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const userId = req.user?.userId;
    const { type, amount, categoryName, paymentMethod, transactionDate, note, customerId, supplierId, accountId } =
      req.body;

    const txId = `tx_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const now = new Date().toISOString();

    const newTx: TransactionRecord = {
      id: txId,
      businessId,
      type,
      amount,
      categoryName,
      paymentMethod,
      transactionDate,
      transactionTime: '10:30 AM',
      note: note || '',
      customerId,
      supplierId,
      accountId,
      createdBy: userId,
      createdAt: now,
    };

    // Save to in-memory store
    if (!inMemoryTransactions.has(businessId)) {
      inMemoryTransactions.set(businessId, []);
    }
    inMemoryTransactions.get(businessId)!.unshift(newTx);

    // Save to DB in transaction if pool active
    if (pool) {
      let client;
      try {
        client = await pool.connect();
        await client.query('BEGIN');

        await client.query(
          `INSERT INTO transactions (id, business_id, type, amount, category_name, payment_method, transaction_date, note, customer_id, supplier_id, created_by)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)`,
          [txId, businessId, type, amount, categoryName, paymentMethod, transactionDate, note || null, customerId || null, supplierId || null, userId || null]
        );

        // Update customer pending amount if applicable
        if (customerId) {
          if (type === 'CUSTOMER_CREDIT' || type === 'INCOME') {
            await client.query(
              `UPDATE customers SET total_credit_given = total_credit_given + $1, pending_amount = pending_amount + $1, last_transaction_at = NOW() WHERE id = $2 AND business_id = $3`,
              [amount, customerId, businessId]
            );
          } else if (type === 'CUSTOMER_PAYMENT') {
            await client.query(
              `UPDATE customers SET total_payments_received = total_payments_received + $1, pending_amount = pending_amount - $1, last_transaction_at = NOW() WHERE id = $2 AND business_id = $3`,
              [amount, customerId, businessId]
            );
          }
        }

        // Update supplier pending amount if applicable
        if (supplierId) {
          if (type === 'SUPPLIER_PURCHASE' || type === 'EXPENSE') {
            await client.query(
              `UPDATE suppliers SET total_purchases = total_purchases + $1, pending_amount = pending_amount + $1, last_purchase_at = NOW() WHERE id = $2 AND business_id = $3`,
              [amount, supplierId, businessId]
            );
          } else if (type === 'SUPPLIER_PAYMENT') {
            await client.query(
              `UPDATE suppliers SET total_paid = total_paid + $1, pending_amount = pending_amount - $1, last_purchase_at = NOW() WHERE id = $2 AND business_id = $3`,
              [amount, supplierId, businessId]
            );
          }
        }

        // Update Account Balance
        const isIncoming = ['INCOME', 'CUSTOMER_PAYMENT', 'TRANSFER_IN'].includes(type);
        const delta = isIncoming ? amount : -amount;
        await client.query(
          `UPDATE accounts SET balance = balance + $1, updated_at = NOW() WHERE business_id = $2 AND account_type = $3`,
          [delta, businessId, paymentMethod]
        );

        await client.query('COMMIT');
      } catch (dbTxErr) {
        if (client) await client.query('ROLLBACK');
      } finally {
        if (client) client.release();
      }
    }

    res.status(201).json({
      success: true,
      data: newTx,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/transactions/:id
transactionsRouter.get('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const txId = req.params.id;

    let tx: TransactionRecord | undefined;

    if (inMemoryTransactions.has(businessId)) {
      tx = inMemoryTransactions.get(businessId)!.find((t) => t.id === txId);
    }

    if (!tx && pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, type, amount, category_name, payment_method, transaction_date, transaction_time, note, customer_id, supplier_id, created_by, created_at FROM transactions WHERE id = $1 AND business_id = $2`,
          [txId, businessId]
        );
        if (result.rows.length > 0) {
          const row = result.rows[0];
          tx = {
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
            supplierId: row.supplier_id,
            createdBy: row.created_by,
            createdAt: row.created_at,
          };
        }
      } catch (dbErr) {}
    }

    if (!tx) {
      res.status(404).json({ success: false, error: 'Transaction not found or access denied' });
      return;
    }

    res.json({ success: true, data: tx });
  } catch (err) {
    next(err);
  }
});

// PUT /api/v1/transactions/:id
transactionsRouter.put('/:id', validateRequest(updateTransactionSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const txId = req.params.id;
    const { amount, categoryName, paymentMethod, transactionDate, note } = req.body;

    let tx: TransactionRecord | undefined;
    if (inMemoryTransactions.has(businessId)) {
      tx = inMemoryTransactions.get(businessId)!.find((t) => t.id === txId);
      if (tx) {
        if (amount !== undefined) tx.amount = amount;
        if (categoryName !== undefined) tx.categoryName = categoryName;
        if (paymentMethod !== undefined) tx.paymentMethod = paymentMethod;
        if (transactionDate !== undefined) tx.transactionDate = transactionDate;
        if (note !== undefined) tx.note = note;
      }
    }

    if (pool) {
      try {
        await pool.query(
          `UPDATE transactions SET amount = COALESCE($1, amount), category_name = COALESCE($2, category_name), payment_method = COALESCE($3, payment_method), transaction_date = COALESCE($4, transaction_date), note = COALESCE($5, note), updated_at = NOW() WHERE id = $6 AND business_id = $7`,
          [amount || null, categoryName || null, paymentMethod || null, transactionDate || null, note || null, txId, businessId]
        );
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Transaction updated successfully' });
  } catch (err) {
    next(err);
  }
});

// DELETE /api/v1/transactions/:id
transactionsRouter.delete('/:id', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const txId = req.params.id;

    if (inMemoryTransactions.has(businessId)) {
      const list = inMemoryTransactions.get(businessId)!;
      const idx = list.findIndex((t) => t.id === txId);
      if (idx !== -1) {
        list.splice(idx, 1);
      }
    }

    if (pool) {
      try {
        await pool.query(`DELETE FROM transactions WHERE id = $1 AND business_id = $2`, [txId, businessId]);
      } catch (dbErr) {}
    }

    res.json({ success: true, message: 'Transaction deleted successfully' });
  } catch (err) {
    next(err);
  }
});
