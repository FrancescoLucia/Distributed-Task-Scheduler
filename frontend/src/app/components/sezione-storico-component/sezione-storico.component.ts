import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

import { WorkflowService, EsecuzioneSummary } from '../../dati-runtime-workflow.service';
import { etichettaStato } from '../../etichette';
import { ALGORITMO_LABELS, AlgoritmoSchedulazione } from '../../types/algoritmo-schedulazione';
import { SezioneGrafoComponent } from '../sezione-grafo-component/sezione-grafo.component';

@Component({
  selector: 'app-sezione-storico',
  standalone: true,
  imports: [
    DatePipe,
    MatCardModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    SezioneGrafoComponent,
  ],
  templateUrl: './sezione-storico.component.html',
  styleUrl: './sezione-storico.component.css',
})
export class SezioneStoricoComponent {
  private readonly servizioWorkflow = inject(WorkflowService);

  protected readonly listaEsecuzioni = this.servizioWorkflow.listaEsecuzioni;
  protected readonly grafoStorico = this.servizioWorkflow.grafoStorico;
  protected readonly esecuzioneSelezionata = signal<EsecuzioneSummary | null>(null);
  protected readonly etichettaStato = etichettaStato;

  protected etichettaAlgoritmo(algoritmo: AlgoritmoSchedulazione): string {
    return ALGORITMO_LABELS[algoritmo] ?? algoritmo;
  }

  protected selezionaEsecuzione(esecuzione: EsecuzioneSummary): void {
    this.esecuzioneSelezionata.set(esecuzione);
    this.servizioWorkflow.caricaGrafoEsecuzione(esecuzione.id);
  }
}
