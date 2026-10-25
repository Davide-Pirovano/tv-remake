package com.gathin.tvhome;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class GuestLanguagesTest {
 @Test public void listKeepsSystemLanguagesWithoutDuplicatesOrPseudoLocales() {
  List<Locale> items=GuestLanguages.otherLocales(new String[]{"it-IT","en-US","en-XA","ar-XB","ja-JP","ja_JP","ar-EG","zh-TW",""});
  Set<String> tags=new HashSet<>();for(Locale locale:items)tags.add(locale.toLanguageTag());
  assertEquals(new HashSet<>(Arrays.asList("ja-JP","ar-EG","zh-TW")),tags);
  assertEquals(3,items.size());
 }
 @Test public void regionalDuplicatesPreferBaseAndChineseScriptsStayDistinct() {
  List<Locale> items=GuestLanguages.otherLocales(new String[]{"hr-HR","hr","zh-CN","zh-TW"});
  Set<String> tags=new HashSet<>();for(Locale l:items)tags.add(l.toLanguageTag());
  assertEquals(new HashSet<>(Arrays.asList("hr","zh-CN","zh-TW")),tags);
 }
}
