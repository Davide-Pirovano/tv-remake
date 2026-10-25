# Ottimizzazione del Saba — versione 3.2

L'obiettivo è ridurre l'attesa con il telecomando, il costo dell'avvio e il lavoro in background, mantenendo TV antenna, app streaming e accesso alla home originale. La versione 3.2 è pronta nel codice; **non è stata installata né misurata sul TV**, che al momento non è disponibile.

## Lavoro eliminato dal launcher

| Percorso | Prima | Versione 3.2 con il profilo Saba |
| --- | --- | --- |
| Stato Wi-Fi/VPN con indicatore nascosto | Due callback di rete registrati, interrogazioni anche per l'etichetta invisibile | Nessuna registrazione e nessuna lettura dello stato di rete per quell'indicatore |
| Sfondi disattivati | Gestore, ImageView, stato di rotazione e oggetto executor inizializzati comunque | Il gestore non viene creato |
| Timer aggiuntivo della home statica | Aggiornamento del saluto ogni minuto | Aggiornamento alla prossima ora locale; ripianificazione quando cambia l'ora o il fuso |
| Inizializzazione delle scelte del tema | Scrittura sincrona delle preferenze | Aggiornamento immediato in memoria e scrittura asincrona |
| Input TV opzionali nascosti | Callback registrato comunque | Callback solo se il pulsante Ingressi è utilizzato |

L'orologio Android continua ad aggiornarsi normalmente. Il timer al minuto resta disponibile quando servono funzioni dinamiche come sfondi, cielo, festività o calendario eventi. PIN, nuovo soggiorno e altre scritture sensibili conservano il loro comportamento precedente.

Le sezioni `GuestTV.Config` e `GuestTV.AppTiles` sono visibili in una traccia Android quando il tracing è attivo. R8, riduzione delle risorse, animazioni del launcher disattivate, riutilizzo dei riquadri e rilascio delle immagini sotto pressione di memoria erano già presenti e restano attivi.

Questi sono cambiamenti del lavoro eseguito dal codice, **non una percentuale misurata di aumento della velocità del TV**.

## Misura prima dell'aggiornamento

Riattiva il debug e collega il dispositivo sulla sua rete. Esegui gli strumenti dal repository sul computer; non aggiungono servizi al TV.

```sh
adb connect INDIRIZZO_TV:5555
python3 tools/tv_performance.py --serial INDIRIZZO_TV:5555 inspect --output outputs/prima.json
```

`inspect` è di sola lettura. Salva RAM, spazio libero, processi che stanno usando CPU, memoria della nostra home, della home originale e di TV Live, più le statistiche frame del launcher. Un comando non disponibile nel firmware viene indicato nel report invece di essere considerato una misura riuscita.

Prima della raccolta usa la home con le frecce per circa 15 secondi, apri un'app e torna con Home fisico. Ripeti lo stesso percorso dopo l'aggiornamento e salva `outputs/dopo.json`. CPU e RAM sono fotografie istantanee, non un benchmark completo; confronta la stessa schermata e un'attività simile, a download e aggiornamenti terminati.

I report e i backup predefiniti negli esempi sono dentro `outputs/`, escluso da Git. Non contengono il PIN, ma includono dati del dispositivo e nomi dei processi: rimangono locali.

## Profilo Android 11

Dopo aver installato l'APK aggiornato, questo comando disattiva le tre animazioni di sistema e precompila il solo package `it.saba.guesthome` con il filtro ART `speed`:

```sh
python3 tools/tv_performance.py --serial INDIRIZZO_TV:5555 apply --backup outputs/animazioni-originali.json
```

Il comando:

1. Verifica Android 11 e la presenza del launcher.
2. Legge le tre scale attuali e salva un backup privato prima di modificarle; rifiuta di sovrascrivere un backup esistente.
3. Imposta le tre scale a zero e compila il solo launcher.
4. In caso di errore prova a ripristinare subito le scale originali; il backup rimane disponibile.

La compilazione richiede CPU e spazio temporaneamente e prepara codice per i successivi avvii del processo. Non compila tutte le app e non modifica dati, account, canali o impostazioni di debug. La verifica finale va effettuata dopo un riavvio completo, includendo il comportamento della home e dei tasti dedicati.

Per ripristinare esattamente le tre scale, comprese quelle prima assenti:

```sh
python3 tools/tv_performance.py --serial INDIRIZZO_TV:5555 restore --backup outputs/animazioni-originali.json
```

Il ripristino verifica collegamento e firmware del backup. Riguarda le scale delle animazioni; la compilazione ART rimane gestita da Android e può essere aggiornata dal sistema o da una nuova installazione dell'app.

## Ulteriore lavoro sul TV

Il report prima/dopo serve a scegliere gli interventi successivi sul sistema: quali processi consumano CPU mentre la home è ferma, quanto pesa la vecchia home lasciata disponibile, se manca spazio e quali app vengono riavviate per pressione sulla RAM. Gli interventi sui pacchetti richiedono questi dati e la verifica sul Saba, soprattutto per conservare tuner, DRM, app streaming e telecomando.

I processi in cache aiutano a tornare rapidamente alle app: lo scopo è eliminare lavoro inutile, mantenendo il riuso utile della memoria. L'ottimizzazione non include cancellazioni periodiche delle cache, arresti forzati o limiti arbitrari ai processi in background.

## Riferimenti tecnici

- [Android: analizzare e ottimizzare l'avvio](https://developer.android.com/topic/performance/appstartup/analysis-optimization): I/O, inizializzazioni e chiamate Binder sul percorso critico.
- [SharedPreferences.Editor](https://developer.android.com/reference/android/content/SharedPreferences.Editor#apply()): aggiornamento in memoria immediato e scrittura asincrona con `apply()`; le scritture pendenti possono comunque essere attese nelle transizioni del ciclo di vita.
- [AOSP: filtri di compilazione ART](https://source.android.com/docs/core/runtime/configure): `speed` compila i metodi in anticipo; `speed-profile` usa i profili.
- [Android: ciclo di vita dei processi](https://developer.android.com/guide/components/activities/process-lifecycle): riutilizzo dei processi e gestione della memoria.
