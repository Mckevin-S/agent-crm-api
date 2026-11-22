import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';
import { Subscription, switchMap, timer } from 'rxjs';
import { ApiService } from './api.service';
import { CrmState } from './models';

/**
 * Suit l'état du CRM : interrogé régulièrement, et mis à jour dès qu'une réponse de l'API indique
 * que les données viennent d'une copie locale (mode dégradé).
 */
@Injectable({ providedIn: 'root' })
export class CrmStatusService implements OnDestroy {
  private readonly api = inject(ApiService);
  private polling?: Subscription;

  readonly state = signal<CrmState | null>(null);
  private readonly staleSeen = signal(false);
  readonly degraded = computed(() => this.staleSeen() || (this.state() !== null && !this.state()!.available));

  start(): void {
    if (this.polling) {
      return;
    }
    this.polling = timer(0, 15_000)
      .pipe(switchMap(() => this.api.crmStatus()))
      .subscribe({ next: (s) => this.update(s), error: () => this.state.set(null) });
  }

  stop(): void {
    this.polling?.unsubscribe();
    this.polling = undefined;
  }

  /** Une vue a reçu des données : live=false signale une copie locale. */
  report(live: boolean): void {
    this.staleSeen.set(!live);
  }

  update(state: CrmState): void {
    this.state.set(state);
    if (state.available && !state.outageSimulated) {
      this.staleSeen.set(false);
    }
  }

  refresh(): void {
    this.api.crmStatus().subscribe((s) => this.update(s));
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
