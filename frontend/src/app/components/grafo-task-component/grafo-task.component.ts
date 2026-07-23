import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import cytoscape, { Core, ElementDefinition } from 'cytoscape';
import dagre from 'cytoscape-dagre';

import { ArcoDipendenza, ETipoTask, NodoGrafo } from '../../dati-runtime-workflow.service';

cytoscape.use(dagre);

const ICONE_TIPO: Record<ETipoTask, string> = {
  MATH: '∑',
  FILE: '📁',
  HTTP: '🌐',
  SCRIPT: '›_',
};

interface ElementoNodoGrafo {
  id: string;
  label: string;
  stato: string;
}

interface ElementoArcoGrafo {
  source: string;
  target: string;
}

@Component({
  selector: 'app-grafo-task',
  standalone: true,
  templateUrl: './grafo-task.component.html',
  styleUrl: './grafo-task.component.css',
})
export class GrafoTaskComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) nodi: NodoGrafo[] = [];
  @Input({ required: true }) dipendenze: ArcoDipendenza[] = [];

  @ViewChild('contenitoreGrafo', { static: true })
  private readonly contenitoreGrafo!: ElementRef<HTMLDivElement>;

  private cy?: Core;
  private resizeObserver?: ResizeObserver;

  ngAfterViewInit(): void {
    this.cy = cytoscape({
      container: this.contenitoreGrafo.nativeElement,
      elements: this.elementiGrafo(),
      layout: this.layoutDagre(),
      minZoom: 0.4,
      maxZoom: 2,
      wheelSensitivity: 0.15,
      style: [
        {
          selector: 'node',
          style: {
            shape: 'round-rectangle',
            width: 190,
            height: 96,
            'background-color': '#ffffff',
            'border-color': '#d3dde1',
            'border-width': 1.5,
            label: 'data(label)',
            color: '#172026',
            'font-size': 13,
            'font-weight': 600,
            'text-wrap': 'wrap',
            'text-max-width': '168px',
            'text-valign': 'center',
            'text-halign': 'center',
            'text-justification': 'center',
            padding: '8px',
            'text-outline-width': 0,
          },
        },
        {
          selector: 'edge',
          style: {
            width: 2,
            'line-color': '#8ca1a8',
            'target-arrow-color': '#8ca1a8',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
          },
        },
        {
          selector: 'node[stato = "IN_ESECUZIONE"]',
          style: {
            'border-color': '#006d75',
            'background-color': '#dff6f8',
          },
        },
        {
          selector: 'node[stato = "COMPLETATO"]',
          style: {
            'border-color': '#1b6e35',
            'background-color': '#e6f5ec',
          },
        },
        {
          selector: 'node[stato = "IN_ATTESA"], node[stato = "PRONTO"]',
          style: {
            'border-color': '#6a7378',
            'background-color': '#edf1f3',
          },
        },
        {
          selector: 'node[stato = "FALLITO"], node[stato = "ANNULLATO"]',
          style: {
            'border-color': '#b4232a',
            'background-color': '#f8d7da',
          },
        },
        {
          selector: 'node[stato = "IN_PAUSA"]',
          style: {
            'border-color': '#9a5b00',
            'background-color': '#fff1d6',
          },
        },
      ],
    });

    // this.resizeObserver = new ResizeObserver(() => {
    //   this.cy?.resize();
    //   this.cy?.fit(undefined, 40);
    // });
    // this.resizeObserver.observe(this.contenitoreGrafo.nativeElement);

    // this.cy.fit(undefined, 40);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.cy || (!changes['nodi'] && !changes['dipendenze'])) {
      return;
    }

    this.cy.elements().remove();
    this.cy.add(this.elementiGrafo());
    this.cy.layout(this.layoutDagre()).run();
    this.cy.fit(undefined, 40);
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
    this.cy?.destroy();
  }

  private elementiGrafo(): ElementDefinition[] {
    const nodi: ElementDefinition[] = this.nodi.map((nodo) => ({
      data: this.mappaNodo(nodo),
    }));

    const archi: ElementDefinition[] = this.dipendenze.map((dipendenza) => ({
      data: {
        ...this.mappaArco(dipendenza),
        id: `${dipendenza.sorgente}->${dipendenza.destinazione}`,
      },
    }));

    return [...nodi, ...archi];
  }

  private mappaNodo(nodo: NodoGrafo): ElementoNodoGrafo {
    const retry = nodo.tentativi > 0 ? `\n↺ ${nodo.tentativi}` : '';
    const stato = nodo.stato ? `\n<${this.etichettaStato(nodo.stato)}>` : '';
    const icona = ICONE_TIPO[nodo.tipo] ? `${ICONE_TIPO[nodo.tipo]}\n\n` : '';
    return {
      id: String(nodo.id),
      label: `${icona}${nodo.nome}${stato}${retry}`,
      stato: nodo.stato ?? '',
    };
  }

  private mappaArco(dipendenza: ArcoDipendenza): ElementoArcoGrafo {
    return {
      source: String(dipendenza.sorgente),
      target: String(dipendenza.destinazione),
    };
  }

  private etichettaStato(stato: string): string {
    const etichette: Record<string, string> = {
      IN_ATTESA: 'In attesa',
      PRONTO: 'Pronto',
      IN_ESECUZIONE: 'In esecuzione',
      COMPLETATO: 'Completato',
      FALLITO: 'Fallito',
      ANNULLATO: 'Annullato',
      IN_PAUSA: 'In pausa',
    };
    return etichette[stato] ?? stato;
  }

  private layoutDagre() {
    return {
      name: 'dagre',
      rankDir: 'LR',
      rankSep: 110,
      nodeSep: 45,
      edgeSep: 20,
      padding: 24,
      animate: false,
      fit: true,
    } as const;
  }
}
