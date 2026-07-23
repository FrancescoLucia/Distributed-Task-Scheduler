import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { BASE_URL, INTERVALLO_POLLING_MS } from './costanti';
import { AlgoritmoSchedulazione } from './types/algoritmo-schedulazione';

export type EStatoWorkflow = 'IN_PAUSA' | 'IN_ESECUZIONE' | 'COMPLETATO' | 'FALLITO' | 'ANNULLATO' | 'INATTIVO';
export type EStatoTask = 'IN_ATTESA' | 'PRONTO' | 'IN_ESECUZIONE' | 'COMPLETATO' | 'FALLITO' | 'ANNULLATO';
export type StatoWorker = 'libero' | 'occupato' | 'inPausa' | 'offline';
export type ETipoTask = 'SCRIPT' | 'MATH' | 'FILE' | 'HTTP';

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
  algoritmo: AlgoritmoSchedulazione;
  taskTotali: number;
  taskCompletati: number;
}

export interface NodoGrafo {
  id: number;
  nome: string;
  tipo: ETipoTask;
  stato: EStatoTask | null;
  tentativi: number;
  errore?: string;
}

export interface ArcoDipendenza {
  sorgente: number;
  destinazione: number;
}

export interface WorkflowGraph {
  nodi: NodoGrafo[];
  dipendenze: ArcoDipendenza[];
}

export interface EsecuzioneRuntime {
  engineStatus: EngineStatus;
  esecuzione: EsecuzioneSummary;
  graph: WorkflowGraph;
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
  readonly esecuzioneCorrente = signal<EsecuzioneSummary | null>(null);
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

  importaDemo(): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${BASE_URL}/workflow/importa/demo`, null).pipe(
      tap(() => this.caricaCatalogo())
    );
  }

  importaDaFile(contenuto: string): Observable<{ id: number }> {
    return this.http.post<{ id: number }>(`${BASE_URL}/workflow/importa`, contenuto, {
      headers: { 'Content-Type': 'application/json' },
    }).pipe(
      tap(() => this.caricaCatalogo())
    );
  }

  rinominaWorkflow(id: number, nome: string): Observable<void> {
    return this.http.put<void>(`${BASE_URL}/workflow/${id}/nome`, { nome }).pipe(
      tap(() => this.caricaCatalogo())
    );
  }

  urlTemplate(): string {
    return `${BASE_URL}/workflow/template`;
  }

  avviaWorkflow(id: number, algoritmo: AlgoritmoSchedulazione): Observable<{ esecuzioneId: number }> {
    return this.http.post<{ esecuzioneId: number }>(`${BASE_URL}/workflow/${id}/avvia?algoritmo=${algoritmo}`, null).pipe(
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
          this.caricaRuntime(Number(idSalvato));
        }
      }
    });
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
    this.esecuzioneCorrente.set(null);
    localStorage.setItem(this.STORAGE_KEY, String(esecuzioneId));
    this.fermaPolling();
    this.caricaRuntime(esecuzioneId);
    this.intervalloPolling = setInterval(() => {
      this.caricaRuntime(esecuzioneId);
    }, INTERVALLO_POLLING_MS);
  }

  private caricaRuntime(esecuzioneId: number): void {
    this.http.get<EsecuzioneRuntime>(`${BASE_URL}/esecuzioni/${esecuzioneId}/runtime`).subscribe(runtime => {
      this.statoEngine.set(runtime.engineStatus);
      this.grafoWorkflow.set(runtime.graph);
      this.esecuzioneCorrente.set(runtime.esecuzione);
      const esecuzione = runtime.esecuzione;
      if (esecuzione.stato === 'COMPLETATO' || esecuzione.stato === 'FALLITO' || esecuzione.stato === 'ANNULLATO') {
        this.fermaPolling();
        localStorage.removeItem(this.STORAGE_KEY);
        this.caricaCatalogo();
        this.caricaEsecuzioni();
      }
    });
  }

  fermaPolling(): void {
    if (this.intervalloPolling !== null) {
      clearInterval(this.intervalloPolling);
      this.intervalloPolling = null;
    }
  }
}
