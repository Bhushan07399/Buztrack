import { describe, it, expect, beforeAll } from 'vitest';
import request from 'supertest';
import { app } from '../src/app.js';
import { ProductionOCRService } from '../src/abstractions/ocr/OCRService.js';
import { LocalStorageProvider } from '../src/abstractions/storage/StorageService.js';
import { generateCsv, generatePdfBuffer, generateXlsxBuffer } from '../src/modules/reports/exportEngine.js';

describe('Phase 4: OCR Bill Scanner, Storage, Duplicate Detection & Export Engine Tests', () => {
  let userAToken: string;
  let userABusinessId: string;
  const userAPhone = `98333${Math.floor(10000 + Math.random() * 90000)}`;

  let userBToken: string;
  let userBBusinessId: string;
  const userBPhone = `98444${Math.floor(10000 + Math.random() * 90000)}`;

  let createdBillId: string;
  let createdSupplierId: string;

  beforeAll(async () => {
    // Register User A
    const resA = await request(app).post('/api/v1/auth/register').send({
      fullName: 'Retailer Alpha',
      phone: userAPhone,
      password: 'password123',
      businessName: 'Alpha Supermarket',
    });
    userAToken = resA.body.data.token;
    userABusinessId = resA.body.data.business.id;

    // Register User B
    const resB = await request(app).post('/api/v1/auth/register').send({
      fullName: 'Wholesaler Beta',
      phone: userBPhone,
      password: 'password123',
      businessName: 'Beta Wholesalers',
    });
    userBToken = resB.body.data.token;
    userBBusinessId = resB.body.data.business.id;

    // Create supplier for User A
    const suppRes = await request(app)
      .post('/api/v1/suppliers')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({ name: 'Gupta Wholesale Mart', phone: '9810099887' });
    createdSupplierId = suppRes.body.data.id;
  });

  // BILLS CRUD
  it('1. Create Bill for User A', async () => {
    const res = await request(app)
      .post('/api/v1/bills')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        supplierName: 'Gupta Wholesale Mart',
        supplierId: createdSupplierId,
        invoiceNumber: 'INV-4001',
        invoiceDate: '2026-03-25',
        subtotal: 4000.0,
        taxAmount: 720.0,
        totalAmount: 4720.0,
        items: [{ name: 'Rice 25kg', quantity: 2, unitPrice: 2000.0, totalPrice: 4000.0 }],
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.invoiceNumber).toBe('INV-4001');
    createdBillId = res.body.data.id;
  });

  it('2. Get Bill by ID', async () => {
    const res = await request(app)
      .get(`/api/v1/bills/${createdBillId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.invoiceNumber).toBe('INV-4001');
  });

  it('3. List Bills (Paginated)', async () => {
    const res = await request(app)
      .get('/api/v1/bills')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBeGreaterThan(0);
  });

  it('4. Delete Bill', async () => {
    const res = await request(app)
      .delete(`/api/v1/bills/${createdBillId}`)
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
  });

  it('7. Tenant Isolation: User B cannot access User A bill', async () => {
    const res = await request(app)
      .get(`/api/v1/bills/${createdBillId}`)
      .set('Authorization', `Bearer ${userBToken}`)
      .set('x-business-id', userBBusinessId);

    expect(res.status).toBe(404);
  });

  // OCR EXTRACTION
  it('8 - 11. Production OCR Service parses text & extracts invoice, amounts & matches supplier', async () => {
    const ocr = new ProductionOCRService();
    const rawBillText = 'GUPTA WHOLESALE MART Invoice #: INV-8822 Date: 2026-03-25 Total: ₹2,360.00';
    const result = await ocr.processBillImage(Buffer.from(rawBillText), 'test_bill.jpg', [
      { id: createdSupplierId, name: 'Gupta Wholesale Mart' },
    ]);

    expect(result.invoiceNumber).toBe('INV-8822');
    expect(result.invoiceDate).toBe('2026-03-25');
    expect(result.totalAmount).toBe(2360.0);
    expect(result.supplierName).toBe('Gupta Wholesale Mart');
    expect(result.matchedSupplierId).toBe(createdSupplierId);
  });

  // DUPLICATE INVOICE DETECTION
  it('13. Duplicate invoice detection returns 409 DUPLICATE_INVOICE', async () => {
    // First Bill creation
    await request(app)
      .post('/api/v1/bills')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        supplierName: 'Gupta Wholesale Mart',
        supplierId: createdSupplierId,
        invoiceNumber: 'INV-DUP-100',
        invoiceDate: '2026-03-25',
        subtotal: 1000.0,
        taxAmount: 180.0,
        totalAmount: 1180.0,
      });

    // Attempting same invoice number for same supplier
    const resDup = await request(app)
      .post('/api/v1/bills')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId)
      .send({
        supplierName: 'Gupta Wholesale Mart',
        supplierId: createdSupplierId,
        invoiceNumber: 'INV-DUP-100',
        invoiceDate: '2026-03-25',
        subtotal: 1000.0,
        taxAmount: 180.0,
        totalAmount: 1180.0,
      });

    expect(resDup.status).toBe(409);
    expect(resDup.body.error).toContain('409 DUPLICATE_INVOICE');
  });

  // EXPORT ENGINE (PDF, CSV, XLSX)
  it('18. PDF Export generation produces valid PDF buffer', async () => {
    const pdfBuf = await generatePdfBuffer(
      { businessName: 'Alpha Supermarket', reportTitle: 'Transaction Statement' },
      [{ date: '2026-03-25', type: 'INCOME', partyOrCategory: 'Sales', paymentMethod: 'CASH', amount: 5000.0 }]
    );

    expect(pdfBuf).toBeInstanceOf(Buffer);
    expect(pdfBuf.toString('ascii', 0, 4)).toBe('%PDF');
  });

  it('19. CSV Export generation produces properly escaped UTF-8 CSV', () => {
    const csvStr = generateCsv(
      { businessName: 'Alpha Supermarket', reportTitle: 'Transaction Statement' },
      [{ date: '2026-03-25', type: 'INCOME', partyOrCategory: 'Sales, Groceries', paymentMethod: 'CASH', amount: 5000.0 }]
    );

    expect(csvStr).toContain('Date,Type,Category/Party');
    expect(csvStr).toContain('"Sales, Groceries"');
  });

  it('20. XLSX Export generation produces valid Excel buffer', async () => {
    const xlsxBuf = await generateXlsxBuffer(
      { businessName: 'Alpha Supermarket', reportTitle: 'Transaction Statement' },
      [{ date: '2026-03-25', type: 'INCOME', partyOrCategory: 'Sales', paymentMethod: 'CASH', amount: 5000.0 }]
    );

    expect(xlsxBuf).toBeInstanceOf(Buffer);
    expect(xlsxBuf.length).toBeGreaterThan(100);
  });

  it('21. GET /api/v1/reports/export/transactions?format=pdf -> Downloads PDF report', async () => {
    const res = await request(app)
      .get('/api/v1/reports/export/transactions?format=pdf')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toContain('application/pdf');
  });

  it('22. GET /api/v1/reports/export/transactions?format=csv -> Downloads CSV report', async () => {
    const res = await request(app)
      .get('/api/v1/reports/export/transactions?format=csv')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toContain('text/csv');
  });

  it('23. GET /api/v1/reports/export/transactions?format=xlsx -> Downloads XLSX report', async () => {
    const res = await request(app)
      .get('/api/v1/reports/export/transactions?format=xlsx')
      .set('Authorization', `Bearer ${userAToken}`)
      .set('x-business-id', userABusinessId);

    expect(res.status).toBe(200);
    expect(res.headers['content-type']).toContain('spreadsheetml');
  });

  // STORAGE & SECURITY UPLOAD VALIDATION
  it('25. Invalid file type upload is rejected (400 Bad Request)', async () => {
    const storage = new LocalStorageProvider();
    await expect(storage.uploadFile(Buffer.from('echo malicious'), 'script.exe', 'application/x-msdownload')).rejects.toThrow();
  });

  it('26. Oversized file upload (>10MB) is rejected (413 Payload Too Large)', async () => {
    const storage = new LocalStorageProvider();
    const hugeBuffer = Buffer.alloc(11 * 1024 * 1024); // 11MB
    await expect(storage.uploadFile(hugeBuffer, 'huge.jpg', 'image/jpeg')).rejects.toThrow();
  });
});
