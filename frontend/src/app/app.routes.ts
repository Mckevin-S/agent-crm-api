import { Routes } from '@angular/router';
import { authGuard, guestGuard, managerGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'connexion',
    canActivate: [guestGuard],
    title: 'Connexion · Relais',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'accueil' },
      {
        path: 'accueil',
        title: 'Tableau de bord · Relais',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'clients',
        title: 'Clients · Relais',
        loadComponent: () => import('./features/customers/customer-list.component').then((m) => m.CustomerListComponent),
      },
      {
        path: 'clients/:id',
        title: 'Fiche client · Relais',
        loadComponent: () => import('./features/customers/customer-detail.component').then((m) => m.CustomerDetailComponent),
      },
      {
        path: 'taches',
        title: 'Tâches · Relais',
        loadComponent: () => import('./features/tasks/tasks.component').then((m) => m.TasksComponent),
      },
      {
        path: 'equipe',
        canActivate: [managerGuard],
        title: 'Équipe · Relais',
        loadComponent: () => import('./features/team/team.component').then((m) => m.TeamComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
