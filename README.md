# Distributed Task Scheduler

## Introduzione

Il progetto è sviluppato nell'ambito dell'insegnamento "Tecniche Avanzate di Programmazione" del corso di laurea magistrale in ingegneria informatica dell'Università degli Studi della Basilicata. Il sistema ha come obiettivo l'esecuzione di workflow complessi costituiti da task eterogenei e interconnessi.

## Contenuto del repository

### Docs

La cartella `docs` contiene i docmenti progettuali prodotti per il primo checkpoint (documento di visione, traccia, WoW).

La sottocartella `diagrammi` contiene gli svg e i sorgenti mermaid dei diagrammi presentati.


### Database

Il database è PostgreSQL

### Backend

Backend in Java espone una API REST realizzata con il framework Quarkus.

Per avviarlo (è necessaria la JVM 25), dalla cartella `backend` eseguire il comando

```bash
./gradlew quarkusDev
```

Il backend si avvia di default sulla porta 8080.

### Frontend

Frontend realizzato con il framework angular.

Per avviarlo (è necessario avere installato npm), dalla cartella `frontend` eseguire i comandi:

```bash
npm i
npm run start
```

Il frontend si avvia di default sulla porta 4200.