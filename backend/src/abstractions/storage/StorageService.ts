export interface UploadedFileResult {
  fileKey: string;
  url: string;
  mimeType: string;
  sizeBytes: number;
}

export interface StorageService {
  uploadFile(fileBuffer: Buffer, fileName: string, mimeType: string): Promise<UploadedFileResult>;
  getFileUrl(fileKey: string): Promise<string>;
  deleteFile(fileKey: string): Promise<boolean>;
}

export class LocalStorageProvider implements StorageService {
  async uploadFile(fileBuffer: Buffer, fileName: string, mimeType: string): Promise<UploadedFileResult> {
    const fileKey = `uploads/${Date.now()}_${fileName}`;
    return {
      fileKey,
      url: `http://localhost:5000/static/${fileKey}`,
      mimeType,
      sizeBytes: fileBuffer.length,
    };
  }

  async getFileUrl(fileKey: string): Promise<string> {
    return `http://localhost:5000/static/${fileKey}`;
  }

  async deleteFile(_fileKey: string): Promise<boolean> {
    return true;
  }
}
