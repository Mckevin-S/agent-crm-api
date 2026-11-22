import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const session = {
    token: 'jwt',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    agent: { id: 1, email: 'sophie@demo.fr', fullName: 'Sophie Bernard', manager: true, initials: 'SB' },
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('conserve la session et le rôle après connexion', () => {
    service.login('sophie@demo.fr', 'demo1234').subscribe();
    http.expectOne('/api/auth/login').flush(session);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.isManager()).toBeTrue();
    expect(service.token()).toBe('jwt');
    expect(localStorage.getItem('agent-crm.session')).toContain('jwt');
  });

  it('efface la session et redirige à la déconnexion', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    service.login('sophie@demo.fr', 'demo1234').subscribe();
    http.expectOne('/api/auth/login').flush(session);

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/connexion']);
  });

  it('ignore une session enregistrée qui a expiré', () => {
    localStorage.setItem('agent-crm.session', JSON.stringify({ ...session, expiresAt: '2000-01-01T00:00:00Z' }));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });

    expect(TestBed.inject(AuthService).isAuthenticated()).toBeFalse();
  });
});
