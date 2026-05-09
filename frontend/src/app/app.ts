import { Component, OnInit, computed, inject } from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkflowService, EStatoWorkflow, EStatoTask } from './dati-runtime-workflow.service';
import { SezioneAvanzamentoComponent } from './components/sezione-avanzamento-component/sezione-avanzamento.component';
import { SezioneGrafoComponent } from './components/sezione-grafo-component/sezione-grafo.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    MatBadgeModule,
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    MatToolbarModule,
    MatTooltipModule,
    SezioneGrafoComponent,
    SezioneAvanzamentoComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly servizioWorkflow = inject(WorkflowService);

  protected readonly statoEngine = this.servizioWorkflow.statoEngine;
  protected readonly listaWorkflow = this.servizioWorkflow.listaWorkflow;
  protected readonly grafoWorkflow = this.servizioWorkflow.grafoWorkflow;

  protected readonly avanzamentoPercentuale = computed(() => {
    const engine = this.statoEngine();
    if (!engine || engine.taskTotali === 0) return 0;
    return Math.round((engine.taskCompletati / engine.taskTotali) * 100);
  });

  protected readonly taskInErrore = computed(() => {
    const grafo = this.grafoWorkflow();
    if (!grafo) return 0;
    return grafo.nodi.filter(n => n.stato === 'FALLITO').length;
  });

  ngOnInit(): void {
    this.servizioWorkflow.caricaListaWorkflow();
    this.servizioWorkflow.caricaStatoEngine();
  }

  protected importaWorkflow(): void {
    this.servizioWorkflow.importaWorkflow().subscribe();
  }

  protected avviaWorkflow(id: number): void {
    this.servizioWorkflow.avviaWorkflow(id).subscribe();
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
