export const ALGORITMI_SCHEDULAZIONE = [
  'FIGLI_DESC',
  'FIGLI_ASC',
  'DIPENDENZE_DESC',
  'DIPENDENZE_ASC',
  'ALFABETICO',
  'CASUALE',
] as const;

export type AlgoritmoSchedulazione = typeof ALGORITMI_SCHEDULAZIONE[number];

export const ALGORITMO_DEFAULT: AlgoritmoSchedulazione = 'FIGLI_DESC';

export const ALGORITMO_LABELS: Record<AlgoritmoSchedulazione, string> = {
  FIGLI_DESC: 'Numero Figli decrescenti (default)',
  FIGLI_ASC: 'Numero Figli crescenti',
  DIPENDENZE_DESC: 'Numero Dipendenze decrescenti',
  DIPENDENZE_ASC: 'Numero Dipendenze crescenti',
  ALFABETICO: 'Alfabetico',
  CASUALE: 'Casuale',
};
