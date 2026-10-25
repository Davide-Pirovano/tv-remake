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
6. Eseguire le misure prima/dopo descritte in [PERFORMANCE.md](PERFORMANCE.md), controllando saluto, orologio, ingressi e uso di CPU/RAM con la versione 3.2.

Non usare Home iniettato via ADB per sostituire la prova fisica: non attraversa il filtro dei tasti.

## Benvenuto — versione 3.4

Schermata a tutto schermo con sfondo petrolio sfumato, saluti in 11 lingue, nome ospiti facoltativo e indicazione bilingue per iniziare. Due TextView native in un TextSwitcher: scorrimento verticale di 24 dp e dissolvenza di 420 ms, un cambio ogni 3,2 secondi. Nessun video, immagine remota o dipendenza aggiunta. Se le animazioni di sistema sono disabilitate, i saluti cambiano senza transizioni. Callback e animazioni vengono fermati al cambio schermata, in background e allo scollegamento della vista.

Da verificare sul Saba: leggibilità dei saluti latini, arabi e asiatici; OK fisico e clic aprono immediatamente la scelta lingua; il benvenuto non ritorna dopo aver iniziato; un nuovo soggiorno lo riattiva con il nuovo nome. Controllare anche background/ritorno e animazioni di sistema disattivate. Nessuna prova visiva o misura hardware è stata eseguita per questa revisione.

## Reset ospiti — versione 3.3

Aggiunta la procedura persistente per quattro app e account Google, con conferme manuali esplicite, blocco del completamento parziale e backend nativo per cancellare dati delle app solo quando il launcher è device owner. Sul Saba attuale non è attivato il ruolo: usare la procedura dalle impostazioni. Nessun account o dato del TV è stato modificato durante lo sviluppo. Vedere [RESET-OSPITI.md](RESET-OSPITI.md) per i passaggi e le prove sul dispositivo.

## Verifica locale

Ultima verifica locale: 25 ottobre 2026. Versione 3.4 compilata: 237 test JVM e 10 test Python passati, zero errori lint (restano avvisi), sintassi dello script Bash verificata. I test coprono anche il rifiuto di esiti parziali/falliti, la conferma manuale obbligatoria per Google e il vincolo sulle sole quattro app streaming. I test delle prestazioni coprono pianificazione del saluto, fusi e ora legale, backup prima delle modifiche, ripristino in caso di errore, vincolo sul dispositivo/SDK e rifiuto di backup manomessi. Tutte le misure di velocità del dispositivo restano da raccogliere sul TV.

Comandi riproducibili:

```sh
./gradlew testReleaseUnitTest lintRelease assembleRelease
python3 -m unittest discover -s tools -p 'test_*.py'
bash -n tools/install.sh
```

Non sono inclusi nel repository PIN, backup del dispositivo, file di firma o APK personali.
