import { Component, Input } from '@angular/core';
import { MatBadgeModule } from '@angular/material/badge';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-sezione-avanzamento',
  standalone: true,
  imports: [MatBadgeModule, MatCardModule, MatProgressBarModule],
  templateUrl: './sezione-avanzamento.component.html',
  styleUrl: './sezione-avanzamento.component.css',
})
export class SezioneAvanzamentoComponent {
  @Input({ required: true }) percentuale = 0;
  @Input({ required: true }) taskCompletati = 0;
  @Input({ required: true }) taskTotali = 0;
  @Input({ required: true }) taskInErrore = 0;
}
