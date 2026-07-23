import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkflowService } from '../../dati-runtime-workflow.service';
import { etichettaStato } from '../../etichette';
import { SezioneGrafoComponent } from '../sezione-grafo-component/sezione-grafo.component';

@Component({
  selector: 'app-sezione-esecuzione',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    SezioneGrafoComponent,
  ],
  templateUrl: './sezione-esecuzione.component.html',
  styleUrl: './sezione-esecuzione.component.css',
})
export class SezioneEsecuzioneComponent {
  private readonly servizioWorkflow = inject(WorkflowService);

  protected readonly statoEngine = this.servizioWorkflow.statoEngine;
  protected readonly grafoWorkflow = this.servizioWorkflow.grafoWorkflow;
  protected readonly esecuzioneCorrente = this.servizioWorkflow.esecuzioneCorrente;
  protected readonly etichettaStato = etichettaStato;

  protected readonly terminata = computed(() => {
    const stato = this.esecuzioneCorrente()?.stato;
    return stato === 'FALLITO' || stato === 'ANNULLATO' || stato === 'COMPLETATO';
  });

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

  protected mettiInPausa(): void {
    const id = this.servizioWorkflow.esecuzioneAttivaId();
    if (id !== null) this.servizioWorkflow.mettiInPausa(id).subscribe();
  }

  protected riprendi(): void {
    const id = this.servizioWorkflow.esecuzioneAttivaId();
    if (id !== null) this.servizioWorkflow.riprendi(id).subscribe();
  }

  protected annullaWorkflow(): void {
    const id = this.servizioWorkflow.esecuzioneAttivaId();
    if (id !== null) this.servizioWorkflow.annulla(id).subscribe();
  }
}
