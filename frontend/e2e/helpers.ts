import { Page, expect } from '@playwright/test';

export type DemoAccount = 'mckevin' | 'lucas' | 'sophie';

/** Connexion avec un compte de démonstration via le raccourci de l'écran de connexion. */
export async function loginAs(page: Page, who: DemoAccount): Promise<void> {
  await page.goto('/connexion');
  await page.getByTestId(`demo-${who}@demo.fr`).click();
  await expect(page).toHaveURL(/\/accueil$/);
}

/** Ouvre une fiche client depuis la recherche globale. */
export async function openCustomer(page: Page, query: string, fullName: string): Promise<void> {
  await page.getByTestId('global-search').fill(query);
  await page.getByRole('option', { name: new RegExp(fullName) }).click();
  await expect(page.getByRole('heading', { level: 1, name: new RegExp(fullName) })).toBeVisible();
}
