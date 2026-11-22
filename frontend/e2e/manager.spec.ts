import { expect, test } from '@playwright/test';
import { loginAs, openCustomer } from './helpers';

test.describe("Manager d'agence", () => {
  test("suit la charge de l'équipe", async ({ page }) => {
    await loginAs(page, 'sophie');
    await page.getByTestId('nav-equipe').click();
    await expect(page.getByRole('heading', { name: 'Équipe' })).toBeVisible();
    await expect(page.locator('[data-testid^="member-"]')).toHaveCount(2);
    await expect(page.getByText('Mckevin Kamguem')).toBeVisible();
    await expect(page.getByText('Lucas Morel')).toBeVisible();
  });

  test('bascule en mode dégradé pendant une panne du CRM, puis revient à la normale', async ({ page }) => {
    await loginAs(page, 'sophie');
    // Fiche consultée une première fois : une copie locale est conservée
    await openCustomer(page, 'Dubois', 'Antoine Dubois');

    await page.getByTestId('crm-status').click();
    await page.getByTestId('outage-toggle').click();
    await page.keyboard.press('Escape');
    await expect(page.getByTestId('crm-status')).toContainText('Mode dégradé');

    await page.reload();
    await expect(page.getByTestId('degraded-banner')).toBeVisible();
    await expect(page.getByRole('heading', { level: 1, name: /Antoine Dubois/ })).toBeVisible();

    await page.getByTestId('crm-status').click();
    await page.getByTestId('outage-toggle').click();
    await page.keyboard.press('Escape');
    await expect(page.getByTestId('crm-status')).toContainText('CRM connecté');
    await page.reload();
    await expect(page.getByTestId('degraded-banner')).toHaveCount(0);
  });
});
