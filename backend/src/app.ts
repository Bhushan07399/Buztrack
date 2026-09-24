import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import path from 'path';
import { authRouter } from './modules/auth/auth.routes.js';
import { businessRouter } from './modules/business/business.routes.js';
import { accountsRouter } from './modules/accounts/accounts.routes.js';
import { transactionsRouter } from './modules/transactions/transactions.routes.js';
import { customersRouter } from './modules/customers/customers.routes.js';
import { suppliersRouter } from './modules/suppliers/suppliers.routes.js';
import { billsRouter } from './modules/bills/bills.routes.js';
import { cashbookRouter } from './modules/cashbook/cashbook.routes.js';
import { recurringRouter } from './modules/recurring/recurring.routes.js';
import { remindersRouter } from './modules/reminders/reminders.routes.js';
import { reportsRouter } from './modules/reports/reports.routes.js';
import { exportRouter } from './modules/reports/export.routes.js';
import { subscriptionsRouter } from './modules/subscriptions/subscriptions.routes.js';
import { paymentsRouter } from './modules/payments/payments.routes.js';
import { errorHandler } from './middleware/errorHandler.js';

export const app = express();

app.use(helmet({ crossOriginResourcePolicy: false }));
app.use(cors());
app.use(express.json({ limit: '15mb' }));
app.use(express.urlencoded({ extended: true, limit: '15mb' }));

// Static file serving for uploads/attachments
app.use('/static/uploads', express.static(path.join(process.cwd(), 'uploads')));

// Health Check Endpoint
app.get('/health', (_req, res) => {
  res.json({
    status: 'OK',
    service: 'BUZTRACK Production API',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
  });
});

// Versioned API Routes
const apiV1 = express.Router();

apiV1.use('/auth', authRouter);
apiV1.use('/business', businessRouter);
apiV1.use('/accounts', accountsRouter);
apiV1.use('/transactions', transactionsRouter);
apiV1.use('/customers', customersRouter);
apiV1.use('/suppliers', suppliersRouter);
apiV1.use('/bills', billsRouter);
apiV1.use('/cashbook', cashbookRouter);
apiV1.use('/recurring', recurringRouter);
apiV1.use('/reminders', remindersRouter);
apiV1.use('/reports/export', exportRouter);
apiV1.use('/reports', reportsRouter);
apiV1.use('/subscriptions', subscriptionsRouter);
apiV1.use('/payments', paymentsRouter);

app.use('/api/v1', apiV1);

app.use(errorHandler);
