package com.gathin.tvhome;
import java.util.*;
/** Builds the long list only when requested; source tags come from this TV's system resources. */
final class GuestLanguages {
    static List<Locale> otherLocales(String[] tags) {
        Set<String> common=new HashSet<>(Arrays.asList("it","en","fr","de","es"));
        Map<String,Locale> unique=new TreeMap<>();
        for(String raw:tags) {
            Locale l=Locale.forLanguageTag(raw.replace('_','-'));
            String tag=l.toLanguageTag(), language=l.getLanguage();
            if(language.isEmpty()||common.contains(language)||tag.equals("ar-XB")||tag.equals("en-XA")||language.equals("zz"))continue;
            String script=l.getScript();
            if(language.equals("zh")&&script.isEmpty())
                script=(l.getCountry().equals("TW")||l.getCountry().equals("HK"))?"Hant":"Hans";
            String key=language+"-"+script;
            Locale previous=unique.get(key);
            if(previous==null||l.getCountry().isEmpty())unique.put(key,l);
        }
        List<Locale> result=new ArrayList<>(unique.values());
        result.sort(Comparator.comparing(l->l.getDisplayName(l),String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}
