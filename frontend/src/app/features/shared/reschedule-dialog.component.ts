import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Task } from '../../core/models';
import { addDays, isoDate } from './dates';

@Component({
  selector: 'app-reschedule-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatDatepickerModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Reporter « {{ data.title }} »</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline">
        <mat-label>Nouvelle échéance</mat-label>
        <input matInput [matDatepicker]="picker" [(ngModel)]="date" [min]="today" />
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="!date" (click)="dialogRef.close(iso())">Reporter</button>
    </mat-dialog-actions>
  `,
})
export class RescheduleDialogComponent {
  readonly dialogRef = inject(MatDialogRef<RescheduleDialogComponent, string>);
  readonly data = inject<Task>(MAT_DIALOG_DATA);
  readonly today = new Date();
  date: Date | null = addDays(7);

  iso(): string | undefined {
    return this.date ? isoDate(this.date) : undefined;
  }
}
