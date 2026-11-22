import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, filter, map, of, switchMap, tap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Recommendation, Task } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { DismissDialogComponent } from './dismiss-dialog.component';
import { RescheduleDialogComponent } from './reschedule-dialog.component';

/** Actions de suivi partagées entre le tableau de bord, la fiche client et la liste des tâches. */
@Injectable({ providedIn: 'root' })
export class FollowUpActions {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);

  accept(customerId: string, r: Recommendation): Observable<Task> {
    return this.api.accept(customerId, r.code, r.reference).pipe(
      tap(() => this.notify.success('Tâche créée : ' + r.title)),
    );
  }

  /** Ouvre la saisie du motif ; émet uniquement si la recommandation a bien été écartée. */
  dismiss(customerId: string, r: Recommendation): Observable<void> {
    return this.dialog.open(DismissDialogComponent, { data: { title: r.title }, width: '520px' }).afterClosed().pipe(
      filter((reason): reason is string => !!reason),
      switchMap((reason) => this.api.dismiss(customerId, r.code, r.reference, reason)),
      tap(() => this.notify.success('Recommandation écartée pour 90 jours.')),
    );
  }

  complete(task: Task): Observable<Task> {
    return this.api.completeTask(task.id).pipe(tap(() => this.notify.success('Tâche terminée.')));
  }

  reschedule(task: Task): Observable<Task | null> {
    return this.dialog.open(RescheduleDialogComponent, { data: task, width: '420px' }).afterClosed().pipe(
      switchMap((date: string | undefined) => (date ? this.api.rescheduleTask(task.id, date) : of(null))),
      map((t) => t),
      tap((t) => t && this.notify.success('Échéance reportée.')),
    );
  }
}
