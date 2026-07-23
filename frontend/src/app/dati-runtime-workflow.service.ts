import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { BASE_URL, INTERVALLO_POLLING_MS } from './costanti';

export type EStatoWorkflow = 'IN_PAUSA' | 'IN_ESECUZIONE' | 'COMPLETATO' | 'FALLITO' | 'ANNULLATO' | 'INATTIVO';
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
  esecuzioneInCorsoId: number | null;
  workflowInCorsoId: number | null;
  workflowInCorsoNome: string | null;
  taskAttivi: number;
  taskCompletati: number;
  taskTotali: number;
}

export interface WorkflowCatalogo {
  id: number;
  nome: string;
  dataCreazione: string;
  numeroTask: number;
}

export interface EsecuzioneSummary {
  id: number;
  workflowId: number;
  nome: string;
  dataInizio: string;
  dataFine: string | null;
  stato: EStatoWorkflow;
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
  private readonly STORAGE_KEY = 'esecuzioneAttivaId';

  readonly statoEngine = signal<EngineStatus | null>(null);
  readonly catalogo = signal<WorkflowCatalogo[]>([]);
  readonly listaEsecuzioni = signal<EsecuzioneSummary[]>([]);
  readonly grafoWorkflow = signal<WorkflowGraph | null>(null);
  readonly grafoStorico = signal<WorkflowGraph | null>(null);
  readonly esecuzioneAttivaId = signal<number | null>(null);
  readonly configurazioneEngine = signal<ConfigurazioneEngine | null>(null);

  private intervalloPolling: ReturnType<typeof setInterval> | null = null;

  caricaCatalogo(nome?: string): void {
    const url = nome ? `${BASE_URL}/workflow?nome=${encodeURIComponent(nome)}` : `${BASE_URL}/workflow`;
    this.http.get<WorkflowCatalogo[]>(url).subscribe(lista => this.catalogo.set(lista));
  }

  eliminaWorkflow(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/workflow/${id}`).pipe(
      tap(() => this.caricaCatalogo())
    );
  }

  importaWorkflow(): Observable<number> {
    return this.http.post<number>(`${BASE_URL}/workflow/importa`, null).pipe(
      tap(() => this.caricaCatalogo())
    );
  }

  avviaWorkflow(id: number): Observable<{ esecuzioneId: number }> {
    return this.http.post<{ esecuzioneId: number }>(`${BASE_URL}/workflow/${id}/avvia`, null).pipe(
      tap(risposta => this.avviaPolling(risposta.esecuzioneId))
    );
  }

  caricaEsecuzioni(workflowId?: number): void {
    const url = workflowId !== undefined
      ? `${BASE_URL}/esecuzioni?workflowId=${workflowId}`
      : `${BASE_URL}/esecuzioni`;
    this.http.get<EsecuzioneSummary[]>(url).subscribe(lista => this.listaEsecuzioni.set(lista));
  }

  caricaGrafoEsecuzione(esecuzioneId: number): void {
    this.http.get<WorkflowGraph>(`${BASE_URL}/esecuzioni/${esecuzioneId}/graph`).subscribe(grafo =>
      this.grafoStorico.set(grafo)
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
          this.avviaPolling(stato.esecuzioneInCorsoId ?? Number(idSalvato));
        } else {
          localStorage.removeItem(this.STORAGE_KEY);
        }
      }
    });
  }

  private caricaGrafo(esecuzioneId: number): void {
    this.http.get<WorkflowGraph>(`${BASE_URL}/esecuzioni/${esecuzioneId}/graph`).subscribe(grafo =>
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

  mettiInPausa(esecuzioneId: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/esecuzioni/${esecuzioneId}/pausa`, null);
  }

  riprendi(esecuzioneId: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/esecuzioni/${esecuzioneId}/riprendi`, null);
  }

  annulla(esecuzioneId: number): Observable<void> {
    return this.http.post<void>(`${BASE_URL}/esecuzioni/${esecuzioneId}/annulla`, null);
  }

  avviaPolling(esecuzioneId: number): void {
    this.esecuzioneAttivaId.set(esecuzioneId);
    localStorage.setItem(this.STORAGE_KEY, String(esecuzioneId));
    this.fermaPolling();
    this.caricaStatoEngine();
    this.caricaGrafo(esecuzioneId);
    this.intervalloPolling = setInterval(() => {
      this.caricaGrafo(esecuzioneId);
      this.http.get<EngineStatus>(`${BASE_URL}/engine/status`).subscribe(stato => {
        this.statoEngine.set(stato);
        if (stato.stato === 'COMPLETATO' || stato.stato === 'FALLITO' || stato.stato === 'ANNULLATO') {
          this.fermaPolling();
          localStorage.removeItem(this.STORAGE_KEY);
          this.caricaCatalogo();
          this.caricaEsecuzioni();
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
