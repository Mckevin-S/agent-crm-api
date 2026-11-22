import { addDays, isoDate } from './dates';

describe('dates', () => {
  it('formate une date locale en AAAA-MM-JJ', () => {
    expect(isoDate(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });

  it('ajoute des jours en franchissant un changement de mois', () => {
    expect(isoDate(addDays(3, new Date(2026, 10, 29)))).toBe('2026-12-02');
  });
});
