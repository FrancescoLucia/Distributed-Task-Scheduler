import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';

import { WorkflowService } from '../../dati-runtime-workflow.service';
import { etichettaStato } from '../../etichette';

@Component({
  selector: 'app-sezione-catalogo',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatDividerModule,
    MatIconModule,
    MatListModule,
    MatTooltipModule,
  ],
  templateUrl: './sezione-catalogo.component.html',
  styleUrl: './sezione-catalogo.component.css',
})
export class SezioneCatalogoComponent {
  private readonly servizioWorkflow = inject(WorkflowService);

  protected readonly listaWorkflow = this.servizioWorkflow.listaWorkflow;
  protected readonly etichettaStato = etichettaStato;

  protected importaWorkflow(): void {
    this.servizioWorkflow.importaWorkflow().subscribe();
  }

  protected avviaWorkflow(id: number): void {
    this.servizioWorkflow.avviaWorkflow(id).subscribe();
  }
}
