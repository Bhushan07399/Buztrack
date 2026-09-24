import { Router } from 'express';
import { authenticateJwt, AuthenticatedRequest } from '../../middleware/auth.js';
import { requireTenant } from '../../middleware/tenant.js';
import { generateCsv, generatePdfBuffer, generateXlsxBuffer, ExportRow, ExportHeaderInfo } from './exportEngine.js';
import { inMemoryTransactions } from '../transactions/transactions.routes.js';
import { inMemoryCustomers } from '../customers/customers.routes.js';
import { inMemorySuppliers } from '../suppliers/suppliers.routes.js';
import { inMemoryBusinesses } from '../auth/auth.routes.js';
import { pool } from '../../database/db.js';

export const exportRouter = Router();

exportRouter.use(authenticateJwt);
exportRouter.use(requireTenant);

// GET /api/v1/reports/export/transactions?format=pdf|csv|xlsx
exportRouter.get('/transactions', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const format = ((req.query.format as string) || 'pdf').toLowerCase();

    let businessName = 'Sharma General Store';
    if (inMemoryBusinesses.has(businessId)) {
      businessName = inMemoryBusinesses.get(businessId)!.name;
    }

    const headerInfo: ExportHeaderInfo = {
      businessName,
      reportTitle: 'Transaction Ledger Statement',
      dateRange: (req.query.dateFrom as string) ? `${req.query.dateFrom} to ${req.query.dateTo || 'Today'}` : 'All Time',
    };

    let rows: ExportRow[] = [];

    if (inMemoryTransactions.has(businessId)) {
      rows = inMemoryTransactions.get(businessId)!.map((t) => ({
        date: t.transactionDate,
        type: t.type,
        partyOrCategory: t.customerName || t.supplierName || t.categoryName,
        paymentMethod: t.paymentMethod,
        amount: t.amount,
        note: t.note,
      }));
    }

    if (rows.length === 0 && pool) {
      try {
        const result = await pool.query(
          `SELECT t.transaction_date, t.type, t.category_name, t.payment_method, t.amount, t.note, c.name as customer_name, s.name as supplier_name
           FROM transactions t
           LEFT JOIN customers c ON t.customer_id = c.id
           LEFT JOIN suppliers s ON t.supplier_id = s.id
           WHERE t.business_id = $1
           ORDER BY t.transaction_date DESC`,
          [businessId]
        );
        rows = result.rows.map((row) => ({
          date: row.transaction_date,
          type: row.type,
          partyOrCategory: row.customer_name || row.supplier_name || row.category_name,
          paymentMethod: row.payment_method,
          amount: parseFloat(row.amount),
          note: row.note || '',
        }));
      } catch (dbErr) {}
    }

    if (format === 'csv') {
      const csvStr = generateCsv(headerInfo, rows);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="transactions_statement.csv"`);
      res.send(csvStr);
    } else if (format === 'xlsx') {
      const buffer = await generateXlsxBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="transactions_statement.xlsx"`);
      res.send(buffer);
    } else {
      const pdfBuffer = await generatePdfBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="transactions_statement.pdf"`);
      res.send(pdfBuffer);
    }
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/reports/export/customers/:id/statement?format=pdf|csv|xlsx
exportRouter.get('/customers/:id/statement', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const custId = req.params.id;
    const format = ((req.query.format as string) || 'pdf').toLowerCase();

    let custName = 'Customer';
    if (inMemoryCustomers.has(businessId)) {
      const c = inMemoryCustomers.get(businessId)!.find((c) => c.id === custId);
      if (c) custName = c.name;
    }

    const headerInfo: ExportHeaderInfo = {
      businessName: 'Sharma General Store',
      reportTitle: `Customer Udhari Statement - ${custName}`,
      dateRange: 'All Time',
    };

    let rows: ExportRow[] = [];
    if (inMemoryTransactions.has(businessId)) {
      rows = inMemoryTransactions
        .get(businessId)!
        .filter((t) => t.customerId === custId)
        .map((t) => ({
          date: t.transactionDate,
          type: t.type,
          partyOrCategory: custName,
          paymentMethod: t.paymentMethod,
          amount: t.amount,
          note: t.note,
        }));
    }

    if (format === 'csv') {
      const csvStr = generateCsv(headerInfo, rows);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="customer_${custId}_statement.csv"`);
      res.send(csvStr);
    } else if (format === 'xlsx') {
      const buffer = await generateXlsxBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="customer_${custId}_statement.xlsx"`);
      res.send(buffer);
    } else {
      const pdfBuffer = await generatePdfBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="customer_${custId}_statement.pdf"`);
      res.send(pdfBuffer);
    }
  } catch (err) {
    next(err);
  }
});

// GET /api/v1/reports/export/suppliers/:id/statement?format=pdf|csv|xlsx
exportRouter.get('/suppliers/:id/statement', async (req: AuthenticatedRequest, res, next) => {
  try {
    const businessId = req.businessId!;
    const suppId = req.params.id;
    const format = ((req.query.format as string) || 'pdf').toLowerCase();

    let suppName = 'Supplier';
    if (inMemorySuppliers.has(businessId)) {
      const s = inMemorySuppliers.get(businessId)!.find((s) => s.id === suppId);
      if (s) suppName = s.name;
    }

    const headerInfo: ExportHeaderInfo = {
      businessName: 'Sharma General Store',
      reportTitle: `Supplier Statement - ${suppName}`,
      dateRange: 'All Time',
    };

    let rows: ExportRow[] = [];
    if (inMemoryTransactions.has(businessId)) {
      rows = inMemoryTransactions
        .get(businessId)!
        .filter((t) => t.supplierId === suppId)
        .map((t) => ({
          date: t.transactionDate,
          type: t.type,
          partyOrCategory: suppName,
          paymentMethod: t.paymentMethod,
          amount: t.amount,
          note: t.note,
        }));
    }

    if (format === 'csv') {
      const csvStr = generateCsv(headerInfo, rows);
      res.setHeader('Content-Type', 'text/csv; charset=utf-8');
      res.setHeader('Content-Disposition', `attachment; filename="supplier_${suppId}_statement.csv"`);
      res.send(csvStr);
    } else if (format === 'xlsx') {
      const buffer = await generateXlsxBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
      res.setHeader('Content-Disposition', `attachment; filename="supplier_${suppId}_statement.xlsx"`);
      res.send(buffer);
    } else {
      const pdfBuffer = await generatePdfBuffer(headerInfo, rows);
      res.setHeader('Content-Type', 'application/pdf');
      res.setHeader('Content-Disposition', `inline; filename="supplier_${suppId}_statement.pdf"`);
      res.send(pdfBuffer);
    }
  } catch (err) {
    next(err);
  }
});
