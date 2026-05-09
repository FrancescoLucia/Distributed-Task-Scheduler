import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';

import { NodoWorker } from '../../dati-runtime-workflow.service';

@Component({
  selector: 'app-sezione-worker',
  standalone: true,
  imports: [MatCardModule, MatChipsModule],
  templateUrl: './sezione-worker.component.html',
  styleUrl: './sezione-worker.component.css',
})
export class SezioneWorkerComponent {
  @Input({ required: true }) worker: NodoWorker[] = [];
  @Input({ required: true }) workerLiberi = 0;
  @Input({ required: true }) workerOccupati = 0;

  protected etichettaStatoWorker(stato: NodoWorker['stato']): string {
    const etichette: Record<NodoWorker['stato'], string> = {
      libero: 'Libero',
      occupato: 'Occupato',
      inPausa: 'In pausa',
      offline: 'Offline',
    };
    return etichette[stato];
  }
}
