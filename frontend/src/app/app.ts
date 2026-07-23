import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabGroup, MatTab, MatTabChangeEvent } from '@angular/material/tabs';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { WorkflowService } from './dati-runtime-workflow.service';
import { etichettaStato } from './etichette';
import { ConfigDialogComponent } from './components/config-dialog-component/config-dialog.component';
import { SezioneCatalogoComponent } from './components/sezione-catalogo-component/sezione-catalogo.component';
import { SezioneEsecuzioneComponent } from './components/sezione-esecuzione-component/sezione-esecuzione.component';
import { SezioneStoricoComponent } from './components/sezione-storico-component/sezione-storico.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTooltipModule,
    MatDialogModule,
    MatTabGroup,
    MatTab,
    SezioneCatalogoComponent,
    SezioneEsecuzioneComponent,
    SezioneStoricoComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly servizioWorkflow = inject(WorkflowService);
  private readonly dialog = inject(MatDialog);

  protected readonly statoEngine = this.servizioWorkflow.statoEngine;
  protected readonly etichettaStato = etichettaStato;

  ngOnInit(): void {
    this.servizioWorkflow.caricaCatalogo();
    this.servizioWorkflow.ripristinaSeAttivo();
  }

  protected apriConfigurazione(): void {
    this.dialog.open(ConfigDialogComponent);
  }

  protected onCambioTab(evento: MatTabChangeEvent): void {
    if (evento.tab.textLabel === 'Esecuzioni passate') {
      this.servizioWorkflow.caricaEsecuzioni();
    }
  }
}
