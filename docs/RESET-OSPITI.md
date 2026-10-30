# Pulizia degli account tra soggiorni — 3.3

Nell'area protetta: **Prepara per nuovi ospiti**, inserisci facoltativamente il nome, quindi conferma. La procedura riguarda sempre Prime Video, Spotify, Netflix, YouTube e tutti gli account Google configurati nel profilo Android corrente del televisore. Non elimina account o profili dai servizi online. PIN del proprietario, Wi-Fi, canali e launcher rimangono conservati.

## Prova sul Saba — versione 3.11

Il servizio Accessibilità separato **Pulizia app ospiti** è stato attivato dal proprietario. Quando viene avviato dal PIN, usa esclusivamente la pagina delle impostazioni Android delle quattro app: verifica nome package, intestazione della conferma e pulsante OK, con un limite di tempo per ogni passaggio. Un caricamento copre l'interfaccia; le impostazioni restano comunque in primo piano. Il servizio non controlla lo schermo al di fuori della pulizia e non modifica il servizio del telecomando.

Sul TV, il 30 ottobre 2026, il servizio ha raggiunto le conferme e fatto ritorno alle informazioni di tutte e quattro le app. **Il ritorno dalla schermata non è un callback di successo:** non prova che dati o account siano stati cancellati. La home indica «Verifica richiesta», e chiede una conferma esplicita soltanto dopo aver verificato le quattro app. Gli esiti automaticamente segnati dalla prima versione sul Saba vengono riclassificati come da verificare. Gli account Google del sistema devono essere rimossi e verificati separatamente. Lingue dei profili online non vengono modificate.

Il precedente tentativo si fermava alla terza app perché Android riapriva la schermata precedente. La 3.11 usa una nuova schermata per ciascuna app ed è arrivata anche a YouTube. Testare nuovamente dopo aggiornamenti di Android, modifiche della lingua o dell'interfaccia delle impostazioni; in caso di schermata inattesa il servizio si interrompe senza segnare il passaggio come confermato.

## Procedura dalle impostazioni

Android non concede a un normale launcher il permesso di cancellare i dati delle altre app. Il servizio del telecomando è separato da quello di pulizia.

La procedura guidata funziona così:

1. Apri ciascuna app dalla lista della pulizia. **Apri impostazioni Android** porta alle informazioni dell'app; cerca Memoria → Cancella dati / Cancella tutti i dati. Non basta svuotare la cache o fare logout. Se l'app manca, verificarlo prima di confermare.
2. Tornando alla home, entra nell'area proprietario con il PIN: ritroverai la pulizia in corso. Apri il passaggio e conferma il controllo manuale. La conferma è dichiarata manuale, non spacciata per un esito verificato da Android.
3. Apri **Account Google del TV** e rimuovi tutti gli account dalle impostazioni. Se non ce ne sono, conferma dopo aver controllato. Alcuni firmware aprono una pagina generale: cerca Account / Account e accesso.
4. Solo quando i cinque passaggi risultano confermati puoi completare il soggiorno: italiano, nuovo nome, benvenuto e scelta lingua. Se il cambio lingua non è disponibile, impostare italiano dal sistema e riprovare.

La pulizia è irreversibile dopo la cancellazione dei dati: non c'è un pulsante che annulli il reset e riattivi il soggiorno precedente. Il progresso e il nuovo nome vengono salvati prima di iniziare e resistono a chiusura e riavvio. Durante la preparazione il percorso di benvenuto e il cambio lingua della home rimandano all'area proprietario; questo non è un sistema kiosk che blocca i pulsanti hardware delle app.

## Pulizia automatica opzionale

È incluso un ricevitore amministratore con elenco di policy vuoto. Non richiede la facoltà di ripristino di fabbrica. Installare l'APK o attivare un normale amministratore **non** basta: Android 9+ deve riconoscere il package come **device owner**. Solo in quel caso compare il tasto per azzerare automaticamente le quattro app attraverso `DevicePolicyManager.clearApplicationUserData`.

La configurazione non viene effettuata dallo script d'installazione e non è stata verificata sul Saba. Richiede una verifica sul posto della compatibilità del firmware e dello stato di provisioning. Se il TV richiedesse un ripristino di fabbrica per diventare amministrato, fermarsi e valutarlo separatamente: non è necessario per usare il percorso manuale. Non cancellare account soltanto per forzare il provisioning.

Ogni app riceve la conferma automatica soltanto dopo il callback di successo di Android e il salvataggio del risultato. Fallimenti, app assenti o richieste interrotte richiedono un nuovo tentativo o il percorso manuale. Non è possibile avviare una seconda pulizia o completare il soggiorno mentre ci sono richieste in corso nel processo. Se il processo termina, i passaggi senza conferma rimangono da completare. Nessun servizio permanente o controllo periodico viene aggiunto.

**Gli account Google richiedono sempre il controllo manuale nelle impostazioni**, anche nel percorso amministrato. Un'app normale non vede necessariamente tutti gli account; un elenco vuoto letto dal launcher non costituisce prova della rimozione. Per questo la versione 3.3 non promette un reset di tutti gli account con un solo clic.

## Lingue e accessi

La cancellazione dei dati azzera accessi e preferenze **locali** dell'app. Gli account Google a livello di sistema vanno rimossi separatamente per evitare che YouTube recuperi un accesso precedente. Dopo la pulizia, il TV riparte in italiano e il nuovo ospite sceglie la lingua dal benvenuto.

Le preferenze conservate nei profili online, per esempio la lingua del profilo Netflix, possono tornare al successivo accesso. La home non modifica i profili cloud e non garantisce che tutte le app seguano il sistema: va verificato su ogni app al primo accesso. Il reset non revoca le sessioni su altri dispositivi.

## Verifiche ancora necessarie sul TV

- App con accessi di prova: cancellare dati e verificare nuovo accesso richiesto in tutte e quattro.
- Aggiungere un account Google di prova, rimuoverlo e verificare che YouTube non lo recuperi.
- Uscire/riavviare a metà procedura: progresso conservato, PIN richiesto e nessun nuovo soggiorno dichiarato pronto.
- Provare app assente, cancellazione rifiutata e annullamento di una conferma manuale.
- Completare con nome, verificare italiano, nuovo benvenuto e successiva scelta lingua.
- Se il firmware consente device owner: verificare i quattro callback, errori e uscita durante la pulizia. Questo percorso non è stato provato su hardware.

## Riferimenti Android e app

- [DevicePolicyManager.clearApplicationUserData](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#clearApplicationUserData(android.content.ComponentName,java.lang.String,java.util.concurrent.Executor,android.app.admin.DevicePolicyManager.OnClearApplicationUserDataListener)): richiede device/profile owner.
- [AccountManager](https://developer.android.com/reference/android/accounts/AccountManager): visibilità e accesso agli account limitati.
- [YouTube: rimuovere l'account dal TV](https://support.google.com/youtube/answer/7612539?hl=it).
- [Netflix: lingua del profilo](https://help.netflix.com/it/node/13245).
