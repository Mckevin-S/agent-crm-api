import { expect, test } from '@playwright/test';
import { loginAs, openCustomer } from './helpers';

test.describe('Conseiller', () => {
  test('le tableau de bord présente les actions recommandées du portefeuille', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Mckevin');
    await expect(page.getByText('Actions recommandées')).toBeVisible();
    await expect(page.getByTestId('reco-RENEWAL_SOON').first()).toBeVisible();
  });

  test('transforme une recommandation en tâche puis la termine', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await openCustomer(page, 'Martin', 'Claire Martin');

    const renewal = page.getByTestId('reco-RENEWAL_SOON');
    await expect(renewal).toBeVisible();
    await renewal.getByTestId('accept').click();
    await expect(renewal).toHaveCount(0);

    await page.getByTestId('nav-taches').click();
    const row = page.locator('[data-testid^="task-"]', { hasText: 'renouvellement' }).first();
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: /comme faite/ }).click();
    await expect(page.locator('[data-testid^="task-"]', { hasText: 'renouvellement' })).toHaveCount(0);
  });

  test('écarte une recommandation avec un motif et trace un échange', async ({ page }) => {
    await loginAs(page, 'mckevin');
    await openCustomer(page, 'Martin', 'Claire Martin');

    const protection = page.getByTestId('reco-PROTECTION_CROSS_SELL');
    await protection.getByTestId('dismiss').click();
    await page.getByTestId('dismiss-reason').fill('Déjà couverte par le contrat de son employeur');
    await page.getByTestId('dismiss-submit').click();
    await expect(protection).toHaveCount(0);

    await page.getByTestId('log-interaction').click();
    await page.getByTestId('interaction-subject').fill('Point sur le renouvellement auto');
    await page.getByTestId('interaction-submit').click();
    await expect(page.getByText('Point sur le renouvellement auto')).toBeVisible();
  });

  test("ne peut pas ouvrir la fiche d'un client hors de son portefeuille", async ({ page }) => {
    await loginAs(page, 'mckevin');
    await page.goto('/clients/C-10242');
    await expect(page.getByText('Accès refusé')).toBeVisible();
    await expect(page.getByTestId('nav-equipe')).toHaveCount(0);
  });
});
