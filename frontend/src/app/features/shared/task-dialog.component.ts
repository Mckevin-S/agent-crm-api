import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { Priority, Task } from '../../core/models';
import { errorMessage } from '../../core/notify.service';
import { addDays, isoDate } from './dates';

@Component({
  selector: 'app-task-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatButtonToggleModule,
    MatDatepickerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Nouvelle tâche pour {{ data.customerName }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-form-field appearance="outline">
          <mat-label>Tâche</mat-label>
          <input matInput formControlName="title" maxlength="160" cdkFocusInitial placeholder="Ex. Envoyer le devis habitation" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="3"></textarea>
        </mat-form-field>
        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Échéance</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="dueDate" [min]="today" />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
          </mat-form-field>
          <mat-button-toggle-group formControlName="priority" aria-label="Priorité">
            <mat-button-toggle value="LOW">Basse</mat-button-toggle>
            <mat-button-toggle value="MEDIUM">Moyenne</mat-button-toggle>
            <mat-button-toggle value="HIGH">Haute</mat-button-toggle>
          </mat-button-toggle-group>
        </div>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()">Créer la tâche</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `.row { display: flex; gap: 12px; align-items: baseline; flex-wrap: wrap; } .row mat-form-field { flex: 1; } .error { color: var(--danger); }`,
})
export class TaskDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<TaskDialogComponent, Task>);
  readonly data = inject<{ customerId: string; customerName: string }>(MAT_DIALOG_DATA);

  readonly today = new Date();
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.group({
    title: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(160)]),
    notes: this.fb.control<string | null>(null),
    priority: this.fb.nonNullable.control<Priority>('MEDIUM'),
    dueDate: this.fb.control<Date | null>(addDays(3), Validators.required),
  });

  submit(): void {
    const v = this.form.getRawValue();
    if (this.form.invalid || !v.dueDate) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.api.createTask(this.data.customerId, { title: v.title, notes: v.notes, priority: v.priority, dueDate: isoDate(v.dueDate) })
      .subscribe({
        next: (task) => this.dialogRef.close(task),
        error: (e) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
  }
}
