import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { CrmStatusService } from '../../core/crm-status.service';
import { CHANNEL_ICONS, Customer360, PRODUCT_ICONS, Recommendation, Task } from '../../core/models';
import { NotifyService, errorMessage } from '../../core/notify.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { DegradedBannerComponent } from '../shared/degraded-banner.component';
import { FollowUpActions } from '../shared/follow-up.service';
import { InteractionDialogComponent } from '../shared/interaction-dialog.component';
import { MoneyPipe } from '../shared/money.pipe';
import { RecommendationCardComponent } from '../shared/recommendation-card.component';
import { TaskDialogComponent } from '../shared/task-dialog.component';
import { TaskRowComponent } from '../shared/task-row.component';

/** Vue client à 360° : identité, indicateurs, recommandations, contrats, sinistres, tâches et échanges. */
@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTabsModule,
    MatTooltipModule, AvatarComponent, EmptyStateComponent, RelativeTimePipe, DegradedBannerComponent, MoneyPipe,
    RecommendationCardComponent, TaskRowComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './customer-detail.component.html',
  styleUrl: './customer-detail.component.scss',
})
export class CustomerDetailComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly actions = inject(FollowUpActions);
  private readonly crm = inject(CrmStatusService);

  readonly id = input.required<string>();
  readonly view = signal<Customer360 | null>(null);
  readonly error = signal<{ message: string; forbidden: boolean } | null>(null);
  readonly busy = signal(false);
  readonly productIcons = PRODUCT_ICONS;
  readonly channelIcons = CHANNEL_ICONS;

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.load(id));
    });
  }

  load(id = this.id()): void {
    this.api.customer(id).subscribe({
      next: (v) => {
        this.view.set(v);
        this.error.set(null);
        this.crm.report(v.live);
        this.busy.set(false);
      },
      error: (e: unknown) => {
        this.busy.set(false);
        this.view.set(null);
        this.error.set({ message: errorMessage(e), forbidden: e instanceof HttpErrorResponse && e.status === 403 });
        if (e instanceof HttpErrorResponse && e.status === 503) {
          this.crm.report(false);
        }
      },
    });
  }

  accept(r: Recommendation): void {
    this.busy.set(true);
    this.actions.accept(this.id(), r).subscribe({ next: () => this.load(), error: (e) => this.fail(e) });
  }

  dismiss(r: Recommendation): void {
    this.actions.dismiss(this.id(), r).subscribe({ next: () => this.load(), error: (e) => this.fail(e) });
  }

  complete(t: Task): void {
    this.actions.complete(t).subscribe({ next: () => this.load(), error: (e) => this.fail(e) });
  }

  reschedule(t: Task): void {
    this.actions.reschedule(t).subscribe({ next: (r) => r && this.load(), error: (e) => this.fail(e) });
  }

  logInteraction(): void {
    const v = this.view();
    this.dialog
      .open(InteractionDialogComponent, { data: { customerId: this.id(), customerName: v?.customer.fullName }, width: '640px' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.notify.success('Échange enregistré.');
          this.load();
        }
      });
  }

  newTask(): void {
    const v = this.view();
    this.dialog
      .open(TaskDialogComponent, { data: { customerId: this.id(), customerName: v?.customer.fullName }, width: '560px' })
      .afterClosed()
      .subscribe((task) => {
        if (task) {
          this.notify.success('Tâche créée.');
          this.load();
        }
      });
  }

  private fail(e: unknown): void {
    this.notify.error(e);
    this.load();
  }
}
