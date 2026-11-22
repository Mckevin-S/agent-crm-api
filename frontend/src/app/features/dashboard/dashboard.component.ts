import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CrmStatusService } from '../../core/crm-status.service';
import { Dashboard, PortfolioRecommendation, Task } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { DegradedBannerComponent } from '../shared/degraded-banner.component';
import { FollowUpActions } from '../shared/follow-up.service';
import { MoneyPipe } from '../shared/money.pipe';
import { RecommendationCardComponent } from '../shared/recommendation-card.component';
import { TaskRowComponent } from '../shared/task-row.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, EmptyStateComponent, DegradedBannerComponent,
    MoneyPipe, RecommendationCardComponent, TaskRowComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly actions = inject(FollowUpActions);
  private readonly crm = inject(CrmStatusService);
  readonly auth = inject(AuthService);

  readonly dashboard = signal<Dashboard | null>(null);
  readonly busy = signal(false);
  readonly firstName = computed(() => this.auth.user()?.fullName.split(' ')[0] ?? '');
  readonly greeting = computed(() => (new Date().getHours() < 18 ? 'Bonjour' : 'Bonsoir'));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.dashboard().subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.crm.report(d.live);
        this.busy.set(false);
      },
      error: (e) => {
        this.notify.error(e);
        this.busy.set(false);
      },
    });
  }

  accept(item: PortfolioRecommendation): void {
    this.busy.set(true);
    this.actions.accept(item.customerId, item.recommendation).subscribe({
      next: () => this.load(),
      error: (e) => {
        this.notify.error(e);
        this.load();
      },
    });
  }

  dismiss(item: PortfolioRecommendation): void {
    this.actions.dismiss(item.customerId, item.recommendation).subscribe({
      next: () => this.load(),
      error: (e) => this.notify.error(e),
    });
  }

  complete(task: Task): void {
    this.actions.complete(task).subscribe({ next: () => this.load(), error: (e) => this.notify.error(e) });
  }

  reschedule(task: Task): void {
    this.actions.reschedule(task).subscribe({ next: (t) => t && this.load(), error: (e) => this.notify.error(e) });
  }
}
