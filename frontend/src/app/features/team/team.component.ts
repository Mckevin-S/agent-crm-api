import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChartConfiguration } from 'chart.js';
import { ApiService } from '../../core/api.service';
import { TeamMember } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { ChartComponent } from '../../shared/chart.component';

function cssVar(name: string, fallback: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

/** Vue manager : charge et activité de chaque conseiller. */
@Component({
  selector: 'app-team',
  standalone: true,
  imports: [MatIconModule, MatProgressBarModule, AvatarComponent, ChartComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Équipe</h1><p>Charge de travail et activité des conseillers de l'agence.</p></div>
      </div>
      @if (team(); as members) {
        <div class="grid">
          <section class="card table">
            <div class="row header">
              <span>Conseiller</span><span>Clients</span><span>Tâches ouvertes</span><span>En retard</span>
              <span>Actions conseillées</span><span>Échanges (30 j)</span>
            </div>
            @for (m of members; track m.agentId) {
              <div class="row" [attr.data-testid]="'member-' + m.agentId">
                <span class="name"><app-avatar [person]="person(m)" [size]="30" /> {{ m.fullName }}</span>
                <span>{{ m.customers }}</span>
                <span>{{ m.openTasks }}</span>
                <span [class.alert]="m.overdueTasks > 0">{{ m.overdueTasks }}</span>
                <span>{{ m.openRecommendations }}</span>
                <span>{{ m.interactionsLast30Days }}</span>
              </div>
            }
          </section>
          <section class="card chart">
            <h2>Charge par conseiller</h2>
            @if (chart(); as config) {
              <app-chart [config]="config" [height]="260" label="Charge par conseiller" />
            }
          </section>
        </div>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; align-items: start; }
    .table { overflow: hidden; }
    .row { display: grid; grid-template-columns: 2fr repeat(5, 1fr); gap: 12px; align-items: center; padding: 12px 16px;
      border-bottom: 1px solid var(--border); }
    .row:last-child { border-bottom: none; }
    .header { font-size: 11px; text-transform: uppercase; letter-spacing: 0.05em; color: var(--muted); font-weight: 600; background: var(--surface-2); }
    .name { display: flex; align-items: center; gap: 10px; font-weight: 600; }
    .alert { color: var(--danger); font-weight: 700; }
    .chart { padding: 20px; }
    .chart h2 { margin: 0 0 12px; font-size: 16px; }
    @media (max-width: 1100px) { .grid { grid-template-columns: 1fr; } }
  `,
})
export class TeamComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly theme = inject(ThemeService);
  readonly team = signal<TeamMember[] | null>(null);

  readonly chart = computed<ChartConfiguration | null>(() => {
    this.theme.theme();
    const members = this.team();
    if (!members?.length) {
      return null;
    }
    const text = cssVar('--muted', '#66717f');
    const grid = cssVar('--border', '#e1e6eb');
    return {
      type: 'bar',
      data: {
        labels: members.map((m) => m.fullName),
        datasets: [
          { label: 'Tâches ouvertes', data: members.map((m) => m.openTasks), backgroundColor: cssVar('--accent', '#0e7c7b'), borderRadius: 6 },
          { label: 'En retard', data: members.map((m) => m.overdueTasks), backgroundColor: cssVar('--prio-high', '#c2410c'), borderRadius: 6 },
          { label: 'Actions conseillées', data: members.map((m) => m.openRecommendations), backgroundColor: cssVar('--prio-medium', '#1d5bd6'), borderRadius: 6 },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: text, usePointStyle: true, boxWidth: 8 } } },
        scales: {
          x: { grid: { display: false }, ticks: { color: text } },
          y: { beginAtZero: true, grid: { color: grid }, ticks: { color: text, precision: 0 } },
        },
      },
    };
  });

  ngOnInit(): void {
    this.api.team().subscribe({ next: (t) => this.team.set(t), error: (e) => this.notify.error(e) });
  }

  person(m: TeamMember) {
    const initials = m.fullName.split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
    return { id: m.agentId, fullName: m.fullName, initials };
  }
}
