import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkflowService, WorkflowCatalogo } from '../../dati-runtime-workflow.service';
import { ConfermaDialogComponent, DatiConferma } from '../conferma-dialog-component/conferma-dialog.component';
import { RinominaDialogComponent } from '../rinomina-dialog-component/rinomina-dialog.component';

@Component({
  selector: 'app-sezione-catalogo',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatTooltipModule,
  ],
  templateUrl: './sezione-catalogo.component.html',
  styleUrl: './sezione-catalogo.component.css',
})
export class SezioneCatalogoComponent implements OnInit {
  private readonly servizioWorkflow = inject(WorkflowService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  protected readonly catalogo = this.servizioWorkflow.catalogo;
  protected readonly ricerca = new FormControl('', { nonNullable: true });
  protected readonly erroriImport = signal<string[]>([]);
  protected readonly urlTemplate = this.servizioWorkflow.urlTemplate();

  protected readonly engineOccupato = computed(() => {
    const stato = this.servizioWorkflow.statoEngine()?.stato;
    return stato === 'IN_ESECUZIONE' || stato === 'IN_PAUSA';
  });

  protected inEsecuzione(id: number): boolean {
    return this.engineOccupato() && this.servizioWorkflow.statoEngine()?.workflowInCorsoId === id;
  }

  ngOnInit(): void {
    this.ricerca.valueChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe(nome => {
      this.servizioWorkflow.caricaCatalogo(nome.trim() || undefined);
    });
  }

  protected importaDemo(): void {
    this.servizioWorkflow.importaDemo().subscribe({
      next: () => this.snackBar.open('Workflow demo importato', undefined, { duration: 3000 }),
      error: err => this.mostraErrore(err),
    });
  }

  protected async importaDaFile(evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const contenuto = await file.text();
    input.value = '';
    this.erroriImport.set([]);
    this.servizioWorkflow.importaDaFile(contenuto).subscribe({
      next: () => this.snackBar.open('Workflow importato', undefined, { duration: 3000 }),
      error: err => this.gestisciErroreImport(err),
    });
  }

  protected chiudiErrori(): void {
    this.erroriImport.set([]);
  }

  protected rinominaWorkflow(workflow: WorkflowCatalogo): void {
    this.dialog.open(RinominaDialogComponent, { data: { nome: workflow.nome } })
      .afterClosed().subscribe((nuovoNome: string | undefined) => {
        if (!nuovoNome || nuovoNome === workflow.nome) return;
        this.servizioWorkflow.rinominaWorkflow(workflow.id, nuovoNome).subscribe({
          next: () => this.snackBar.open('Workflow rinominato', undefined, { duration: 3000 }),
          error: err => this.mostraErrore(err),
        });
      });
  }

  protected avviaWorkflow(workflow: WorkflowCatalogo): void {
    this.conferma({
      titolo: 'Avvia workflow',
      messaggio: `Avviare una nuova esecuzione di "${workflow.nome}"?`,
      testoConferma: 'Avvia',
    }, () => {
      this.servizioWorkflow.avviaWorkflow(workflow.id).subscribe({
        next: () => this.snackBar.open(`Esecuzione di "${workflow.nome}" avviata`, undefined, { duration: 3000 }),
        error: err => this.mostraErrore(err),
      });
    });
  }

  protected eliminaWorkflow(workflow: WorkflowCatalogo): void {
    this.conferma({
      titolo: 'Elimina workflow',
      messaggio: `Eliminare "${workflow.nome}" dal catalogo? Verranno rimosse anche tutte le sue esecuzioni.`,
      testoConferma: 'Elimina',
      pericolo: true,
    }, () => {
      this.servizioWorkflow.eliminaWorkflow(workflow.id).subscribe({
        next: () => this.snackBar.open(`"${workflow.nome}" eliminato dal catalogo`, undefined, { duration: 3000 }),
        error: err => this.mostraErrore(err),
      });
    });
  }

  private conferma(dati: DatiConferma, azione: () => void): void {
    this.dialog.open(ConfermaDialogComponent, { data: dati }).afterClosed().subscribe(confermato => {
      if (confermato) azione();
    });
  }

  private gestisciErroreImport(err: HttpErrorResponse): void {
    const errori = err.error?.errori;
    if (Array.isArray(errori) && errori.length > 0) {
      this.erroriImport.set(errori);
    } else {
      this.mostraErrore(err);
    }
  }

  private mostraErrore(err: HttpErrorResponse): void {
    const messaggio = typeof err.error === 'string' && err.error ? err.error : 'Operazione non riuscita';
    this.snackBar.open(messaggio, undefined, { duration: 4000 });
  }
}
