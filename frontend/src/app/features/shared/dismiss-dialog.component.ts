import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

/** Motif obligatoire pour écarter une recommandation (elle reviendra dans 90 jours). */
@Component({
  selector: 'app-dismiss-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatChipsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Écarter la recommandation</h2>
    <mat-dialog-content>
      <p class="muted">« {{ data.title }} » ne sera plus proposée pendant 90 jours. Le motif est conservé dans l'historique.</p>
      <mat-chip-listbox aria-label="Motifs fréquents" (change)="reason = $event.value ?? reason">
        @for (r of presets; track r) {
          <mat-chip-option [value]="r">{{ r }}</mat-chip-option>
        }
      </mat-chip-listbox>
      <mat-form-field appearance="outline" class="reason">
        <mat-label>Motif</mat-label>
        <textarea matInput [(ngModel)]="reason" rows="2" maxlength="300" data-testid="dismiss-reason"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="!reason.trim()" (click)="dialogRef.close(reason.trim())"
              data-testid="dismiss-submit">Écarter</button>
    </mat-dialog-actions>
  `,
  styles: `.reason { margin-top: 16px; }`,
})
export class DismissDialogComponent {
  readonly dialogRef = inject(MatDialogRef<DismissDialogComponent, string>);
  readonly data = inject<{ title: string }>(MAT_DIALOG_DATA);
  readonly presets = ['Déjà traité avec le client', 'Client non intéressé', 'Équipé ailleurs', 'Pas le bon moment'];
  reason = '';
}
