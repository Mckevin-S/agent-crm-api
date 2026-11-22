import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Bandeau du mode dégradé : les données affichées viennent de la dernière copie locale. */
@Component({
  selector: 'app-degraded-banner',
  standalone: true,
  imports: [MatIconModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="banner" role="status" data-testid="degraded-banner">
      <mat-icon>cloud_off</mat-icon>
      <div>
        <strong>CRM indisponible : mode dégradé</strong>
        <span>Données issues de la dernière copie locale@if (fetchedAt()) { du {{ fetchedAt() | date: "d MMM à HH:mm" }}}. Vos actions (échanges, tâches) restent enregistrées.</span>
      </div>
    </div>
  `,
  styles: `
    .banner { display: flex; gap: 12px; align-items: flex-start; padding: 12px 16px; border-radius: var(--radius);
      background: var(--prio-high-bg); color: var(--prio-high); border: 1px solid currentColor; margin-bottom: 16px; }
    .banner div { display: flex; flex-direction: column; }
    .banner span { color: var(--ink-soft); font-size: 13px; }
  `,
})
export class DegradedBannerComponent {
  readonly fetchedAt = input<string | null | undefined>(null);
}
