import { describe, it, expect } from 'vitest';
import request from 'supertest';
import { app } from '../src/app.js';

describe('Production Authentication, Business Onboarding & Multi-Tenant Security', () => {
  let userAToken: string;
  let userABusinessId: string;
  const userAPhone = '9870001122';

  let userBToken: string;
  let userBBusinessId: string;
  const userBPhone = '9870003344';

  it('1. POST /api/v1/auth/register -> Registers User A and creates Business A', async () => {
    const res = await request(app)
      .post('/api/v1/auth/register')
      .send({
        fullName: 'Rahul Sharma',
        phone: userAPhone,
        password: 'password123',
        businessName: 'Sharma General Store',
        businessType: 'RETAIL',
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.token).toBeDefined();
    expect(res.body.data.user.phone).toBe(userAPhone);
    expect(res.body.data.business.name).toBe('Sharma General Store');

    userAToken = res.body.data.token;
    userABusinessId = res.body.data.business.id;
  });

  it('2. POST /api/v1/auth/register -> Duplicate phone registration is rejected (409)', async () => {
    const res = await request(app)
      .post('/api/v1/auth/register')
      .send({
        fullName: 'Duplicate Rahul',
        phone: userAPhone,
        password: 'password123',
        businessName: 'Duplicate Store',
      });

    expect(res.status).toBe(409);
    expect(res.body.success).toBe(false);
    expect(res.body.error).toContain('already exists');
  });

  it('3. POST /api/v1/auth/login -> Login User A with correct password', async () => {
    const res = await request(app)
      .post('/api/v1/auth/login')
      .send({
        phone: userAPhone,
        password: 'password123',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.token).toBeDefined();
    expect(res.body.data.business.id).toBe(userABusinessId);
  });

  it('4. POST /api/v1/auth/login -> Login fails with wrong password (401)', async () => {
    const res = await request(app)
      .post('/api/v1/auth/login')
      .send({
        phone: userAPhone,
        password: 'WRONG_PASSWORD',
      });

    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });

  it('5. GET /api/v1/auth/me -> Retrieves session info for User A', async () => {
    const res = await request(app)
      .get('/api/v1/auth/me')
      .set('Authorization', `Bearer ${userAToken}`);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.user.phone).toBe(userAPhone);
    expect(res.body.data.activeBusiness.id).toBe(userABusinessId);
  });

  it('6. GET /api/v1/auth/me -> Rejects request without token (401)', async () => {
    const res = await request(app).get('/api/v1/auth/me');
    expect(res.status).toBe(401);
  });

  it('7. Register User B and create Business B', async () => {
    const res = await request(app)
      .post('/api/v1/auth/register')
      .send({
        fullName: 'Suresh Patel',
        phone: userBPhone,
        password: 'password456',
        businessName: 'Patel Wholesalers',
      });

    expect(res.status).toBe(201);
    userBToken = res.body.data.token;
    userBBusinessId = res.body.data.business.id;
  });

  it('8. CROSS-TENANT SECURITY: User A attempts to access Business B -> Strictly REJECTED (403 Forbidden)', async () => {
    // User A passes their valid token, but sets x-business-id to User B's business ID
    const res = await request(app)
      .get('/api/v1/business/profile')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(403);
    expect(res.body.success).toBe(false);
    expect(res.body.error).toContain('Access denied');
  });

  it('9. Business Onboarding / Profile Update without requiring GSTIN', async () => {
    const res = await request(app)
      .put('/api/v1/business/profile')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        businessName: 'Sharma Provision Store',
        owner: 'Rahul V. Sharma',
        mobile: userAPhone,
        address: 'Shop 15, Station Road, Jaipur',
        GSTIN: '', // Optional GSTIN!
        businessType: 'RETAIL',
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.businessName).toBe('Sharma Provision Store');
    expect(res.body.data.GSTIN).toBeNull();
  });

  it('10. POST /api/v1/auth/logout -> Logout succeeds', async () => {
    const res = await request(app)
      .post('/api/v1/auth/logout')
      .set('Authorization', `Bearer ${userAToken}`);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });
});
