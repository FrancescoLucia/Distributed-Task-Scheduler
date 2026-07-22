import { Component, OnInit, computed, inject } from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule, MatTabGroup, MatTab } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { WorkflowService, EStatoWorkflow, EStatoTask } from './dati-runtime-workflow.service';
import { ConfigDialogComponent } from './components/config-dialog-component/config-dialog.component';
import { SezioneAvanzamentoComponent } from './components/sezione-avanzamento-component/sezione-avanzamento.component';
import { SezioneGrafoComponent } from './components/sezione-grafo-component/sezione-grafo.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    MatBadgeModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatExpansionModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatTooltipModule,
    MatDialogModule,
    SezioneGrafoComponent,
    SezioneAvanzamentoComponent,
    MatTabGroup,
    MatTab
],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly servizioWorkflow = inject(WorkflowService);
  private readonly dialog = inject(MatDialog);

  protected readonly statoEngine = this.servizioWorkflow.statoEngine;
  protected readonly listaWorkflow = this.servizioWorkflow.listaWorkflow;
  protected readonly grafoWorkflow = this.servizioWorkflow.grafoWorkflow;

  protected readonly taskCompletati = computed(() =>
    this.grafoWorkflow()?.nodi.filter(n => n.stato === 'COMPLETATO').length ?? 0
  );

  protected readonly taskTotali = computed(() =>
    this.grafoWorkflow()?.nodi.length ?? 0
  );

  protected readonly avanzamentoPercentuale = computed(() => {
    const totali = this.taskTotali();
    if (totali === 0) return 0;
    return Math.round((this.taskCompletati() / totali) * 100);
  });

  protected readonly taskInErrore = computed(() =>
    this.grafoWorkflow()?.nodi.filter(n => n.tentativi > 0).length ?? 0
  );

  ngOnInit(): void {
    this.servizioWorkflow.caricaListaWorkflow();
    this.servizioWorkflow.ripristinaSeAttivo();
  }

  protected apriConfigurazione(): void {
    this.dialog.open(ConfigDialogComponent);
  }

  protected importaWorkflow(): void {
    this.servizioWorkflow.importaWorkflow().subscribe();
  }

  protected avviaWorkflow(id: number): void {
    this.servizioWorkflow.avviaWorkflow(id).subscribe();
  }

  protected mettiInPausa(): void {
    const id = this.servizioWorkflow.idWorkflowAttivo();
    if (id !== null) this.servizioWorkflow.mettiInPausa(id).subscribe();
  }

  protected riprendi(): void {
    const id = this.servizioWorkflow.idWorkflowAttivo();
    if (id !== null) this.servizioWorkflow.riprendi(id).subscribe();
  }

  protected annullaWorkflow(): void {
    const id = this.servizioWorkflow.idWorkflowAttivo();
    if (id !== null) this.servizioWorkflow.annulla(id).subscribe();
  }

  protected etichettaStato(stato: EStatoWorkflow | EStatoTask | string | null): string {
    const etichette: Record<string, string> = {
      IN_PAUSA: 'In pausa',
      IN_ESECUZIONE: 'In esecuzione',
      COMPLETATO: 'Completato',
      FALLITO: 'Fallito',
      ANNULLATO: 'Annullato',
      IN_ATTESA: 'In attesa',
      PRONTO: 'Pronto',
    };
    return stato ? (etichette[stato] ?? stato) : '';
  }
}
