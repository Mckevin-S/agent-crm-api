import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { CHANNEL_ICONS, CHANNEL_LABELS, Channel, Direction, Interaction } from '../../core/models';
import { errorMessage } from '../../core/notify.service';
import { addDays, isoDate } from './dates';

@Component({
  selector: 'app-interaction-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatButtonToggleModule,
    MatCheckboxModule, MatDatepickerModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Tracer un échange avec {{ data.customerName }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <div class="row">
          <mat-button-toggle-group formControlName="channel" aria-label="Canal">
            @for (c of channels; track c) {
              <mat-button-toggle [value]="c"><mat-icon>{{ icons[c] }}</mat-icon> {{ labels[c] }}</mat-button-toggle>
            }
          </mat-button-toggle-group>
          <mat-button-toggle-group formControlName="direction" aria-label="Sens">
            <mat-button-toggle value="OUTBOUND">Sortant</mat-button-toggle>
            <mat-button-toggle value="INBOUND">Entrant</mat-button-toggle>
          </mat-button-toggle-group>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Objet</mat-label>
          <input matInput formControlName="subject" maxlength="120" cdkFocusInitial data-testid="interaction-subject" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="4" maxlength="2000"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="followUp">Programmer une relance</mat-checkbox>
        @if (form.controls.followUp.value) {
          <mat-form-field appearance="outline" class="date">
            <mat-label>Date de relance</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="followUpOn" [min]="today" />
            <mat-datepicker-toggle matIconSuffix [for]="picker" />
            <mat-datepicker #picker />
          </mat-form-field>
        }
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()" data-testid="interaction-submit">Enregistrer</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .row { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .row mat-icon { font-size: 18px; width: 18px; height: 18px; vertical-align: middle; }
    .date { margin-top: 12px; }
    .error { color: var(--danger); }
  `,
})
export class InteractionDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<InteractionDialogComponent, Interaction>);
  readonly data = inject<{ customerId: string; customerName: string }>(MAT_DIALOG_DATA);

  readonly channels: Channel[] = ['PHONE', 'EMAIL', 'MEETING', 'VIDEO'];
  readonly labels = CHANNEL_LABELS;
  readonly icons = CHANNEL_ICONS;
  readonly today = new Date();
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.group({
    channel: this.fb.nonNullable.control<Channel>('PHONE'),
    direction: this.fb.nonNullable.control<Direction>('OUTBOUND'),
    subject: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    notes: this.fb.control<string | null>(null),
    followUp: this.fb.nonNullable.control(false),
    followUpOn: this.fb.control<Date | null>(addDays(7)),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.loading.set(true);
    this.api.logInteraction(this.data.customerId, {
      channel: v.channel,
      direction: v.direction,
      subject: v.subject,
      notes: v.notes,
      followUpOn: v.followUp && v.followUpOn ? isoDate(v.followUpOn) : null,
    }).subscribe({
      next: (interaction) => this.dialogRef.close(interaction),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
