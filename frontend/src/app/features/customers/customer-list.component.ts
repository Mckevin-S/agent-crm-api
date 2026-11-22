import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { debounceTime, distinctUntilChanged, startWith, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CrmStatusService } from '../../core/crm-status.service';
import { SearchResult } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { DegradedBannerComponent } from '../shared/degraded-banner.component';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatIconModule, MatProgressBarModule, AvatarComponent, EmptyStateComponent,
    DegradedBannerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Clients</h1>
          <p>{{ auth.isManager() ? "Tous les clients de l'agence" : 'Les clients de votre portefeuille' }}.</p>
        </div>
        <label class="search">
          <mat-icon>search</mat-icon>
          <input [formControl]="query" placeholder="Nom, n° client, ville…" aria-label="Filtrer les clients" data-testid="customer-filter" />
        </label>
      </div>
      @if (result(); as r) {
        @if (!r.live) {
          <app-degraded-banner />
        }
        <div class="card table">
          <div class="row header">
            <span>Client</span><span>N° client</span><span>Ville</span><span>Segment</span><span>Conseiller</span>
          </div>
          @for (s of r.customers; track s.customer.id) {
            <a class="row" [routerLink]="['/clients', s.customer.id]" [attr.data-testid]="'customer-' + s.customer.id">
              <span class="name"><app-avatar [person]="s.customer" [size]="30" /> {{ s.customer.fullName }}</span>
              <span class="mono">{{ s.customer.id }}</span>
              <span>{{ s.customer.city }}</span>
              <span><span class="segment" [class.pro]="s.customer.segment === 'PROFESSIONAL'">{{ s.customer.segmentLabel }}</span></span>
              <span class="muted">{{ s.advisorName ?? '–' }}</span>
            </a>
          } @empty {
            <app-empty-state icon="person_search" title="Aucun client trouvé" message="Essayez un autre nom ou numéro client." />
          }
        </div>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .search { display: flex; align-items: center; gap: 6px; border: 1px solid var(--border); background: var(--surface); border-radius: 8px;
      padding: 6px 10px; min-width: 300px; }
    .search:focus-within { border-color: var(--accent); }
    .search mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--muted); }
    .search input { border: none; outline: none; background: transparent; font: inherit; color: var(--ink); flex: 1; }
    .table { overflow: hidden; }
    .row { display: grid; grid-template-columns: 2.2fr 1fr 1.3fr 1fr 1.4fr; gap: 12px; align-items: center; padding: 10px 16px;
      border-bottom: 1px solid var(--border); color: inherit; text-decoration: none; }
    a.row:hover { background: var(--surface-2); }
    .row:last-child { border-bottom: none; }
    .header { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--muted); font-weight: 600; background: var(--surface-2); }
    .name { display: flex; align-items: center; gap: 10px; font-weight: 600; }
    .segment { font-size: 12px; padding: 2px 8px; border-radius: 6px; background: var(--accent-soft); color: var(--accent); font-weight: 600; }
    .segment.pro { background: var(--prio-medium-bg); color: var(--prio-medium); }
  `,
})
export class CustomerListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly notify = inject(NotifyService);
  private readonly crm = inject(CrmStatusService);
  readonly auth = inject(AuthService);

  readonly query = new FormControl(this.route.snapshot.queryParamMap.get('q') ?? '', { nonNullable: true });
  readonly result = signal<SearchResult | null>(null);

  ngOnInit(): void {
    this.query.valueChanges
      .pipe(startWith(this.query.value), debounceTime(150), distinctUntilChanged(), switchMap((q) => this.api.search(q)))
      .subscribe({
        next: (r) => {
          this.result.set(r);
          this.crm.report(r.live);
          this.router.navigate([], { queryParams: this.query.value ? { q: this.query.value } : {}, replaceUrl: true });
        },
        error: (e) => this.notify.error(e),
      });
  }
}
