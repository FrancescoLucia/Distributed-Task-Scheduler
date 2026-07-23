import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface DatiRinomina {
  nome: string;
}

@Component({
  selector: 'app-rinomina-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './rinomina-dialog.component.html',
  styles: `
    .campo-nome {
      width: 360px;
      max-width: 100%;
      margin-top: 16px;
    }
  `,
})
export class RinominaDialogComponent {
  private readonly dati = inject<DatiRinomina>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RinominaDialogComponent>);

  protected readonly nome = new FormControl(this.dati.nome, {
    nonNullable: true,
    validators: [Validators.required, this.nonVuoto],
  });

  private nonVuoto(control: { value: string }): { vuoto: true } | null {
    return control.value.trim().length === 0 ? { vuoto: true } : null;
  }

  protected annulla(): void {
    this.dialogRef.close();
  }

  protected salva(): void {
    if (this.nome.invalid) return;
    this.dialogRef.close(this.nome.value.trim());
  }
}
