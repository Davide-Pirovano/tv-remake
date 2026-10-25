package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class GuestSecurityTest {
 @Test public void pinHashMatchesIndependentPbkdf2Vector() throws Exception {
   byte[] salt=GuestActivity.unhex("000102030405060708090a0b0c0d0e0f");
   assertEquals("407cf2510c0ab6009daf6b18f2edfccf545287211d428264da036fb73d5361a7",GuestActivity.hash("123456",salt));
   assertNotEquals(GuestActivity.hash("123456",salt),GuestActivity.hash("123457",salt));
   assertArrayEquals(salt,GuestActivity.unhex(GuestActivity.hex(salt)));
 }
}
