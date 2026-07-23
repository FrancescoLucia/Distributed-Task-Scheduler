import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { ALGORITMI_SCHEDULAZIONE, ALGORITMO_DEFAULT, ALGORITMO_LABELS, AlgoritmoSchedulazione } from '../../types/algoritmo-schedulazione';

export interface DatiAvvia {
  nomeWorkflow: string;
}

@Component({
  selector: 'app-avvia-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './avvia-dialog.component.html',
  styles: `.campo-algoritmo { width: 320px; max-width: 100%; }`,
})
export class AvviaDialogComponent {
  protected readonly dati = inject<DatiAvvia>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<AvviaDialogComponent>);

  protected readonly algoritmi = ALGORITMI_SCHEDULAZIONE;
  protected readonly etichette = ALGORITMO_LABELS;
  protected readonly algoritmo = new FormControl<AlgoritmoSchedulazione>(ALGORITMO_DEFAULT, { nonNullable: true });

  protected annulla(): void {
    this.dialogRef.close();
  }

  protected conferma(): void {
    this.dialogRef.close(this.algoritmo.value);
  }
}
