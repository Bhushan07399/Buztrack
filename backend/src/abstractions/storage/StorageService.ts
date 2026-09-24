import fs from 'fs';
import path from 'path';

export interface UploadedFileResult {
  fileKey: string;
  url: string;
  mimeType: string;
  sizeBytes: number;
}

export interface StorageService {
  uploadFile(fileBuffer: Buffer, originalFileName: string, mimeType: string): Promise<UploadedFileResult>;
  getFileUrl(fileKey: string): Promise<string>;
  deleteFile(fileKey: string): Promise<boolean>;
}

export class LocalStorageProvider implements StorageService {
  private uploadDir: string;
  private maxSizeBytes = 10 * 1024 * 1024; // 10MB MAX
  private allowedMimeTypes = new Set(['image/jpeg', 'image/png', 'image/webp', 'application/pdf']);

  constructor() {
    this.uploadDir = path.join(process.cwd(), 'uploads');
    if (!fs.existsSync(this.uploadDir)) {
      fs.mkdirSync(this.uploadDir, { recursive: true });
    }
  }

  async uploadFile(fileBuffer: Buffer, originalFileName: string, mimeType: string): Promise<UploadedFileResult> {
    // 1. Validate File Size
    if (fileBuffer.length > this.maxSizeBytes) {
      const err = new Error('File too large. Maximum allowed size is 10MB.');
      (err as any).statusCode = 413;
      throw err;
    }

    // 2. Validate File Type
    if (!this.allowedMimeTypes.has(mimeType.toLowerCase())) {
      const err = new Error(`Invalid file type '${mimeType}'. Allowed types: JPG, PNG, WEBP, PDF.`);
      (err as any).statusCode = 400;
      throw err;
    }

    // 3. Sanitize Filename & Prevent Path Traversal
    const sanitizedBase = path.basename(originalFileName).replace(/[^a-zA-Z0-9._-]/g, '_');
    const safeFileName = `${Date.now()}_${Math.floor(Math.random() * 1000)}_${sanitizedBase}`;
    const filePath = path.join(this.uploadDir, safeFileName);

    // 4. Save File to Disk
    await fs.promises.writeFile(filePath, fileBuffer);

    const fileKey = `uploads/${safeFileName}`;
    return {
      fileKey,
      url: `http://localhost:5000/static/${fileKey}`,
      mimeType,
      sizeBytes: fileBuffer.length,
    };
  }

  async getFileUrl(fileKey: string): Promise<string> {
    const cleanKey = path.basename(fileKey);
    return `http://localhost:5000/static/uploads/${cleanKey}`;
  }

  async deleteFile(fileKey: string): Promise<boolean> {
    try {
      const cleanKey = path.basename(fileKey);
      const filePath = path.join(this.uploadDir, cleanKey);
      if (fs.existsSync(filePath)) {
        await fs.promises.unlink(filePath);
      }
      return true;
    } catch (err) {
      return false;
    }
  }
}
