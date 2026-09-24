import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  PORT: z.string().default('5000').transform((val) => parseInt(val, 10)),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  DATABASE_URL: z.string().default('postgres://postgres:postgres@localhost:5432/buztrack'),
  JWT_SECRET: z.string().default('buztrack_dev_secret_key_must_be_32_characters_long_min'),
  JWT_EXPIRES_IN: z.string().default('7d'),
  REFRESH_TOKEN_SECRET: z.string().default('buztrack_dev_refresh_secret_key_32_chars_min'),
  STORAGE_DRIVER: z.enum(['local', 's3', 'gcs']).default('local'),
  OCR_PROVIDER: z.enum(['mock', 'google_vision', 'tesseract']).default('mock'),
  PAYMENT_GATEWAY_PROVIDER: z.enum(['mock', 'razorpay', 'cashfree']).default('mock'),
  NOTIFICATION_PROVIDER: z.enum(['mock', 'fcm']).default('mock'),
});

export const env = envSchema.parse(process.env);
