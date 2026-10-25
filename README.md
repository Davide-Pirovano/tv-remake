# Casa · Guest TV

Launcher Android TV leggero per una casa vacanze, personalizzato per un Saba con Android 11. Basato sul launcher di [CleanYourTV](https://github.com/Gathin23/cleanyourtv), mantenendo la licenza MIT originale. Base upstream: commit `9dc78f8dd5126af6fcd12dd47ac8ac8b30aef588`.

La home ospiti mostra TV antenna, YouTube, Netflix, Prime Video e Spotify. L'area proprietario è protetta dal PIN e include **Torna al TV normale**: apre la home Android TV originale come un'app; **Home sul telecomando riporta alla nuova interfaccia**.

## Funzioni

- Benvenuto una volta per soggiorno, con nome ospiti facoltativo.
- Cinque lingue principali e una lista aggiuntiva ricavata dalle lingue del TV (35 ulteriori lingue sul Saba verificato).
- Cambio lingua della home e del sistema; le app possono usare la lingua del proprio account. I testi della home non tradotti ricadono sull'inglese.
- Rotella per il cambio lingua pubblico, icona utente per il proprietario.
- Prepara nuovi ospiti, ripristina italiano e cambia PIN nell'area protetta.
- Tastierino numerico con cifre visibili, supporto ai numeri del telecomando e Annulla largo quanto il blocco numerico.
- Sfondo petrolio, elementi vettoriali statici e fila app stabilizzata nella parte bassa. Nessuna animazione aggiunta o immagine remota.
- Home diretto alla nuova interfaccia e blocco del solo tasto IR Google Play di questo modello. TV Live, Netflix, YouTube, Prime e volume vengono passati al sistema.

La versione 3.3 include la pulizia di **tutte e quattro le app** e il controllo della rimozione di **tutti gli account Google dal TV**. Il nuovo soggiorno viene attivato solo dopo aver completato i cinque passaggi. Sul TV attuale la pulizia richiede le impostazioni Android: non è ancora configurato come dispositivo amministrato. Dettagli, limiti e percorso automatico opzionale in [docs/RESET-OSPITI.md](docs/RESET-OSPITI.md).

## Stato delle verifiche

Il ritorno dalla vecchia home alla nuova tramite il pulsante Home fisico è stato confermato sul Saba. È stata compilata anche la correzione dell'avvio automatico, ma l'ultima revisione **va ancora provata sul TV**: il proprietario non è più nell'appartamento. Dettagli e prove rimanenti in [docs/VERIFICHE.md](docs/VERIFICHE.md).

La versione nel repository può essere successiva a quella installata sul televisore. Non vengono eseguite installazioni o modifiche da remoto durante la preparazione del repository.

## Prestazioni — versione 3.2

Il profilo Saba evita le letture Wi-Fi/VPN per l'indicatore nascosto, non inizializza il gestore degli sfondi inutilizzato e aggiorna il saluto alla prossima ora locale invece che ogni minuto. Le preferenze non sensibili del tema vengono scritte in modo asincrono. Aspetto, lingue, app e switch della home sono conservati.

[docs/PERFORMANCE.md](docs/PERFORMANCE.md) descrive le modifiche e lo strumento ADB per leggere CPU/RAM/frame, disattivare le animazioni di sistema con backup e precompilare il solo launcher. Il miglioramento reale richiede ancora le misure sul TV: questi comandi non sono stati eseguiti sul dispositivo.

## Compilazione

Servono JDK 17, Android SDK 34 e Python 3. Imposta `ANDROID_HOME` o `sdk.dir` nel file locale `local.properties`.

```sh
python3 tools/configure_owner.py
./gradlew testReleaseUnitTest lintRelease assembleRelease
python3 -m unittest discover -s tools -p 'test_*.py'
```

Il primo comando chiede il PIN iniziale senza stamparlo e crea `app/src/main/assets/owner.json`, escluso da Git. Usa PBKDF2-HMAC-SHA256 con salt casuale e gli stessi parametri dell'app. La compilazione segnala un errore esplicito se il file manca o non è valido.

Queste credenziali servono solo alla **prima installazione**: un aggiornamento non cambia il PIN già salvato sul TV. PIN, file di credenziali iniziali, backup, APK e chiavi di firma non sono pubblicati nel repository.

APK: `app/build/outputs/apk/release/app-release.apk`. La release usa la chiave debug locale, come l'installazione originale: per aggiornare il TV esistente occorre la **stessa chiave del Mac originale**. Non disinstallare l'app per aggirare una firma diversa, perché perderesti PIN e soggiorno.

## Installazione e aggiornamento

Abilita il debug sul TV, collegati con ADB e accetta l'eventuale richiesta sul televisore. Usa l'indirizzo effettivo del TV; nessun indirizzo privato è incorporato negli script.

```sh
adb connect INDIRIZZO_TV:5555
tools/install.sh INDIRIZZO_TV:5555
```

Lo script installa l'APK mantenendo i dati, concede il cambio lingua già utilizzato dal progetto e apre esplicitamente la nuova home. Non attiva servizi Accessibilità e non disabilita launcher. Non richiede root né `WRITE_SECURE_SETTINGS`.

### Prima configurazione dello switch

1. In Impostazioni Android → Accessibilità, attiva manualmente **Telecomando · Home e Google Play** e conferma la richiesta di Android. Su installazioni precedenti il nome era **Blocco tasto Google Play**.
2. Se la home originale era stata disabilitata durante la prima configurazione, riabilitala dopo aver attivato il servizio:

   ```sh
   adb -s INDIRIZZO_TV:5555 shell pm enable --user 0 com.google.android.tvlauncher
   adb -s INDIRIZZO_TV:5555 shell am start -W -n it.saba.guesthome/com.gathin.tvhome.MainActivity
   ```

3. Prova: icona utente → PIN → **Torna al TV normale**, poi **Home fisico**. Il ritorno deve avvenire alla nuova interfaccia. Prova anche Google Play, Volume − tenuto premuto e gli altri pulsanti dedicati.

Il consenso serve soltanto nella configurazione iniziale; il passaggio quotidiano non deve aprire le impostazioni. Per le installazioni già configurate il servizio mantiene il consenso durante l'aggiornamento.

## Come funziona Home

Il firmware del Saba assegna alla home originale una priorità maggiore, anche dopo aver impostato Casa · Guest TV come preferenza Home. Per lasciare entrambe le interfacce disponibili, `PlayKeyBlocker` instrada Home alla nuova home e `GuestActivity` avvia la vecchia con un intent esplicito. Il servizio non legge lo schermo, non osserva cambi di finestra e non usa timer, rete o polling. Il nome della classe è mantenuto per conservare il consenso già dato sul TV.

Il ricevitore `GuestBootReceiver` richiede la nuova home una volta al completamento dell'avvio. La priorità dell'evento è stata corretta perché il firmware richiama TV Live dopo la prima apertura della home. **Questa revisione richiede la prova finale di riavvio sul Saba.**

Il servizio Accessibilità deve rimanere attivo. Se viene disabilitato, il pulsante dell'area proprietario invita a riattivarlo. Gli eventi Home iniettati con `adb shell input` evitano il filtro e possono aprire la home originale: per la manutenzione usa `am start` con il componente esplicito della nuova home.

## Note sul dispositivo

- Verificato su firmware Saba/Changhong, Android 11, software V2.10036. Non è un'ottimizzazione universale per tutti i TV Android.
- Il tasto Google Play è identificato da scan code 501 e ricevitore `MStar Smart TV IR Receiver`. Su altri telecomandi va verificato.
- Il cambio lingua usa un'API interna Android e va ricontrollato dopo aggiornamenti del firmware.
- Non è stata misurata una percentuale di miglioramento delle prestazioni.

La base upstream e il font Outfit mantengono le rispettive licenze: [LICENSE](LICENSE) e [FONT-LICENSE-OFL.txt](FONT-LICENSE-OFL.txt). Nel layout di questo repository il font si trova in `app/src/main/res/font/`.
