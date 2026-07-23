import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

export interface DatiConferma {
  titolo: string;
  messaggio: string;
  testoConferma: string;
  pericolo?: boolean;
}

@Component({
  selector: 'app-conferma-dialog',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule],
  templateUrl: './conferma-dialog.component.html',
})
export class ConfermaDialogComponent {
  protected readonly dati = inject<DatiConferma>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ConfermaDialogComponent>);

  protected annulla(): void {
    this.dialogRef.close(false);
  }

  protected conferma(): void {
    this.dialogRef.close(true);
  }
}
