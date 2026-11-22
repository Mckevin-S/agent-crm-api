import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../../core/api.service';
import { Task } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { FollowUpActions } from '../shared/follow-up.service';
import { TaskRowComponent } from '../shared/task-row.component';
import { isoDate } from '../shared/dates';

interface Group {
  key: string;
  title: string;
  tasks: Task[];
}

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [MatButtonToggleModule, MatProgressBarModule, EmptyStateComponent, TaskRowComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page narrow">
      <div class="page-header">
        <div><h1>Mes tâches</h1><p>Relances, rendez-vous et régularisations, par échéance.</p></div>
        <mat-button-toggle-group [value]="showDone()" (change)="showDone.set($event.value)" aria-label="Filtre">
          <mat-button-toggle [value]="false">À faire</mat-button-toggle>
          <mat-button-toggle [value]="true">Toutes</mat-button-toggle>
        </mat-button-toggle-group>
      </div>
      @if (tasks(); as list) {
        @for (g of groups(); track g.key) {
          @if (g.tasks.length) {
            <section class="card group">
              <h2 [class]="g.key">{{ g.title }} <span class="count">{{ g.tasks.length }}</span></h2>
              @for (t of g.tasks; track t.id) {
                <app-task-row [task]="t" (completed)="complete($event)" (rescheduled)="reschedule($event)" />
              }
            </section>
          }
        }
        @if (!list.length) {
          <div class="card"><app-empty-state icon="task_alt" title="Aucune tâche" message="Vous êtes à jour." /></div>
        }
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .narrow { max-width: 960px; }
    .group { padding: 16px 20px; margin-bottom: 16px; }
    h2 { margin: 0 0 4px; font-size: 14px; text-transform: uppercase; letter-spacing: 0.05em; display: flex; align-items: center; gap: 8px; }
    h2.overdue { color: var(--danger); }
    h2.today { color: var(--accent); }
    .count { font-size: 12px; background: var(--surface-2); color: var(--muted); padding: 1px 8px; border-radius: 10px; }
  `,
})
export class TasksComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly actions = inject(FollowUpActions);

  readonly tasks = signal<Task[] | null>(null);
  readonly showDone = signal(false);
  readonly groups = computed<Group[]>(() => {
    const list = this.tasks() ?? [];
    const today = isoDate(new Date());
    const open = list.filter((t) => t.status === 'OPEN');
    const groups: Group[] = [
      { key: 'overdue', title: 'En retard', tasks: open.filter((t) => t.dueDate < today) },
      { key: 'today', title: "Aujourd'hui", tasks: open.filter((t) => t.dueDate === today) },
      { key: 'upcoming', title: 'À venir', tasks: open.filter((t) => t.dueDate > today) },
    ];
    if (this.showDone()) {
      groups.push({ key: 'done', title: 'Terminées', tasks: list.filter((t) => t.status !== 'OPEN') });
    }
    return groups;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.tasks(false).subscribe({ next: (t) => this.tasks.set(t), error: (e) => this.notify.error(e) });
  }

  complete(task: Task): void {
    this.actions.complete(task).subscribe({ next: () => this.load(), error: (e) => this.notify.error(e) });
  }

  reschedule(task: Task): void {
    this.actions.reschedule(task).subscribe({ next: (t) => t && this.load(), error: (e) => this.notify.error(e) });
  }
}
