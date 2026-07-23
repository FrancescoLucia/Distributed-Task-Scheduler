import { EStatoTask, EStatoWorkflow } from './dati-runtime-workflow.service';

const ETICHETTE_STATO: Record<string, string> = {
  IN_PAUSA: 'In pausa',
  IN_ESECUZIONE: 'In esecuzione',
  COMPLETATO: 'Completato',
  FALLITO: 'Fallito',
  ANNULLATO: 'Annullato',
  IN_ATTESA: 'In attesa',
  PRONTO: 'Pronto',
  INATTIVO: 'Inattivo',
};

export function etichettaStato(stato: EStatoWorkflow | EStatoTask | string | null): string {
  return stato ? (ETICHETTE_STATO[stato] ?? stato) : '';
}
