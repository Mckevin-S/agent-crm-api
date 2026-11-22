import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Recommendation } from '../../core/models';
import { PriorityChipComponent } from './priority-chip.component';

/** Recommandation avec ses deux décisions possibles : la transformer en tâche, ou l'écarter. */
@Component({
  selector: 'app-recommendation-card',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, PriorityChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="reco" [attr.data-testid]="'reco-' + recommendation().code">
      <div class="head">
        <app-priority-chip [priority]="recommendation().priority" />
        @if (customerName()) {
          <a class="customer" [routerLink]="['/clients', customerId()]">{{ customerName() }}</a>
        }
        <span class="spacer"></span>
        <span class="muted due">à traiter avant le {{ recommendation().suggestedDueDate | date: 'd MMM' }}</span>
      </div>
      <h3>{{ recommendation().title }}</h3>
      <p class="muted">{{ recommendation().rationale }}</p>
      <div class="actions">
        <button mat-flat-button color="primary" (click)="accepted.emit(recommendation())" [disabled]="disabled()"
                data-testid="accept"><mat-icon>add_task</mat-icon> Créer la tâche</button>
        <button mat-button (click)="dismissed.emit(recommendation())" [disabled]="disabled()" data-testid="dismiss">Écarter</button>
      </div>
    </article>
  `,
  styles: `
    .reco { padding: 14px 16px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface);
      display: flex; flex-direction: column; gap: 4px; }
    .head { display: flex; align-items: center; gap: 8px; }
    .customer { font-weight: 600; font-size: 13px; text-decoration: none; }
    .due { font-size: 12px; }
    h3 { margin: 4px 0 0; font-size: 15px; }
    p { margin: 0; font-size: 13px; }
    .actions { display: flex; gap: 8px; margin-top: 8px; }
    .actions mat-icon { font-size: 18px; width: 18px; height: 18px; }
  `,
})
export class RecommendationCardComponent {
  readonly recommendation = input.required<Recommendation>();
  readonly customerId = input<string>();
  readonly customerName = input<string>();
  readonly disabled = input(false);
  readonly accepted = output<Recommendation>();
  readonly dismissed = output<Recommendation>();
}
