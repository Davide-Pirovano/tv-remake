package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Personal guest flow. Owner actions are private activities, authenticated each time. */
public final class GuestActivity extends Activity {
    private LinearLayout panel;
    private boolean authenticated;
    private boolean externalResetSettings;
    private static int resetJobs; // Main-thread callbacks only; no background polling.
    private PinPad activePinPad;
    private GuestWelcomeView welcomeView;
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("stay", MODE_PRIVATE); }
    static Context localized(Context base) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(prefs(base).getString("language", "it")));
        return base.createConfigurationContext(config);
    }
    static void owner(Activity activity) {
        activity.startActivity(new Intent(activity, GuestActivity.class).putExtra("owner", true));
    }
    static void settings(Activity activity) {
        activity.startActivity(new Intent(activity, GuestActivity.class).putExtra("settings", true));
    }
    static void onHome(Activity activity) {
        if (prefs(activity).getBoolean("reset_pending", false) || !prefs(activity).getBoolean("ready", false))
            activity.startActivity(new Intent(activity, GuestActivity.class));
    }
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(localized(base)); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            if (!prefs(this).contains("pin_hash")) {
                try (java.io.InputStream input = getAssets().open("owner.json")) {
                    JSONObject initial = new JSONObject(new java.util.Scanner(input, "UTF-8").useDelimiter("\\A").next());
                    prefs(this).edit().putString("pin_hash", initial.getString("hash"))
                            .putString("pin_salt", initial.getString("salt")).commit();
                }
            }
        } catch (Exception e) { page("Configurazione non disponibile", "Contattare il proprietario."); return; }
        if(prefs(this).getBoolean("reset_pending",false) && !managedResetAvailable()
                && !prefs(this).getBoolean("cleanup_results_checked",false)) {
            SharedPreferences.Editor migration=prefs(this).edit().putBoolean("cleanup_results_checked",true);
            for(int i=0;i<StayReset.PACKAGES.length;i++)
                if("automatico".equals(prefs(this).getString("reset_step_"+i,"")))
                    migration.putString("reset_step_"+i,"da verificare");
            migration.commit();
        }
        if (getIntent().getBooleanExtra("owner", false)) login();
        else if (prefs(this).getBoolean("reset_pending", false)) resetLocked();
        else if (getIntent().getBooleanExtra("settings", false)) guestSettings();
        else if (prefs(this).getBoolean("welcome_seen", false)) languages();
        else welcome();
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void page(String title, String subtitle) {
        if(welcomeView!=null) {welcomeView.stop();welcomeView=null;}
        activePinPad = null;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(GuestStyle.CANVAS);
        panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(64), dp(18), dp(64), dp(18));
        scroll.addView(panel); setContentView(scroll);
        TextView heading = new TextView(this); heading.setText(title); heading.setTextColor(Color.WHITE);
        heading.setTextSize(32); heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); heading.setLetterSpacing(-.025f); heading.setGravity(Gravity.CENTER);
        panel.addView(heading);
        TextView desc = new TextView(this); desc.setText(subtitle); desc.setTextSize(17);
        desc.setTextColor(GuestStyle.SILVER); desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, dp(12), 0, dp(18)); panel.addView(desc);
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(18);
        b.setTextColor(Color.WHITE); b.setPadding(dp(22), dp(8), dp(22), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(440), dp(44)); lp.topMargin=dp(7);
        panel.addView(b,lp);
        GuestStyle.button(b,false);
        b.setOnClickListener(v -> action.run()); return b;
    }
    private EditText field(String hint, boolean pin) {
        EditText e = new EditText(this); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.LTGRAY);
        e.setHint(hint); e.setSingleLine(true); e.setTextSize(20);
        e.setInputType(pin ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(pin ? 8 : 60)});
        panel.addView(e,new LinearLayout.LayoutParams(dp(440),dp(54))); return e;
    }
    private String text(String it, String en, String fr, String de, String es) {
        switch(Locale.forLanguageTag(prefs(this).getString("language","it")).getLanguage()) {
            case "en": return en; case "fr": return fr; case "de": return de; case "es": return es;
            case "it": return it; default: return en;
        }
    }
    private void welcome() {
        if(welcomeView!=null)welcomeView.stop();
        activePinPad=null;
        welcomeView=new GuestWelcomeView(this,prefs(this).getString("guest", ""),()->{
            if(prefs(this).edit().putBoolean("welcome_seen",true).commit())languages();
            else {toast("Impossibile salvare. Riprova.");welcome();}
        });
        setContentView(welcomeView);
        welcomeView.requestFocus();
    }
    private void languages() {
        page("Lingua · Language", "Italiano / English / Français / Deutsch / Español");
        String[] codes={"it","en","fr","de","es"};
        String[] labels={"Italiano","English","Français","Deutsch","Español"};
        for (int i=0;i<codes.length;i++) {
            final String code=codes[i]; Button b=button(labels[i],()->selectLanguage(code));
            if(i==0)b.requestFocus();
        }
        button(getString(R.string.guest_other_languages),this::otherLanguages);
        if(prefs(this).getBoolean("ready",false))button(getString(R.string.guest_back),this::home);
    }
    private void guestSettings() {
        page(getString(R.string.settings), "");
        button(getString(R.string.guest_language),this::languages).requestFocus();
        button(getString(R.string.guest_back),this::home);
    }
    private void otherLanguages() {
        java.util.List<Locale> locales=GuestLanguages.otherLocales(android.content.res.Resources.getSystem().getAssets().getLocales());
        page(getString(R.string.guest_other_languages),getString(R.string.guest_language_hint)+" · "+locales.size());
        ScrollView scroll=(ScrollView)panel.getParent();scroll.removeView(panel);
        setContentView(panel);panel.setBackgroundColor(GuestStyle.CANVAS);panel.setGravity(Gravity.TOP);
        android.widget.ListView list=new android.widget.ListView(this);
        list.setBackground(GuestStyle.surface(getResources().getDisplayMetrics().density,GuestStyle.DEEP,16));
        list.setClipToOutline(true);list.setDividerHeight(0);list.setSelector(new android.graphics.drawable.ColorDrawable(GuestStyle.SURFACE));
        list.setAdapter(new android.widget.ArrayAdapter<Locale>(this,android.R.layout.simple_list_item_1,locales) {
            @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                TextView row=(TextView)super.getView(position,recycled,parent);
                Locale locale=getItem(position);row.setText(locale.getDisplayName(locale));
                row.setTextColor(GuestStyle.MIST);row.setTextSize(19);row.setMinHeight(dp(46));
                row.setPadding(dp(20),dp(10),dp(20),dp(10));return row;
            }
        });
        panel.addView(list,new LinearLayout.LayoutParams(dp(640),0,1));
        list.setOnItemClickListener((parent,view,position,id)->selectLanguage(locales.get(position).toLanguageTag()));
        button(getString(R.string.guest_back),this::languages);list.requestFocus();
    }
    private void selectLanguage(String code) {
        if(prefs(this).getBoolean("reset_pending",false)) { resetLocked(); return; }
        prefs(this).edit().putString("language",code).putBoolean("ready",true).commit();
        boolean systemChanged=systemLanguage(code);
        if (!systemChanged) {
            page(text("Lingua selezionata", "Language selected", "Langue sélectionnée", "Sprache ausgewählt", "Idioma seleccionado"),
                    text("La home usa la lingua scelta. Per applicarla anche al TV, selezionala nelle impostazioni di sistema.",
                    "Home uses your language. To apply it to the TV too, select it in system settings.",
                    "L’accueil utilise votre langue. Sélectionnez-la aussi dans les paramètres du téléviseur.",
                    "Die Startseite nutzt Ihre Sprache. Wählen Sie diese auch in den TV-Einstellungen.",
                    "El inicio usa tu idioma. Selecciónalo también en los ajustes del televisor."));
            button("Lingua TV · TV language",()->{
                try { startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS)); }
                catch (Exception e) { Toast.makeText(this,"Impostazioni lingua non disponibili",Toast.LENGTH_LONG).show(); }
            });
            button("Home",this::home).requestFocus();
        } else home();
    }
    /** Uses only the explicitly granted configuration permission; no root or device policy changes. */
    private boolean systemLanguage(String code) {
        try {
            Class<?> picker=Class.forName("com.android.internal.app.LocalePicker");
            picker.getDeclaredMethod("updateLocales", LocaleList.class)
                    .invoke(null,new LocaleList(Locale.forLanguageTag(code)));
            android.util.Log.i("GuestTV", "System locale requested: "+code);
            return true;
        } catch (Exception | LinkageError e) {
            android.util.Log.w("GuestTV","System locale unavailable",e); return false;
        }
    }
    private void home() {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(panel.getWindowToken(),0);
        startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish();
    }
    private void keypad(String title, String hint, java.util.function.Consumer<String> action, Runnable cancel) {
        page(title,hint);
        activePinPad=new PinPad(this,action);
        panel.addView(activePinPad);
        Button cancelButton=button("Annulla",cancel);
        android.view.ViewGroup.LayoutParams params=cancelButton.getLayoutParams();
        params.width=dp(PinPad.WIDTH_DP);cancelButton.setLayoutParams(params);
    }
    private void login() {
        keypad("Area proprietario", "Inserisci il PIN con le frecce e OK, oppure con i tasti numerici.", entered -> {
            if(System.currentTimeMillis()<prefs(this).getLong("locked_until",0)) { toast("Attendi 30 secondi prima di riprovare."); return; }
            if(!entered.matches("[0-9]{6,8}")) {toast("Inserisci il PIN di 6–8 cifre.");return;}
            PinPad pad=activePinPad;pad.busy(true);
            new Thread(()->{
                boolean ok=verify(entered);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed())return;
                    pad.clear();pad.busy(false);
                    if(ok) {prefs(this).edit().putInt("failures",0).remove("locked_until").commit();authenticated=true;ownerMenu();}
                    else {
                        int n=prefs(this).getInt("failures",0)+1;
                        SharedPreferences.Editor ed=prefs(this).edit().putInt("failures",n);
                        if(n>=5)ed.putLong("locked_until",System.currentTimeMillis()+30000).putInt("failures",0);
                        ed.commit();toast("PIN non corretto.");
                    }
                });
            },"owner-pin").start();
        },this::finish);
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if(activePinPad!=null) {
            int code=event.getKeyCode();int digit=-1;
            if(code>=android.view.KeyEvent.KEYCODE_0&&code<=android.view.KeyEvent.KEYCODE_9)digit=code-android.view.KeyEvent.KEYCODE_0;
            else if(code>=android.view.KeyEvent.KEYCODE_NUMPAD_0&&code<=android.view.KeyEvent.KEYCODE_NUMPAD_9)digit=code-android.view.KeyEvent.KEYCODE_NUMPAD_0;
            if(digit>=0) {if(event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)activePinPad.digit((char)('0'+digit));return true;}
            if(code==android.view.KeyEvent.KEYCODE_DEL) {if(event.getAction()==android.view.KeyEvent.ACTION_DOWN)activePinPad.erase();return true;}
        }
        return super.dispatchKeyEvent(event);
    }
    private void ownerMenu() {
        if(!authenticated) { login(); return; }
        if(prefs(this).getBoolean("reset_pending",false)) { resetProgress(); return; }
        page("Gestione soggiorno", "Le modifiche vengono applicate solo dopo la conferma.");
        button("Prepara per nuovi ospiti", this::prepare).requestFocus();
        button("Ripristina lingua italiana",()->new AlertDialog.Builder(this).setTitle("Ripristinare l’italiano?")
                .setMessage("Il nome ospiti e il benvenuto rimangono invariati.")
                .setNegativeButton("Annulla",null).setPositiveButton("Ripristina",(d,w)->selectLanguage("it")).show());
        button("Cambia PIN proprietario",this::changePin);
        button("Impostazioni TV",()->startActivity(new Intent(Settings.ACTION_SETTINGS)));
        button("Torna al TV normale",this::normalTv);
        button("Torna alla home",this::home);
    }
    private void prepare() {
        if(!authenticated) { login(); return; }
        page("Nuovo soggiorno", "Nome ospiti facoltativo. Il reset comprende Prime Video, Spotify, Netflix, YouTube e gli account Google sul TV.");
        EditText name=field("Es. Marco e Giulia",false);
        button("Prepara e pulisci account",()->new AlertDialog.Builder(this).setTitle("Preparare il nuovo soggiorno?")
                .setMessage("Cancella accessi e preferenze locali delle quattro app e richiede la rimozione di tutti gli account Google dal TV. Servirà un nuovo accesso. Il reset non è annullabile; PIN, Wi-Fi e canali restano conservati.")
                .setNegativeButton("Annulla",null).setPositiveButton("Inizia",(d,w)->{
                    SharedPreferences.Editor edit=prefs(this).edit().putBoolean("reset_pending",true)
                            .putString("reset_guest",name.getText().toString().trim())
                            .putString("reset_session",java.util.UUID.randomUUID().toString());
                    for(int i=0;i<StayReset.STEPS;i++)edit.remove("reset_step_"+i);
                    if(edit.commit()) {
                        if(managedResetAvailable())clearStreamingApps();
                        else if(GuestCleanupService.available())startAccessibleCleanup();
                        else resetProgress();
                    } else toast("Impossibile salvare. Reset non iniziato.");
                }).show());
        button("Annulla",this::ownerMenu); name.requestFocus();
    }
    private void resetLocked() {
        page("Preparazione da completare", "Il proprietario deve completare la pulizia degli account prima del nuovo soggiorno.");
        button("Area proprietario",()->{getIntent().putExtra("owner",true);login();}).requestFocus();
    }
    private String[] resetResults() {
        String[] results=new String[StayReset.STEPS];
        for(int i=0;i<results.length;i++)results[i]=prefs(this).getString("reset_step_"+i,"");
        return results;
    }
    private boolean managedResetAvailable() {
        android.app.admin.DevicePolicyManager manager=getSystemService(android.app.admin.DevicePolicyManager.class);
        return android.os.Build.VERSION.SDK_INT>=28 && manager!=null && manager.isDeviceOwnerApp(getPackageName());
    }
    private void resetProgress() {
        if(!authenticated) {login();return;}
        page("Pulizia per nuovi ospiti", "Avvia la pulizia delle quattro app e controlla gli esiti.\nI profili e le lingue salvati online non vengono modificati.");
        if(managedResetAvailable())button("Azzera automaticamente le quattro app",this::clearStreamingApps).setEnabled(resetJobs==0);
        else if(GuestCleanupService.available())button("Pulisci automaticamente le quattro app",this::startAccessibleCleanup).setEnabled(!GuestCleanupService.active());
        else button("Attiva pulizia automatica",()->{
            externalResetSettings=true;
            try {startActivity(new Intent(Settings.ACTION_SETTINGS));}
            catch(android.content.ActivityNotFoundException | SecurityException e) {
                externalResetSettings=false;toast("Apri Accessibilità dalle impostazioni del TV.");
            }
        });
        if(resetJobs>0)button("Aggiorna esiti — pulizia in corso",this::resetProgress);
        String[] results=resetResults();
        boolean allRequested=true;
        for(int i=0;i<StayReset.PACKAGES.length;i++) {
            String status=results[i].isEmpty()?"Da completare":("da verificare".equals(results[i])?"Verifica richiesta":"OK · "+results[i]);
            TextView row=new TextView(this);row.setText(StayReset.LABELS[i]+" · "+status);
            row.setTextColor(GuestStyle.MIST);row.setTextSize(16);row.setGravity(Gravity.CENTER);
            row.setPadding(0,dp(3),0,dp(3));panel.addView(row);
            if(!"da verificare".equals(results[i]) && !"automatico".equals(results[i]) && !"manuale".equals(results[i]))allRequested=false;
        }
        if(allRequested)button("Conferma verifica delle app",()->new AlertDialog.Builder(this)
                .setTitle("Hai verificato le quattro app?")
                .setMessage("Android non comunica alla home l’esito della cancellazione. Conferma solo dopo aver controllato che Prime Video, Spotify, Netflix e YouTube non mostrino gli account precedenti.")
                .setNegativeButton("Annulla",null).setPositiveButton("Ho verificato",(d,w)->{
                    SharedPreferences.Editor edit=prefs(this).edit();
                    for(int i=0;i<StayReset.PACKAGES.length;i++)edit.putString("reset_step_"+i,"manuale");
                    if(edit.commit())resetProgress();else toast("Conferma non salvata. Riprova.");
                }).show());
        button("Account Google del TV · "+(results[4].isEmpty()?"Da completare":"OK"),()->resetStep(4));
        Button finish=button("Completa e ripristina italiano",this::finishReset);
        finish.setEnabled(resetJobs==0 && StayReset.complete(results));
        button("Esci — riprenderai dopo il PIN",this::finish);
    }
    private void startAccessibleCleanup() {
        if(!authenticated || !GuestCleanupService.available() || GuestCleanupService.active())return;
        if(!systemLanguage("it") && !"it".equals(android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage())) {
            toast("Imposta prima l’italiano nelle impostazioni TV.");return;
        }
        GuestCleanupService.begin();
    }
    private void resetStep(int step) {
        if(!authenticated) {login();return;}
        if(resetJobs>0) {toast("Attendi la pulizia automatica, poi aggiorna gli esiti.");return;}
        boolean accounts=step==4;
        page(StayReset.LABELS[step],accounts
                ?"Nelle impostazioni del TV rimuovi TUTTI gli account Google. Uscire soltanto da YouTube non basta. Se non ci sono account, conferma dopo aver controllato."
                :"Apri le informazioni dell’app, poi Memoria → Cancella dati / Cancella tutti i dati. Svuotare soltanto la cache o uscire dall’account non basta. Se l’app non è installata, verifica prima di confermare.");
        button("Apri impostazioni Android",()->{
            Intent intent=accounts?new Intent(Settings.ACTION_SETTINGS)
                    :new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+StayReset.PACKAGES[step]));
            externalResetSettings=true;
            try {startActivity(intent);}
            catch(android.content.ActivityNotFoundException|SecurityException e) {
                try {startActivity(new Intent(Settings.ACTION_SETTINGS));}
                catch(android.content.ActivityNotFoundException|SecurityException ignored){
                    externalResetSettings=false;toast("Impostazioni non disponibili: aprirle dal telecomando.");
                }
            }
        }).requestFocus();
        button(accounts?"Conferma account Google rimossi":"Conferma dati cancellati / app assente",()->
                new AlertDialog.Builder(this).setTitle("Conferma controllo manuale")
                    .setMessage("La home non può verificare questo passaggio nelle impostazioni. Conferma soltanto dopo averlo completato e controllato. Tornando dalle impostazioni sarà richiesto nuovamente il PIN.")
                    .setNegativeButton("Annulla",null).setPositiveButton("Ho controllato",(d,w)->{
                        if(prefs(this).edit().putString("reset_step_"+step,"manuale").commit())resetProgress();
                        else toast("Conferma non salvata. Riprova.");
                    }).show());
        button("Indietro",this::resetProgress);
    }
    private void clearStreamingApps() {
        if(android.os.Build.VERSION.SDK_INT<28 || !authenticated || resetJobs>0 || !managedResetAvailable())return;
        String session=prefs(this).getString("reset_session","");
        page("Azzeramento app", "Attendi gli esiti di Android. Gli account Google vanno comunque controllati nelle impostazioni.");
        android.app.admin.DevicePolicyManager manager=getSystemService(android.app.admin.DevicePolicyManager.class);
        android.content.ComponentName admin=new android.content.ComponentName(this,GuestDeviceAdminReceiver.class);
        for(int i=0;i<StayReset.PACKAGES.length;i++) {
            final int step=i;
            TextView status=new TextView(this);status.setTextColor(Color.WHITE);
            status.setText(StayReset.LABELS[i]+" · In corso");panel.addView(status);
            // Invalidate earlier confirmations before issuing another destructive operation.
            if(!prefs(this).edit().remove("reset_step_"+step).commit()) {
                status.setText(StayReset.LABELS[i]+" · Errore di salvataggio");continue;
            }
            try {
                resetJobs++;
                manager.clearApplicationUserData(admin,StayReset.PACKAGES[i],getMainExecutor(),(pkg,success)->{
                    resetJobs--;
                    if(!prefs(this).getBoolean("reset_pending",false)
                            || !session.equals(prefs(this).getString("reset_session","")))return;
                    boolean saved=success && prefs(this).edit().putString("reset_step_"+step,"automatico").commit();
                    status.setText(StayReset.LABELS[step]+(saved?" · Dati cancellati":" · Non completato: riprova o usa le impostazioni"));
                });
            }catch(RuntimeException e){resetJobs--;status.setText(StayReset.LABELS[i]+" · Non consentito: usa le impostazioni");}
        }
        button("Torna ai passaggi",this::resetProgress);
    }
    private void finishReset() {
        if(!authenticated || resetJobs>0 || !prefs(this).getBoolean("reset_pending",false)
                || !StayReset.complete(resetResults()))return;
        // Restore language before making the new stay available; never claim a failed locale reset.
        if(!systemLanguage("it") && !"it".equals(android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0).getLanguage())) {
            toast("Ripristina l’italiano nelle impostazioni del TV e riprova.");
            try {startActivity(new Intent(Settings.ACTION_LOCALE_SETTINGS));}
            catch(android.content.ActivityNotFoundException|SecurityException ignored){}
            return;
        }
        SharedPreferences.Editor edit=prefs(this).edit().putString("guest",prefs(this).getString("reset_guest",""))
                .putString("language","it").putBoolean("ready",false).putBoolean("welcome_seen",false)
                .remove("reset_pending").remove("reset_guest").remove("reset_session");
        for(int i=0;i<StayReset.STEPS;i++)edit.remove("reset_step_"+i);
        if(edit.commit())home();else toast("Preparazione non salvata. Riprova.");
    }
    private void normalTv() {
        if(!authenticated){login();return;}
        if(!PlayKeyBlocker.isConnected()){
            new AlertDialog.Builder(this).setTitle("Telecomando non attivo")
                .setMessage("Riattiva il servizio del telecomando in Accessibilità per mantenere Home sulla nuova interfaccia.")
                .setNegativeButton("Annulla",null).setPositiveButton("Apri impostazioni",(d,w)->
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_MAIN)
                .setComponent(new android.content.ComponentName("com.google.android.tvlauncher","com.google.android.tvlauncher.MainActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(android.content.ActivityNotFoundException|SecurityException e){toast("Home originale non disponibile. Contattare il proprietario.");}
    }
    private void changePin() {
        if(!authenticated){login();return;}
        keypad("Nuovo PIN", "Scegli da 6 a 8 cifre e premi OK.", first -> {
            if(!first.matches("[0-9]{6,8}")){toast("Inserisci da 6 a 8 cifre.");return;}
            keypad("Conferma PIN", "Ripeti il nuovo PIN e premi OK.", second -> {
                if(!first.equals(second)){activePinPad.clear();toast("I PIN sono diversi. Riprova.");return;}
                PinPad pad=activePinPad;pad.busy(true);
                new Thread(()->{
                    try {
                        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
                        String digest=hash(second,salt);
                        prefs(this).edit().putString("pin_salt",hex(salt)).putString("pin_hash",digest).commit();
                        runOnUiThread(()->{if(!isFinishing()){toast("PIN aggiornato.");ownerMenu();}});
                    }catch(Exception e){runOnUiThread(()->{pad.busy(false);toast("PIN non aggiornato. Riprova.");});}
                },"change-pin").start();
            },this::ownerMenu);
        },this::ownerMenu);
    }
    private boolean verify(String pin) {
        try {
            String expected=prefs(this).getString("pin_hash","");
            String actual=hash(pin,unhex(prefs(this).getString("pin_salt","")));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),actual.getBytes(StandardCharsets.US_ASCII));
        }catch(Exception e){return false;}
    }
    static String hash(String pin,byte[] salt)throws Exception {
        PBEKeySpec spec=new PBEKeySpec(pin.toCharArray(),salt,60000,256);
        try{return hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());}
        finally{spec.clearPassword();}
    }
    static String hex(byte[] data) {StringBuilder b=new StringBuilder();for(byte n:data)b.append(String.format(Locale.ROOT,"%02x",n&255));return b.toString();}
    static byte[] unhex(String text) {byte[] b=new byte[text.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(text.substring(i*2,i*2+2),16);return b;}
    private void toast(String text) {Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    @Override protected void onStart() {
        super.onStart();
        if(welcomeView!=null)welcomeView.start();
    }
    @Override protected void onResume() {
        super.onResume();
        if(externalResetSettings && !authenticated) {
            externalResetSettings=false;
            login();
        }
    }
    @Override protected void onPause() {
        if(externalResetSettings)authenticated=false;
        super.onPause();
    }
    @Override protected void onStop() {
        if(welcomeView!=null)welcomeView.stop();
        super.onStop();
        // Owner access ends when leaving this activity, including opening the TV settings.
        if (authenticated && getIntent().getBooleanExtra("owner", false)) {
            authenticated = false;
            if(!externalResetSettings)finish();
        }
    }
    @Override public void onBackPressed() {
        if(getIntent().getBooleanExtra("owner",false))finish();
        else if(prefs(this).getBoolean("reset_pending",false))resetLocked();
        else if(prefs(this).getBoolean("ready",false))home();
        else if(prefs(this).getBoolean("welcome_seen",false))languages();
        else welcome();
    }
}
