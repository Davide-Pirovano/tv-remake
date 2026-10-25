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
    private PinPad activePinPad;
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
        if (!prefs(activity).getBoolean("ready", false))
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
        if (getIntent().getBooleanExtra("owner", false)) login();
        else if (getIntent().getBooleanExtra("settings", false)) guestSettings();
        else if (prefs(this).getBoolean("welcome_seen", false)) languages();
        else welcome();
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void page(String title, String subtitle) {
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
        String name = prefs(this).getString("guest", "");
        page("Benvenuti · Welcome" + (name.isEmpty() ? "" : "\n"+name),
                "Scegliete la vostra lingua per iniziare.\nChoose your language to get started.");
        Button start=button("Inizia · Start", () -> {
            prefs(this).edit().putBoolean("welcome_seen", true).commit();
            languages();
        });
        GuestStyle.button(start,true);start.requestFocus();
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
        page("Nuovo soggiorno", "Nome ospiti facoltativo: apparirà solo nel benvenuto.\nQuesta funzione non disconnette gli account delle app.");
        EditText name=field("Es. Marco e Giulia",false);
        button("Conferma nuovo soggiorno",()->new AlertDialog.Builder(this).setTitle("Preparare il nuovo soggiorno?")
                .setMessage("Sostituisce il nome precedente, ripristina l’italiano e riattiva benvenuto e scelta lingua.")
                .setNegativeButton("Annulla",null).setPositiveButton("Conferma",(d,w)->{
                    prefs(this).edit().putString("guest",name.getText().toString().trim())
                            .putString("language","it").putBoolean("ready",false).putBoolean("welcome_seen",false).commit();
                    systemLanguage("it"); home();
                }).show());
        button("Annulla",this::ownerMenu); name.requestFocus();
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
    @Override protected void onStop() {
        super.onStop();
        // Owner access ends when leaving this activity, including opening the TV settings.
        if (authenticated && getIntent().getBooleanExtra("owner", false)) {
            authenticated = false;
            finish();
        }
    }
    @Override public void onBackPressed() {
        if(getIntent().getBooleanExtra("owner",false))finish();
        else if(prefs(this).getBoolean("ready",false))home();
        else if(prefs(this).getBoolean("welcome_seen",false))languages();
        else welcome();
    }
}
