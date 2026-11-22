import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiService } from './api.service';
import { CrmStatusService } from './crm-status.service';
import { CrmState } from './models';

describe('CrmStatusService', () => {
  let service: CrmStatusService;
  const state = (available: boolean, outageSimulated = false) => ({ available, outageSimulated }) as CrmState;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ApiService, useValue: { crmStatus: () => of(state(true)) } }],
    });
    service = TestBed.inject(CrmStatusService);
  });

  it("n'est pas dégradé tant que rien n'est connu", () => {
    expect(service.degraded()).toBeFalse();
  });

  it('passe en mode dégradé quand le CRM est indisponible', () => {
    service.update(state(false));
    expect(service.degraded()).toBeTrue();
  });

  it("passe en mode dégradé dès qu'une vue reçoit une copie locale", () => {
    service.update(state(true));
    service.report(false);
    expect(service.degraded()).toBeTrue();
  });

  it('sort du mode dégradé quand le CRM répond de nouveau', () => {
    service.report(false);
    service.update(state(true));
    expect(service.degraded()).toBeFalse();
  });

  it("reste dégradé tant que la panne simulée n'est pas levée", () => {
    service.report(false);
    service.update(state(true, true));
    expect(service.degraded()).toBeTrue();
  });
});
