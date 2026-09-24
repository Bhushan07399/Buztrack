import { Router } from 'express';
import { z } from 'zod';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { validateRequest } from '../../middleware/validate.js';
import { pool } from '../../database/db.js';
import { inMemoryTransactions } from '../transactions/transactions.routes.js';

export const cashbookRouter = Router();

cashbookRouter.use(authenticateJwt);
cashbookRouter.use(requireTenant);

export interface DailyClosingRecord {
  id: string;
  businessId: string;
  closingDate: string; // YYYY-MM-DD
  expectedIncome: number;
  expectedExpense: number;
  expectedCash: number;
  actualCash: number;
  difference: number;
  notes?: string;
  closedBy?: string;
  closedAt: string;
  isClosed: boolean;
}

export const inMemoryDailyClosings = new Map<string, DailyClosingRecord[]>(); // businessId -> DailyClosingRecord[]
export const inMemoryOpeningCash = new Map<string, Map<string, number>>(); // businessId -> (date -> amount)

const setOpeningCashSchema = z.object({
  body: z.object({
    date: z.string().min(10, 'Valid date (YYYY-MM-DD) is required'),
    openingCash: z.number().min(0, 'Opening cash must be non-negative'),
  }),
});

const closeDaySchema = z.object({
  body: z.object({
    date: z.string().min(10, 'Valid date (YYYY-MM-DD) is required'),
    actualCash: z.number().min(0, 'Actual cash must be non-negative'),
    notes: z.string().optional(),
  }),
});

// GET /api/v1/cashbook/summary
cashbookRouter.get('/summary', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const targetDate = (req.query.date as string) || new Date().toISOString().split('T')[0];

    let openingCash = 10000.0;
    let cashIn = 0.0;
    let cashOut = 0.0;

    if (inMemoryOpeningCash.has(businessId) && inMemoryOpeningCash.get(businessId)!.has(targetDate)) {
      openingCash = inMemoryOpeningCash.get(businessId)!.get(targetDate)!;
    }

    if (inMemoryTransactions.has(businessId)) {
      const txs = inMemoryTransactions.get(businessId)!.filter((t) => t.paymentMethod === 'CASH' && t.transactionDate === targetDate);
      for (const t of txs) {
        if (['INCOME', 'CUSTOMER_PAYMENT', 'TRANSFER_IN'].includes(t.type)) {
          cashIn += t.amount;
        } else if (['EXPENSE', 'SUPPLIER_PAYMENT', 'TRANSFER_OUT'].includes(t.type)) {
          cashOut += t.amount;
        }
      }
    }

    if (pool) {
      try {
        const txRes = await pool.query(
          `SELECT type, amount FROM transactions WHERE business_id = $1 AND payment_method = 'CASH' AND transaction_date = $2`,
          [businessId, targetDate]
        );
        cashIn = 0;
        cashOut = 0;
        for (const row of txRes.rows) {
          const amt = parseFloat(row.amount);
          if (['INCOME', 'CUSTOMER_PAYMENT', 'TRANSFER_IN'].includes(row.type)) {
            cashIn += amt;
          } else if (['EXPENSE', 'SUPPLIER_PAYMENT', 'TRANSFER_OUT'].includes(row.type)) {
            cashOut += amt;
          }
        }
      } catch (dbErr) {}
    }

    const expectedCash = openingCash + cashIn - cashOut;

    res.json({
      success: true,
      data: {
        date: targetDate,
        openingCash,
        cashIn,
        cashOut,
        expectedCash,
      },
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/cashbook
cashbookRouter.get('/', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const todayStr = new Date().toISOString().split('T')[0];

    let cashBalance = 15450.0;
    if (pool) {
      try {
        const accRes = await pool.query(`SELECT balance FROM accounts WHERE business_id = $1 AND account_type = 'CASH' LIMIT 1`, [businessId]);
        if (accRes.rows.length > 0) {
          cashBalance = parseFloat(accRes.rows[0].balance);
        }
      } catch (dbErr) {}
    }

    res.json({
      success: true,
      data: {
        currentCashBalance: cashBalance,
        todayDate: todayStr,
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/cashbook/opening
cashbookRouter.post('/opening', validateRequest(setOpeningCashSchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const { date, openingCash } = req.body;

    if (!inMemoryOpeningCash.has(businessId)) {
      inMemoryOpeningCash.set(businessId, new Map());
    }
    inMemoryOpeningCash.get(businessId)!.set(date, openingCash);

    res.status(200).json({
      success: true,
      data: { date, openingCash },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/v1/cashbook/close
cashbookRouter.post('/close', validateRequest(closeDaySchema), async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const userId = req.user?.userId;
    const { date, actualCash, notes } = req.body;

    // Check duplicate closing for same date
    if (inMemoryDailyClosings.has(businessId)) {
      const existing = inMemoryDailyClosings.get(businessId)!.find((c) => c.closingDate === date);
      if (existing) {
        res.status(409).json({
          success: false,
          error: `Daily closing for ${date} has already been recorded for this business.`,
        });
        return;
      }
    }

    if (pool) {
      try {
        const checkRes = await pool.query(`SELECT id FROM daily_closings WHERE business_id = $1 AND closing_date = $2`, [businessId, date]);
        if (checkRes.rows.length > 0) {
          res.status(409).json({
            success: false,
            error: `Daily closing for ${date} has already been recorded for this business.`,
          });
          return;
        }
      } catch (dbErr) {}
    }

    // Calculate server-side expected cash
    let openingCash = 10000.0;
    let cashIn = 0.0;
    let cashOut = 0.0;

    if (inMemoryOpeningCash.has(businessId) && inMemoryOpeningCash.get(businessId)!.has(date)) {
      openingCash = inMemoryOpeningCash.get(businessId)!.get(date)!;
    }

    if (inMemoryTransactions.has(businessId)) {
      const txs = inMemoryTransactions.get(businessId)!.filter((t) => t.paymentMethod === 'CASH' && t.transactionDate === date);
      for (const t of txs) {
        if (['INCOME', 'CUSTOMER_PAYMENT', 'TRANSFER_IN'].includes(t.type)) cashIn += t.amount;
        else if (['EXPENSE', 'SUPPLIER_PAYMENT', 'TRANSFER_OUT'].includes(t.type)) cashOut += t.amount;
      }
    }

    if (pool) {
      try {
        const txRes = await pool.query(
          `SELECT type, amount FROM transactions WHERE business_id = $1 AND payment_method = 'CASH' AND transaction_date = $2`,
          [businessId, date]
        );
        cashIn = 0;
        cashOut = 0;
        for (const row of txRes.rows) {
          const amt = parseFloat(row.amount);
          if (['INCOME', 'CUSTOMER_PAYMENT', 'TRANSFER_IN'].includes(row.type)) cashIn += amt;
          else if (['EXPENSE', 'SUPPLIER_PAYMENT', 'TRANSFER_OUT'].includes(row.type)) cashOut += amt;
        }
      } catch (dbErr) {}
    }

    const expectedCash = openingCash + cashIn - cashOut;
    const difference = actualCash - expectedCash;

    const closingId = `dc_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
    const now = new Date().toISOString();

    const record: DailyClosingRecord = {
      id: closingId,
      businessId,
      closingDate: date,
      expectedIncome: cashIn,
      expectedExpense: cashOut,
      expectedCash,
      actualCash,
      difference,
      notes: notes || '',
      closedBy: userId,
      closedAt: now,
      isClosed: true,
    };

    if (!inMemoryDailyClosings.has(businessId)) {
      inMemoryDailyClosings.set(businessId, []);
    }
    inMemoryDailyClosings.get(businessId)!.unshift(record);

    if (pool) {
      try {
        await pool.query(
          `INSERT INTO daily_closings (id, business_id, closing_date, expected_income, expected_expense, expected_cash, actual_cash, difference, notes, closed_by)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
          [closingId, businessId, date, cashIn, cashOut, expectedCash, actualCash, difference, notes || null, userId || null]
        );
      } catch (dbErr) {}
    }

    res.status(201).json({
      success: true,
      data: record,
    });
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/cashbook/closings
cashbookRouter.get('/closings', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    let list: DailyClosingRecord[] = [];

    if (pool) {
      try {
        const result = await pool.query(
          `SELECT id, business_id, closing_date, expected_income, expected_expense, expected_cash, actual_cash, difference, notes, closed_by, closed_at FROM daily_closings WHERE business_id = $1 ORDER BY closing_date DESC`,
          [businessId]
        );
        list = result.rows.map((row) => ({
          id: row.id,
          businessId: row.business_id,
          closingDate: row.closing_date,
          expectedIncome: parseFloat(row.expected_income),
          expectedExpense: parseFloat(row.expected_expense),
          expectedCash: parseFloat(row.expected_cash),
          actualCash: parseFloat(row.actual_cash),
          difference: parseFloat(row.difference),
          notes: row.notes || '',
          closedBy: row.closed_by,
          closedAt: row.closed_at,
          isClosed: true,
        }));
      } catch (dbErr) {}
    }

    if (list.length === 0 && inMemoryDailyClosings.has(businessId)) {
      list = inMemoryDailyClosings.get(businessId)!;
    }

    res.json({
      success: true,
      data: list,
    });
  } catch (err) {
    next(err);
  }
});
