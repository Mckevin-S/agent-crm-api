import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Channel,
  CrmState,
  Customer360,
  Dashboard,
  Direction,
  Interaction,
  Priority,
  SearchResult,
  Task,
  TeamMember,
} from './models';

export interface InteractionDraft {
  channel: Channel;
  direction: Direction;
  subject: string;
  notes?: string | null;
  followUpOn?: string | null;
  followUpTitle?: string | null;
}

export interface TaskDraft {
  title: string;
  notes?: string | null;
  priority: Priority;
  dueDate: string;
}

/** Client typé de l'API REST. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>('/api/dashboard');
  }

  team(): Observable<TeamMember[]> {
    return this.http.get<TeamMember[]>('/api/team');
  }

  search(query: string): Observable<SearchResult> {
    return this.http.get<SearchResult>('/api/customers', { params: { q: query } });
  }

  customer(id: string): Observable<Customer360> {
    return this.http.get<Customer360>(`/api/customers/${encodeURIComponent(id)}`);
  }

  logInteraction(customerId: string, draft: InteractionDraft): Observable<Interaction> {
    return this.http.post<Interaction>(`/api/customers/${customerId}/interactions`, draft);
  }

  createTask(customerId: string, draft: TaskDraft): Observable<Task> {
    return this.http.post<Task>(`/api/customers/${customerId}/tasks`, draft);
  }

  accept(customerId: string, code: string, reference: string): Observable<Task> {
    return this.http.post<Task>(`/api/customers/${customerId}/recommendations/${code}/accept`, { reference });
  }

  dismiss(customerId: string, code: string, reference: string, reason: string): Observable<void> {
    return this.http.post<void>(`/api/customers/${customerId}/recommendations/${code}/dismiss`, { reference, reason });
  }

  tasks(openOnly = false): Observable<Task[]> {
    return this.http.get<Task[]>('/api/tasks', { params: { openOnly } });
  }

  completeTask(id: number): Observable<Task> {
    return this.http.post<Task>(`/api/tasks/${id}/complete`, {});
  }

  rescheduleTask(id: number, dueDate: string): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}/due-date`, { dueDate });
  }

  crmStatus(): Observable<CrmState> {
    return this.http.get<CrmState>('/api/crm/status');
  }

  simulateOutage(outage: boolean): Observable<CrmState> {
    return this.http.put<CrmState>('/api/crm/outage', { outage });
  }
}
