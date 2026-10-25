# Verifiche e consegna

## Confermato sul televisore durante la sessione precedente

- Home del telecomando fisico torna alla nuova interfaccia anche con la home originale abilitata. Conferma visiva del proprietario e log `Home key routed to guest home`.
- Il servizio rimane attivo dopo il riavvio; Home fisico continua ad aprire la nuova interfaccia.
- Il tasto Google Play fisico è bloccato e il volume tenuto premuto funziona.
- PIN valido, tastierino con cifre visibili, Annulla allineato al blocco numerico.
- Spotify nella home, fila app stabilizzata nella parte bassa in cinque ritorni alla home.
- Elenco aggiuntivo con 35 lingue sul Saba; scelta del croato e ripristino italiano riusciti.

## Modifiche completate dopo quelle prove

- `Torna al TV normale` avvia direttamente la home originale se il servizio è attivo. Non apre più la procedura di riabilitazione di Android TV Home.
- Nessuna voce `Tutte le app` o `Configura Google Play` aggiunta all'area proprietario.
- Servizio disponibile durante l'avvio, con apertura della nuova interfaccia dopo lo sblocco utente.
- Ricevitore `BOOT_COMPLETED` con priorità 999 per non aspettare la coda delle altre app. La revisione con priorità precedente era stata compilata/installata, ma TV Live era ancora comparsa prima dell'evento; l'evento risultava in coda. La priorità corretta è stata compilata, **non confermata sul TV**.
- Pulizia dello stato del servizio in caso di scollegamento e riconnessione.
- Configurazione iniziale del PIN privata, esclusa da Git, con comando interattivo e controllo prima della compilazione.
- Script d'installazione autonomo: non dipende dal resto del repository upstream e usa il package corretto `it.saba.guesthome`.

## Prove da completare al ritorno nell'appartamento

1. Installare l'APK aggiornato con la stessa chiave dell'installazione esistente, mantenendo i dati.
2. Dall'area proprietario aprire `Torna al TV normale`: la vecchia home deve aprirsi con un solo clic, senza dialoghi o impostazioni Android.
3. Premere Home fisico: deve comparire la nuova home. Ripetere da un'app aperta dalla vecchia home.
4. Riavviare senza premere il telecomando: al completamento dell'avvio deve comparire la nuova home, anche se il firmware mostra inizialmente TV Live. Controllare che resti visibile.
5. Riprovare Google Play, TV Live, Netflix, YouTube, Prime e Volume − tenuto premuto.

Non usare Home iniettato via ADB per sostituire la prova fisica: non attraversa il filtro dei tasti.

## Verifica locale

Ultima verifica locale: 25 ottobre 2026. Build release riuscita, 230 test JVM e 2 test Python passati, zero errori lint (restano avvisi). Sintassi dello script Bash verificata.

Comandi riproducibili:

```sh
./gradlew testReleaseUnitTest lintRelease assembleRelease
python3 -m unittest discover -s tools -p 'test_*.py'
bash -n tools/install.sh
```

Non sono inclusi nel repository PIN, backup del dispositivo, file di firma o APK personali.
