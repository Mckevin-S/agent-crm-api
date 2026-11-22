import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, debounceTime, distinctUntilChanged, filter, of, switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CrmStatusService } from '../../core/crm-status.service';
import { CustomerSummary } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { LogoComponent } from '../../shared/logo.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
  managerOnly?: boolean;
}

/** Cadre de l'application : navigation, recherche rapide de client et état du CRM. */
@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ReactiveFormsModule, MatIconModule, MatButtonModule, MatMenuModule,
    MatTooltipModule, MatAutocompleteModule, MatSlideToggleModule, AvatarComponent, LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotifyService);
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly crm = inject(CrmStatusService);

  readonly nav: NavItem[] = [
    { path: 'accueil', label: 'Tableau de bord', icon: 'space_dashboard' },
    { path: 'clients', label: 'Clients', icon: 'groups' },
    { path: 'taches', label: 'Tâches', icon: 'task_alt' },
    { path: 'equipe', label: 'Équipe', icon: 'insights', managerOnly: true },
  ];
  readonly search = new FormControl('', { nonNullable: true });
  readonly suggestions = signal<CustomerSummary[]>([]);
  readonly togglingOutage = signal(false);

  ngOnInit(): void {
    this.crm.start();
    this.search.valueChanges
      .pipe(
        debounceTime(200),
        distinctUntilChanged(),
        filter((q) => typeof q === 'string'),
        switchMap((q) => (q.trim().length < 2 ? of(null) : this.api.search(q).pipe(catchError(() => of(null))))),
      )
      .subscribe((result) => {
        this.suggestions.set(result?.customers.slice(0, 6) ?? []);
        if (result) {
          this.crm.report(result.live);
        }
      });
  }

  ngOnDestroy(): void {
    this.crm.stop();
  }

  open(event: MatAutocompleteSelectedEvent): void {
    const id = event.option.value as string;
    this.search.setValue('', { emitEvent: false });
    this.suggestions.set([]);
    this.router.navigate(['/clients', id]);
  }

  submitSearch(): void {
    const q = this.search.value.trim();
    this.router.navigate(['/clients'], { queryParams: q ? { q } : {} });
  }

  toggleOutage(outage: boolean): void {
    this.togglingOutage.set(true);
    this.api.simulateOutage(outage).subscribe({
      next: (state) => {
        this.crm.update(state);
        this.crm.report(!outage);
        this.notify.success(outage ? 'Panne du CRM simulée : l’application passe en mode dégradé.' : 'CRM rétabli.');
        this.togglingOutage.set(false);
      },
      error: (e) => {
        this.notify.error(e);
        this.togglingOutage.set(false);
      },
    });
  }
}
