import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkflowService } from '../../dati-runtime-workflow.service';

@Component({
  selector: 'app-config-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTooltipModule,
  ],
  templateUrl: './config-dialog.component.html',
  styleUrl: './config-dialog.component.css',
})
export class ConfigDialogComponent implements OnInit {
  private readonly servizioWorkflow = inject(WorkflowService);
  private readonly dialogRef = inject(MatDialogRef<ConfigDialogComponent>);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly modificaAbilitata = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    maxTentativi: [0, [Validators.required, Validators.min(0)]],
    intervallo: [0, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.form.disable();
    this.servizioWorkflow.getConfigurazione().subscribe(config => {
      this.form.patchValue(config);
    });
  }

  protected abilitaModifica(): void {
    this.modificaAbilitata.set(true);
    this.form.enable();
  }

  protected salva(): void {
    if (this.form.invalid) return;
    this.servizioWorkflow.aggiornaConfigurazione(this.form.getRawValue()).subscribe(() => {
      this.servizioWorkflow.caricaStatoEngine();
      this.snackBar.open('Configurazione salvata', undefined, { duration: 3000 });
      this.dialogRef.close(true);
    });
  }

  protected annulla(): void {
    this.dialogRef.close();
  }
}
