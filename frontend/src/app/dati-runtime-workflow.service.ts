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

export interface ConfigurazioneEngine {
  maxTentativi: number;
  intervallo: number;
}

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly http = inject(HttpClient);
  private readonly STORAGE_KEY = 'workflowAttivoId';

  readonly statoEngine = signal<EngineStatus | null>(null);
  readonly listaWorkflow = signal<WorkflowSummary[]>([]);
  readonly grafoWorkflow = signal<WorkflowGraph | null>(null);
  readonly idWorkflowAttivo = signal<number | null>(null);
  readonly configurazioneEngine = signal<ConfigurazioneEngine | null>(null);

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

  ripristinaSeAttivo(): void {
    this.http.get<EngineStatus>(`${BASE_URL}/engine/status`).subscribe(stato => {
      this.statoEngine.set(stato);
      const idSalvato = localStorage.getItem(this.STORAGE_KEY);
      if (idSalvato !== null) {
        if (stato.stato === 'IN_ESECUZIONE' || stato.stato === 'IN_PAUSA') {
          this.avviaPolling(stato.workflowInCorsoId ?? Number(idSalvato));
        } else {
          localStorage.removeItem(this.STORAGE_KEY);
        }
      }
    });
  }

  private caricaGrafo(id: number): void {
    this.http.get<WorkflowGraph>(`${BASE_URL}/workflow/${id}/graph`).subscribe(grafo =>
      this.grafoWorkflow.set(grafo)
    );
  }

  caricaConfigurazione(): void {
    this.getConfigurazione().subscribe();
  }

  getConfigurazione(): Observable<ConfigurazioneEngine> {
    return this.http.get<ConfigurazioneEngine>(`${BASE_URL}/engine/configurazione`).pipe(
      tap(c => this.configurazioneEngine.set(c))
    );
  }

  aggiornaConfigurazione(config: ConfigurazioneEngine): Observable<void> {
    return this.http.put<void>(`${BASE_URL}/engine/configurazione`, config).pipe(
      tap(() => this.configurazioneEngine.set(config))
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

  mettiInPausa(id: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/workflow/${id}/pausa`, null);
  }

  riprendi(id: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/workflow/${id}/riprendi`, null);
  }

  annulla(id: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/workflow/${id}/annulla`, null);
  }

  avviaPolling(idWorkflow: number): void {
    this.idWorkflowAttivo.set(idWorkflow);
    localStorage.setItem(this.STORAGE_KEY, String(idWorkflow));
    this.fermaPolling();
    this.caricaStatoEngine();
    this.caricaGrafo(idWorkflow);
    this.intervalloPolling = setInterval(() => {
      this.caricaGrafo(idWorkflow);
      this.http.get<EngineStatus>(`${BASE_URL}/engine/status`).subscribe(stato => {
        this.statoEngine.set(stato);
        if (stato.stato === 'COMPLETATO' || stato.stato === 'FALLITO' || stato.stato === 'ANNULLATO') {
          this.fermaPolling();
          localStorage.removeItem(this.STORAGE_KEY);
          this.caricaListaWorkflow();
        }
      });
    }, INTERVALLO_POLLING_MS);
  }

  fermaPolling(): void {
    if (this.intervalloPolling !== null) {
      clearInterval(this.intervalloPolling);
      this.intervalloPolling = null;
    }
  }
}
