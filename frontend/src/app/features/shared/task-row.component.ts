import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Task } from '../../core/models';
import { PriorityChipComponent } from './priority-chip.component';

@Component({
  selector: 'app-task-row',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatTooltipModule, PriorityChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="task" [class.done]="task().status !== 'OPEN'" [attr.data-testid]="'task-' + task().id">
      @if (task().status === 'OPEN') {
        <button class="check" (click)="completed.emit(task())" matTooltip="Marquer comme faite"
                [attr.aria-label]="'Marquer « ' + task().title + ' » comme faite'"><mat-icon>radio_button_unchecked</mat-icon></button>
      } @else {
        <mat-icon class="check-done" aria-label="Faite">check_circle</mat-icon>
      }
      <div class="body">
        <span class="title">{{ task().title }}</span>
        <span class="meta">
          @if (showCustomer() && task().customerName) {
            <a [routerLink]="['/clients', task().customerId]">{{ task().customerName }}</a> ·
          }
          <span [class.overdue]="task().overdue">
            {{ task().overdue ? 'En retard – ' : '' }}échéance {{ task().dueDate | date: 'EEE d MMM' }}
          </span>
          @if (task().recommendationCode) {
            · <span class="from-reco">issue d'une recommandation</span>
          }
        </span>
      </div>
      <app-priority-chip [priority]="task().priority" />
      @if (task().status === 'OPEN') {
        <button mat-icon-button (click)="rescheduled.emit(task())" matTooltip="Reporter" aria-label="Reporter l'échéance">
          <mat-icon>event_repeat</mat-icon>
        </button>
      }
    </div>
  `,
  styles: `
    .task { display: flex; align-items: center; gap: 12px; padding: 10px 4px; border-bottom: 1px solid var(--border); }
    .task:last-child { border-bottom: none; }
    .check { background: none; border: none; cursor: pointer; color: var(--muted); display: flex; padding: 0; }
    .check:hover { color: var(--success); }
    .check-done { color: var(--success); }
    .body { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .title { font-weight: 500; }
    .done .title { text-decoration: line-through; color: var(--muted); }
    .meta { font-size: 12px; color: var(--muted); }
    .meta a { text-decoration: none; font-weight: 500; }
    .overdue { color: var(--danger); font-weight: 600; }
    .from-reco { color: var(--accent); }
  `,
})
export class TaskRowComponent {
  readonly task = input.required<Task>();
  readonly showCustomer = input(true);
  readonly completed = output<Task>();
  readonly rescheduled = output<Task>();
}
