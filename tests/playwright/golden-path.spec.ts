// L1-testing-script-generator · Phase 5 · Correlation ID C885C23C-949E-440C-8B0E-04FDD166A559
// Inputs: test-cases.feature, openapi.yaml, wireframes.md
// Covers: S1 (generate), S14 (dashboard shows filings), S5/S7 (edit + approve golden path)
import { test, expect } from '@playwright/test';

test.describe('Automated Regulatory Filing Module — golden path', () => {
  test('executive simulates a trade, legal reviews and approves it', async ({ page }) => {
    // Executive: trigger a demo trade (stands in for a real EquatePlus transaction — FR-001)
    await page.goto('/');
    await page.getByRole('button', { name: /simulate trade/i }).click();
    await expect(page.getByText(/form 4 generated/i)).toBeVisible();

    // Legal & Compliance: find it on the dashboard
    await page.goto('/dashboard');
    await page.getByRole('row', { name: /under review/i }).first().click();

    // Review screen: approve
    await expect(page.getByRole('button', { name: /approve & submit/i })).toBeEnabled();
    await page.getByRole('button', { name: /approve & submit/i }).click();
    await expect(page.getByText(/submitted/i)).toBeVisible();

    // Executive: sees submission confirmation notification (S13)
    await page.goto('/');
    await page.getByRole('button', { name: /notifications/i }).click();
    await expect(page.getByText(/submitted/i)).toBeVisible();
  });

  test('legal rejects an invalid edit (S6)', async ({ page }) => {
    await page.goto('/dashboard');
    await page.getByRole('row', { name: /under review/i }).first().click();
    await page.getByLabel(/shares/i).fill('-5');
    await page.getByRole('button', { name: /save edits/i }).click();
    await expect(page.getByText(/must be/i)).toBeVisible();
  });
});
