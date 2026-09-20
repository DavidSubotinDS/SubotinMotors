import { defineConfig, devices } from '@playwright/test';
import { resolve } from 'node:path';

if (!process.env.E2E_CONTROL_TOKEN) throw new Error('Use npm run test:e2e for isolated startup and cleanup.');

export default defineConfig({
  testDir: './e2e/specs',
  fullyParallel: false,
  workers: 1, // The test-only database and business clock reset before EVERY test (including retries).
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 45000,
  expect: { timeout: 8000 },
  outputDir: process.env.E2E_REPORT_ROOT ? resolve(process.env.E2E_REPORT_ROOT, 'results') : 'test-results/playwright',
  reporter: [['list'], ['html', { outputFolder: process.env.E2E_REPORT_ROOT ? resolve(process.env.E2E_REPORT_ROOT, 'html') : 'playwright-report', open: 'never' }],
    ['junit', { outputFile: process.env.E2E_REPORT_ROOT ? resolve(process.env.E2E_REPORT_ROOT, 'playwright.xml') : 'e2e-results/playwright.xml' }]],
  use: {
    baseURL: process.env.E2E_GATEWAY_URL || 'http://127.0.0.1:18081',
    timezoneId: 'UTC', locale: 'en-GB',
    trace: 'retain-on-failure', screenshot: 'only-on-failure', video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
