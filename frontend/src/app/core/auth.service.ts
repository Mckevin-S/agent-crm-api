import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AgentProfile, Session } from './models';

const STORAGE_KEY = 'agent-crm.session';

/** Session du conseiller : jeton JWT et profil, conservés jusqu'à expiration. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(this.restore());

  readonly user = computed<AgentProfile | null>(() => this.session()?.agent ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isManager = computed(() => this.session()?.agent.manager ?? false);

  token(): string | null {
    return this.session()?.token ?? null;
  }

  login(email: string, password: string): Observable<Session> {
    return this.http.post<Session>('/api/auth/login', { email, password }).pipe(tap((s) => this.store(s)));
  }

  logout(redirect = true): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* stockage indisponible : rien à nettoyer */
    }
    if (redirect) {
      this.router.navigate(['/connexion']);
    }
  }

  private store(session: Session): void {
    this.session.set(session);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } catch {
      /* navigation privée : la session reste en mémoire */
    }
  }

  private restore(): Session | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) {
        return null;
      }
      const session = JSON.parse(raw) as Session;
      return new Date(session.expiresAt).getTime() > Date.now() ? session : null;
    } catch {
      return null;
    }
  }
}
