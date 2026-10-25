# Pulizia Saba: Play Store e mirroring conservati

Profilo preparato sull'inventario salvato del Saba/Changhong DVB92, codename redwood, Android 11. **Non è stato applicato al TV**: il proprietario è fuori dall'appartamento. Nessuna modifica all'APK o al launcher è necessaria per usare lo strumento.

## App accessorie da disattivare

| App / funzione | Package |
| --- | --- |
| Google Play Games | `com.google.android.play.games` |
| YouTube Music | `com.google.android.youtube.tvmusic` |
| Google TV / Play Film | `com.google.android.videos` |
| Alexa Control | `com.homwee.alexacontrol` |
| Store del produttore | `com.homwee.store` |
| Guida del produttore | `com.homwee.help` |
| Portale AI PONT | `com.homwee.aipont` |
| Invio feedback Google | `com.google.android.feedback` |
| Sincronizzazione calendario | `com.google.android.syncadapters.calendar` |
| Configurazione installazioni partner | `android.autoinstalls.config.homwee` |

L'interpretazione delle app del produttore deriva dai nomi dei package/APK e dal profilo della stessa famiglia nel progetto upstream; le dipendenze effettive del firmware vanno verificate con un riavvio. La lista è chiusa: nessun package sconosciuto viene eliminato perché assente dalla home.

## Funzioni conservate

- Casa · Guest TV e home Android TV originale, con i relativi componenti di supporto.
- TV antenna, canali, ingressi HDMI, telecomando, accoppiamento, audio e rete.
- YouTube, Netflix, Prime Video e Spotify.
- Play Store, Google Play Services, Google Services Framework e gestione account per aggiornare le app.
- Chromecast (`com.google.android.apps.mediashell`), Miracast (`com.changhong.inface.net.wfd`), MultiScreenShare (`com.homwee.multiscreenshare`) e CHAirPlay (`com.homwee.mmp.screenshare`).
- Tastiera, impostazioni, servizi di accessibilità, microfono/ricerca vocale e aggiornamenti del firmware e delle app.

Nell'inventario l'APK `CHAirPlay` corrisponde a `com.homwee.mmp.screenshare`: è un indizio del supporto AirPlay, **non una prova di compatibilità con iPhone o video protetti**. Sul posto provare sia Duplica schermo da iPhone sia la trasmissione dall'app YouTube; i protocolli sono differenti. Conservare anche gli altri ricevitori finché le prove non chiariscono quali servono.

## Applicazione sul posto

Servono Python 3 e ADB già autorizzato. Lo strumento non cerca dispositivi e non apre connessioni da solo. Se ADB non è nel PATH, impostare la variabile `ADB` con il percorso dell'eseguibile.

```sh
# Prima leggere l'elenco corrente, senza modifiche.
python3 tools/tv_cleanup.py --serial INDIRIZZO_TV:5555 inspect

# Salvare le misure iniziali descritte in PERFORMANCE.md.
# Poi applicare con un NUOVO file di backup, mai sovrascritto.
python3 tools/tv_cleanup.py --serial INDIRIZZO_TV:5555 apply --backup backup/pulizia-prima.json

# Per annullare:
python3 tools/tv_cleanup.py --serial INDIRIZZO_TV:5555 restore --backup backup/pulizia-prima.json
```

Il profilo accetta solo il fingerprint della famiglia Changhong/DVB92/redwood Android 11 e il profilo Android 0. Salva gli stati esatti delle app presenti in un backup privato prima di modificare il dispositivo. Disattiva soltanto le app della lista attualmente abilitate; conserva le disattivazioni precedenti. Ogni cambio viene verificato rileggendo lo stato Android. In caso di errore tenta il ripristino di tutte le app nel backup. Un'interruzione completa del computer richiede il comando `restore`; non cancellare il backup.

Il ripristino controlla collegamento ADB e fingerprint del firmware e rifiuta package esterni alla lista. Se l'indirizzo del TV cambia o il firmware viene aggiornato, non aggirare il controllo modificando il JSON: verificare prima l'identità del dispositivo.

## RAM e spazio disponibile

La disattivazione non cancella APK, cache o dati. Riduce la possibilità che le app selezionate eseguano attività; il guadagno in RAM/CPU dipende da quali erano realmente attive. **Non promette spazio libero aggiuntivo.**

Una seconda fase, dopo le misure sul TV, può eliminare dati e aggiornamenti delle sole app inutilizzate o disinstallare app non di sistema. Gli APK inclusi nelle partizioni di sistema non diventano spazio disponibile per gli ospiti semplicemente disinstallandoli per l'utente. Non usare root o modificare le partizioni per recuperare quello spazio. Questa fase non è inclusa nel comando `apply`: va decisa sui dati reali di occupazione.

Dopo gli aggiornamenti dal Play Store, rimuovere nuovamente l'account Google del proprietario **prima di consegnare il TV agli ospiti**. La procedura di preparazione nuovi ospiti richiede la verifica di tutti gli account Google: non esiste un'eccezione per un account personale.

## Verifiche sul posto

1. Confrontare CPU, RAM e spazio prima/dopo con `tools/tv_performance.py inspect`.
2. Provare le cinque app, la vecchia home e Home fisico, i pulsanti dedicati, il volume e il microfono.
3. Riavviare e riprovare TV antenna, canali, HDMI e telecomando: alcune dipendenze del firmware emergono solo al riavvio.
4. Provare Chromecast, iPhone/AirPlay e il ricevitore Miracast se disponibile un dispositivo compatibile.
5. Verificare accesso al Play Store, aggiornamenti e successiva rimozione dell'account Google.
6. Se compaiono problemi, ripristinare il backup e ripetere le prove.

Riferimenti: [ADB e package manager Android](https://developer.android.com/tools/adb#pm), [comandi dello stato abilitazione in Android 11](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-11.0.0_r1/services/core/java/com/android/server/pm/PackageManagerShellCommand.java).
