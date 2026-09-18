import { defineConfig, devices } from '@playwright/test';

if (!process.env.E2E_CONTROL_TOKEN) throw new Error('Use npm run test:e2e for isolated startup and cleanup.');

export default defineConfig({
  testDir: './e2e/specs',
  fullyParallel: false,
  workers: 1, // The test-only database and business clock reset before EVERY test (including retries).
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 45000,
  expect: { timeout: 8000 },
  outputDir: 'test-results/playwright',
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'e2e-results/playwright.xml' }]],
  use: {
    baseURL: 'http://127.0.0.1:18081',
    timezoneId: 'UTC', locale: 'en-GB',
    trace: 'retain-on-failure', screenshot: 'only-on-failure', video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
