export interface OCRParsedItem {
  name: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface OCRParsedBillResult {
  supplierName: string;
  invoiceNumber: string;
  invoiceDate: string; // YYYY-MM-DD
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  items: OCRParsedItem[];
  rawText: string;
  confidenceScore: number;
}

export interface OCRService {
  processBillImage(imageBuffer: Buffer, fileName: string): Promise<OCRParsedBillResult>;
}

export class MockOCRProvider implements OCRService {
  async processBillImage(_imageBuffer: Buffer, _fileName: string): Promise<OCRParsedBillResult> {
    return {
      supplierName: 'Gupta Wholesale Mart',
      invoiceNumber: `INV-${Math.floor(1000 + Math.random() * 9000)}`,
      invoiceDate: new Date().toISOString().split('T')[0],
      subtotal: 1200.00,
      taxAmount: 216.00,
      totalAmount: 1416.00,
      items: [
        { name: 'Basmati Rice 25kg', quantity: 1, unitPrice: 1200.00, totalPrice: 1200.00 },
      ],
      rawText: 'GUPTA WHOLESALE MART Invoice # INV-1234 Rice 25kg Total: 1416.00',
      confidenceScore: 0.95,
    };
  }
}
