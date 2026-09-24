import PDFDocument from 'pdfkit';
import ExcelJS from 'exceljs';

export interface ExportHeaderInfo {
  businessName: string;
  ownerName?: string;
  address?: string;
  gstin?: string;
  reportTitle: string;
  dateRange?: string;
}

export interface ExportRow {
  date: string;
  type: string;
  partyOrCategory: string;
  paymentMethod: string;
  amount: number;
  note?: string;
}

// 1. CSV GENERATOR
export function generateCsv(headerInfo: ExportHeaderInfo, rows: ExportRow[]): string {
  const escapeCsv = (str: string) => {
    if (str.includes(',') || str.includes('"') || str.includes('\n')) {
      return `"${str.replace(/"/g, '""')}"`;
    }
    return str;
  };

  const lines: string[] = [];
  lines.push(`# ${headerInfo.businessName} - ${headerInfo.reportTitle}`);
  if (headerInfo.dateRange) lines.push(`# Date Range: ${headerInfo.dateRange}`);
  lines.push(''); // blank line

  // Column Headers
  lines.push(['Date', 'Type', 'Category/Party', 'Payment Method', 'Amount (INR)', 'Note'].map(escapeCsv).join(','));

  // Data Rows
  let totalAmount = 0;
  for (const row of rows) {
    totalAmount += row.amount;
    lines.push([
      row.date,
      row.type,
      row.partyOrCategory,
      row.paymentMethod,
      row.amount.toFixed(2),
      row.note || '',
    ].map(escapeCsv).join(','));
  }

  // Summary Row
  lines.push(['TOTAL', '', '', '', totalAmount.toFixed(2), ''].map(escapeCsv).join(','));

  return lines.join('\n');
}

// 2. EXCEL XLSX GENERATOR
export async function generateXlsxBuffer(headerInfo: ExportHeaderInfo, rows: ExportRow[]): Promise<Buffer> {
  const workbook = new ExcelJS.Workbook();
  const worksheet = workbook.addWorksheet(headerInfo.reportTitle.substring(0, 30));

  // Business Header
  worksheet.addRow([headerInfo.businessName]).font = { bold: true, size: 16 };
  worksheet.addRow([headerInfo.reportTitle]).font = { bold: true, size: 12, color: { argb: 'FF166534' } };
  if (headerInfo.dateRange) worksheet.addRow([`Date Range: ${headerInfo.dateRange}`]);
  worksheet.addRow([]); // empty row

  // Table Headers
  const headerRow = worksheet.addRow(['Date', 'Type', 'Category / Party', 'Payment Method', 'Amount (₹)', 'Note']);
  headerRow.font = { bold: true, color: { argb: 'FFFFFFFF' } };
  headerRow.eachCell((cell) => {
    cell.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF0F172A' }, // Dark Slate
    };
  });

  // Table Data
  let total = 0;
  for (const row of rows) {
    total += row.amount;
    const r = worksheet.addRow([row.date, row.type, row.partyOrCategory, row.paymentMethod, row.amount, row.note || '']);
    r.getCell(5).numFmt = '₹#,##0.00';
  }

  // Summary Row
  const totalRow = worksheet.addRow(['TOTAL', '', '', '', total, '']);
  totalRow.font = { bold: true };
  totalRow.getCell(5).numFmt = '₹#,##0.00';

  // Auto Column Widths
  worksheet.columns.forEach((col) => {
    col.width = 20;
  });

  const buffer = await workbook.xlsx.writeBuffer();
  return Buffer.from(buffer);
}

// 3. PDF GENERATOR
export function generatePdfBuffer(headerInfo: ExportHeaderInfo, rows: ExportRow[]): Promise<Buffer> {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ margin: 40 });
    const chunks: Buffer[] = [];

    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', (err) => reject(err));

    // Header
    doc.fontSize(20).fillColor('#0F172A').text(headerInfo.businessName, { align: 'left' });
    doc.fontSize(12).fillColor('#166534').text(headerInfo.reportTitle, { align: 'left' });
    if (headerInfo.dateRange) {
      doc.fontSize(10).fillColor('#64748B').text(`Date Range: ${headerInfo.dateRange}`);
    }
    doc.moveDown(1.5);

    // Table Header
    doc.fontSize(10).fillColor('#0F172A').font('Helvetica-Bold');
    doc.text('Date', 40, doc.y, { width: 80, continued: true });
    doc.text('Type', { width: 90, continued: true });
    doc.text('Category/Party', { width: 150, continued: true });
    doc.text('Payment', { width: 80, continued: true });
    doc.text('Amount (₹)', { width: 90, align: 'right' });

    doc.moveDown(0.5);
    doc.strokeColor('#CBD5E1').lineWidth(1).moveTo(40, doc.y).lineTo(550, doc.y).stroke();
    doc.moveDown(0.5);

    doc.font('Helvetica').fontSize(9).fillColor('#334155');
    let total = 0;

    for (const row of rows) {
      total += row.amount;
      const y = doc.y;
      if (y > 700) {
        doc.addPage();
      }

      doc.text(row.date, 40, doc.y, { width: 80, continued: true });
      doc.text(row.type, { width: 90, continued: true });
      doc.text(row.partyOrCategory, { width: 150, continued: true });
      doc.text(row.paymentMethod, { width: 80, continued: true });
      doc.text(`₹${row.amount.toFixed(2)}`, { width: 90, align: 'right' });
      doc.moveDown(0.4);
    }

    doc.moveDown(0.5);
    doc.strokeColor('#0F172A').lineWidth(1).moveTo(40, doc.y).lineTo(550, doc.y).stroke();
    doc.moveDown(0.5);

    doc.font('Helvetica-Bold').fontSize(11).fillColor('#0F172A');
    doc.text('TOTAL', 40, doc.y, { width: 400, continued: true });
    doc.text(`₹${total.toFixed(2)}`, { width: 110, align: 'right' });

    doc.moveDown(2);
    doc.fontSize(8).fillColor('#94A3B8').text(`Generated on ${new Date().toLocaleString()} by BUZTRACK Ledger`, { align: 'center' });

    doc.end();
  });
}
