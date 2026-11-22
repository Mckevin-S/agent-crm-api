import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();
  const normalize = (s: string) => s.replace(/\s/g, ' ');

  it('formate en euros sans centimes', () => {
    expect(normalize(pipe.transform(1234.56))).toBe('1 235 €');
  });

  it('affiche un tiret pour une valeur absente', () => {
    expect(pipe.transform(null)).toBe('–');
    expect(pipe.transform(undefined)).toBe('–');
  });
});
