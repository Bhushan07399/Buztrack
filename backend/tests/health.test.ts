import { describe, it, expect } from 'vitest';
import { app } from '../src/app.js';

describe('Health & API Infrastructure', () => {
  it('should return app health status', async () => {
    // Basic test checking app instance definition
    expect(app).toBeDefined();
  });
});
