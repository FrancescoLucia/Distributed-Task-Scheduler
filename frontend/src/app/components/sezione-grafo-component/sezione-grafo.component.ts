import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

import { WorkflowGraph } from '../../dati-runtime-workflow.service';
import { GrafoTaskComponent } from '../grafo-task-component/grafo-task.component';

@Component({
  selector: 'app-sezione-grafo',
  standalone: true,
  imports: [MatCardModule, GrafoTaskComponent],
  templateUrl: './sezione-grafo.component.html',
  styleUrl: './sezione-grafo.component.css',
})
export class SezioneGrafoComponent {
  @Input({ required: true }) grafo!: WorkflowGraph;
}
