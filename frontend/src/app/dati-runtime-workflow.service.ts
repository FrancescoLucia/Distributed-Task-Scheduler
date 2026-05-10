import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { BASE_URL, INTERVALLO_POLLING_MS } from './costanti';

export type EStatoWorkflow = 'IN_PAUSA' | 'IN_ESECUZIONE' | 'COMPLETATO' | 'FALLITO' | 'ANNULLATO';
export type EStatoTask = 'IN_ATTESA' | 'PRONTO' | 'IN_ESECUZIONE' | 'COMPLETATO' | 'FALLITO' | 'ANNULLATO';
export type StatoWorker = 'libero' | 'occupato' | 'inPausa' | 'offline';

export interface NodoWorker {
  id: string;
  nome: string;
  stato: StatoWorker;
  taskCorrente?: string;
}

export interface EngineStatus {
  stato: EStatoWorkflow | null;
  workflowInCorsoId: number | null;
  workflowInCorsoNome: string | null;
  taskAttivi: number;
  taskCompletati: number;
  taskTotali: number;
}

export interface WorkflowSummary {
  id: number;
  nome: string;
  stato: EStatoWorkflow;
  dataCreazione: string;
  taskTotali: number;
  taskCompletati: number;
}

export interface NodoGrafo {
  id: number;
  nome: string;
  stato: EStatoTask;
  tentativi: number;
}

export interface ArcoDipendenza {
  sorgente: number;
  destinazione: number;
}

export interface WorkflowGraph {
  nodi: NodoGrafo[];
  dipendenze: ArcoDipendenza[];
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly http = inject(HttpClient);

  readonly statoEngine = signal<EngineStatus | null>(null);
  readonly listaWorkflow = signal<WorkflowSummary[]>([]);
  readonly grafoWorkflow = signal<WorkflowGraph | null>(null);
  readonly idWorkflowAttivo = signal<number | null>(null);

  private intervalloPolling: ReturnType<typeof setInterval> | null = null;

  caricaListaWorkflow(): void {
    this.http.get<WorkflowSummary[]>(`${BASE_URL}/workflow`).subscribe(lista =>
      this.listaWorkflow.set(lista)
    );
  }

  caricaStatoEngine(): void {
    this.http.get<EngineStatus>(`${BASE_URL}/engine/status`).subscribe(stato =>
      this.statoEngine.set(stato)
    );
  }

  private caricaGrafo(id: number): void {
    this.http.get<WorkflowGraph>(`${BASE_URL}/workflow/${id}/graph`).subscribe(grafo =>
      this.grafoWorkflow.set(grafo)
    );
  }

  importaWorkflow(): Observable<number> {
    return this.http.post<number>(`${BASE_URL}/workflow/importa`, null).pipe(
      tap(() => this.caricaListaWorkflow())
    );
  }

  avviaWorkflow(id: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/workflow/${id}/avvia`, null).pipe(
      tap(() => this.avviaPolling(id))
    );
  }

  avviaPolling(idWorkflow: number): void {
    this.idWorkflowAttivo.set(idWorkflow);
    this.fermaPolling();
    this.caricaStatoEngine();
    this.caricaGrafo(idWorkflow);
    this.intervalloPolling = setInterval(() => {
      this.caricaStatoEngine();
      this.caricaGrafo(idWorkflow);
      const stato = this.statoEngine()?.stato;
      if (stato === 'COMPLETATO' || stato === 'FALLITO' || stato === 'ANNULLATO') {
        this.fermaPolling();
        this.caricaListaWorkflow();
      }
    }, INTERVALLO_POLLING_MS);
  }

  fermaPolling(): void {
    if (this.intervalloPolling !== null) {
      clearInterval(this.intervalloPolling);
      this.intervalloPolling = null;
    }
  }
}
