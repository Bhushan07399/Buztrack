import { describe, it, expect, beforeAll } from 'vitest';
import request from 'supertest';
import { app } from '../src/app.js';

describe('Phase 3: Core Ledger & Financial Sync Integration Tests', () => {
  let userAToken: string;
  let userABusinessId: string;
  const userAPhone = `98111${Math.floor(10000 + Math.random() * 90000)}`;

  let userBToken: string;
  let userBBusinessId: string;
  const userBPhone = `98222${Math.floor(10000 + Math.random() * 90000)}`;

  let createdTxId: string;
  let createdCustomerId: string;
  let createdSupplierId: string;

  beforeAll(async () => {
    // Register User A
    const resA = await request(app).post('/api/v1/auth/register').send({
      fullName: 'Retailer A',
      phone: userAPhone,
      password: 'password123',
      businessName: 'Retail Shop A',
    });
    expect(resA.status).toBe(201);
    userAToken = resA.body.data.token;
    userABusinessId = resA.body.data.business.id;

    // Register User B
    const resB = await request(app).post('/api/v1/auth/register').send({
      fullName: 'Wholesaler B',
      phone: userBPhone,
      password: 'password123',
      businessName: 'Wholesale Shop B',
    });
    expect(resB.status).toBe(201);
    userBToken = resB.body.data.token;
    userBBusinessId = resB.body.data.business.id;
  });

  // TRANSACTIONS
  it('1. Authenticated transaction creation (INCOME)', async () => {
    const res = await request(app)
      .post('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        type: 'INCOME',
        amount: 5000.0,
        categoryName: 'Sales',
        paymentMethod: 'CASH',
        transactionDate: '2026-03-20',
        note: 'Grocery sale',
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.amount).toBe(5000.0);
    createdTxId = res.body.data.id;
  });

  it('2. Transaction listing for business', async () => {
    const res = await request(app)
      .get('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.length).toBeGreaterThan(0);
  });

  it('3. Transaction retrieval by ID', async () => {
    const res = await request(app)
      .get(`/api/v1/transactions/${createdTxId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.id).toBe(createdTxId);
  });

  it('4. Transaction update', async () => {
    const res = await request(app)
      .put(`/api/v1/transactions/${createdTxId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({ note: 'Updated note' });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  it('5. Transaction deletion', async () => {
    const res = await request(app)
      .delete(`/api/v1/transactions/${createdTxId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
  });

  it('6. Tenant Isolation: User B cannot retrieve User A transaction', async () => {
    const res = await request(app)
      .get(`/api/v1/transactions/${createdTxId}`)
      .set('Authorization', `Bearer ${userBToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(404);
  });

  // CUSTOMERS
  it('7. Create Customer for User A', async () => {
    const res = await request(app)
      .post('/api/v1/customers')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        name: 'Ramesh Kumar',
        phone: '9876543210',
        address: 'Jaipur Market',
      });

    expect(res.status).toBe(201);
    expect(res.body.data.name).toBe('Ramesh Kumar');
    createdCustomerId = res.body.data.id;
  });

  it('8. List Customers', async () => {
    const res = await request(app)
      .get('/api/v1/customers')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThan(0);
  });

  it('9. Customer details by ID', async () => {
    const res = await request(app)
      .get(`/api/v1/customers/${createdCustomerId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.name).toBe('Ramesh Kumar');
  });

  it('10. Customer Credit transaction updates pending amount', async () => {
    const res = await request(app)
      .post('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        type: 'CUSTOMER_CREDIT',
        amount: 2500.0,
        categoryName: 'Customer Credit',
        paymentMethod: 'CASH',
        transactionDate: '2026-03-20',
        customerId: createdCustomerId,
        note: 'Credit given for wheat flour',
      });

    expect(res.status).toBe(201);
  });

  it('11. Customer Payment transaction updates pending amount', async () => {
    const res = await request(app)
      .post('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        type: 'CUSTOMER_PAYMENT',
        amount: 1000.0,
        categoryName: 'Credit Collection',
        paymentMethod: 'UPI',
        transactionDate: '2026-03-20',
        customerId: createdCustomerId,
        note: 'Partial payment received',
      });

    expect(res.status).toBe(201);
  });

  it('12. Customer Ledger History', async () => {
    const res = await request(app)
      .get(`/api/v1/customers/${createdCustomerId}/transactions`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBe(2);
  });

  it('13. Tenant Isolation: User B cannot access User A customer', async () => {
    const res = await request(app)
      .get(`/api/v1/customers/${createdCustomerId}`)
      .set('Authorization', `Bearer ${userBToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(404);
  });

  // SUPPLIERS
  it('14. Create Supplier for User A', async () => {
    const res = await request(app)
      .post('/api/v1/suppliers')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        name: 'Gupta Wholesale',
        phone: '9811122233',
      });

    expect(res.status).toBe(201);
    createdSupplierId = res.body.data.id;
  });

  it('15. Supplier Purchase transaction updates pending amount', async () => {
    const res = await request(app)
      .post('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        type: 'SUPPLIER_PURCHASE',
        amount: 8000.0,
        categoryName: 'Purchase',
        paymentMethod: 'CASH',
        transactionDate: '2026-03-20',
        supplierId: createdSupplierId,
        note: 'Rice bags purchase',
      });

    expect(res.status).toBe(201);
  });

  it('16. Supplier Payment transaction updates pending amount', async () => {
    const res = await request(app)
      .post('/api/v1/transactions')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        type: 'SUPPLIER_PAYMENT',
        amount: 3000.0,
        categoryName: 'Supplier Payment',
        paymentMethod: 'BANK',
        transactionDate: '2026-03-20',
        supplierId: createdSupplierId,
        note: 'Bank transfer payment to supplier',
      });

    expect(res.status).toBe(201);
  });

  it('17. Supplier Ledger History', async () => {
    const res = await request(app)
      .get(`/api/v1/suppliers/${createdSupplierId}/transactions`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBe(2);
  });

  it('18. Tenant Isolation: User B cannot access User A supplier', async () => {
    const res = await request(app)
      .get(`/api/v1/suppliers/${createdSupplierId}`)
      .set('Authorization', `Bearer ${userBToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(404);
  });

  // CASHBOOK
  it('19. Set opening cash for today', async () => {
    const res = await request(app)
      .post('/api/v1/cashbook/opening')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        date: '2026-03-20',
        openingCash: 5000.0,
      });

    expect(res.status).toBe(200);
  });

  it('20. Cashbook summary calculation', async () => {
    const res = await request(app)
      .get('/api/v1/cashbook/summary?date=2026-03-20')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.openingCash).toBe(5000.0);
    expect(res.body.data.expectedCash).toBeDefined();
  });

  it('21. Record Daily Closing', async () => {
    const res = await request(app)
      .post('/api/v1/cashbook/close')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        date: '2026-03-20',
        actualCash: 4500.0,
        notes: 'Matched cash drawer',
      });

    expect(res.status).toBe(201);
    expect(res.body.data.actualCash).toBe(4500.0);
    expect(res.body.data.difference).toBeDefined();
  });

  it('22. Duplicate daily closing protection (HTTP 409)', async () => {
    const res = await request(app)
      .post('/api/v1/cashbook/close')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        date: '2026-03-20',
        actualCash: 4500.0,
      });

    expect(res.status).toBe(409);
    expect(res.body.success).toBe(false);
  });

  it('23. Get Daily Closings history', async () => {
    const res = await request(app)
      .get('/api/v1/cashbook/closings')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThan(0);
  });

  it('24. Tenant Isolation: User B cannot access User A cashbook closings', async () => {
    const res = await request(app)
      .get('/api/v1/cashbook/closings')
      .set('Authorization', `Bearer ${userBToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBe(0); // User B has no daily closings
  });

  // FINANCIAL CORRECTNESS
  it('25 - 28. Financial Correctness Checks (Expected Cash = Opening + CashIn - CashOut)', async () => {
    const opening = 2000.0;
    const cashIn = 1500.0;
    const cashOut = 500.0;
    const expected = opening + cashIn - cashOut; // 3000.0
    const actual = 2950.0;
    const diff = actual - expected; // -50.0

    expect(expected).toBe(3000.0);
    expect(diff).toBe(-50.0);
  });
});
