import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { pool } from '../../database/db.js';
import { inMemoryTransactions } from '../transactions/transactions.routes.js';
import { inMemoryCustomers } from '../customers/customers.routes.js';
import { inMemorySuppliers } from '../suppliers/suppliers.routes.js';

export const reportsRouter = Router();

reportsRouter.use(authenticateJwt);
reportsRouter.use(requireTenant);

// GET /api/v1/reports/today-summary
reportsRouter.get('/today-summary', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const todayStr = new Date().toISOString().split('T')[0];

    let totalIncome = 0.0;
    let totalExpenses = 0.0;
    let cashIncome = 0.0;
    let upiIncome = 0.0;
    let cardIncome = 0.0;
    let bankIncome = 0.0;
    let cashExpense = 0.0;
    let creditGivenToday = 0.0;
    let pendingCustomerCollectionTotal = 0.0;
    let pendingSupplierTotal = 0.0;

    // Calculate in-memory
    if (inMemoryTransactions.has(businessId)) {
      const todayTxs = inMemoryTransactions.get(businessId)!.filter((t) => t.transactionDate === todayStr);
      for (const t of todayTxs) {
        if (['INCOME', 'CUSTOMER_PAYMENT'].includes(t.type)) {
          totalIncome += t.amount;
          if (t.paymentMethod === 'CASH') cashIncome += t.amount;
          if (t.paymentMethod === 'UPI') upiIncome += t.amount;
          if (t.paymentMethod === 'CARD') cardIncome += t.amount;
          if (t.paymentMethod === 'BANK') bankIncome += t.amount;
        } else if (['EXPENSE', 'SUPPLIER_PAYMENT'].includes(t.type)) {
          totalExpenses += t.amount;
          if (t.paymentMethod === 'CASH') cashExpense += t.amount;
        } else if (t.type === 'CUSTOMER_CREDIT') {
          creditGivenToday += t.amount;
        }
      }
    }

    if (inMemoryCustomers.has(businessId)) {
      pendingCustomerCollectionTotal = inMemoryCustomers.get(businessId)!.reduce((sum, c) => sum + c.pendingAmount, 0);
    }

    if (inMemorySuppliers.has(businessId)) {
      pendingSupplierTotal = inMemorySuppliers.get(businessId)!.reduce((sum, s) => sum + s.pendingAmount, 0);
    }

    // Query DB if pool active
    if (pool) {
      try {
        const txRes = await pool.query(
          `SELECT type, amount, payment_method FROM transactions WHERE business_id = $1 AND transaction_date = $2`,
          [businessId, todayStr]
        );
        totalIncome = 0;
        totalExpenses = 0;
        cashIncome = 0;
        upiIncome = 0;
        cardIncome = 0;
        bankIncome = 0;
        cashExpense = 0;
        creditGivenToday = 0;

        for (const row of txRes.rows) {
          const amt = parseFloat(row.amount);
          if (['INCOME', 'CUSTOMER_PAYMENT'].includes(row.type)) {
            totalIncome += amt;
            if (row.payment_method === 'CASH') cashIncome += amt;
            if (row.payment_method === 'UPI') upiIncome += amt;
            if (row.payment_method === 'CARD') cardIncome += amt;
            if (row.payment_method === 'BANK') bankIncome += amt;
          } else if (['EXPENSE', 'SUPPLIER_PAYMENT'].includes(row.type)) {
            totalExpenses += amt;
            if (row.payment_method === 'CASH') cashExpense += amt;
          } else if (row.type === 'CUSTOMER_CREDIT') {
            creditGivenToday += amt;
          }
        }

        const custRes = await pool.query(`SELECT SUM(pending_amount) as total FROM customers WHERE business_id = $1`, [businessId]);
        if (custRes.rows.length > 0 && custRes.rows[0].total) {
          pendingCustomerCollectionTotal = parseFloat(custRes.rows[0].total);
        }

        const suppRes = await pool.query(`SELECT SUM(pending_amount) as total FROM suppliers WHERE business_id = $1`, [businessId]);
        if (suppRes.rows.length > 0 && suppRes.rows[0].total) {
          pendingSupplierTotal = parseFloat(suppRes.rows[0].total);
        }
      } catch (dbErr) {}
    }

    res.json({
      success: true,
      data: {
        date: todayStr,
        totalIncome,
        totalExpenses,
        netResult: totalIncome - totalExpenses,
        cashIncome,
        upiIncome,
        cardIncome,
        bankIncome,
        cashExpense,
        creditGivenToday,
        pendingCustomerCollectionTotal,
        pendingSupplierTotal,
      },
    });
  } catch (err) {
    next(err);
  }
});
