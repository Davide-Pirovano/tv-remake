package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlayKeyBlockerTest {
 @Test public void onlyRecordedKeyAndReceiverMatch() {
  assertTrue(PlayKeyBlocker.isStoreKey(501,"MStar Smart TV IR Receiver"));
  assertFalse(PlayKeyBlocker.isStoreKey(500,"MStar Smart TV IR Receiver"));
  assertFalse(PlayKeyBlocker.isStoreKey(501,"Another keyboard"));
  assertFalse(PlayKeyBlocker.isStoreKey(501,null));
 }
}
