package com.wgu.d424.soaksafe.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    @Test
    public void hashAndVerify_roundTrip() {
        String encoded = PasswordHasher.hash("poolOwner2026");
        assertTrue(PasswordHasher.isModernStoredForm(encoded));
        assertTrue(PasswordHasher.verify(encoded, "poolOwner2026"));
        assertFalse(PasswordHasher.verify(encoded, "poolOwner2027"));
    }

    @Test
    public void hash_usesRandomSalt() {
        String a = PasswordHasher.hash("same");
        String b = PasswordHasher.hash("same");
        assertNotEquals(a, b);
        assertTrue(PasswordHasher.verify(a, "same"));
        assertTrue(PasswordHasher.verify(b, "same"));
    }

    @Test
    public void verify_acceptsLegacyPlaintext() {
        assertFalse(PasswordHasher.isModernStoredForm("legacy-secret"));
        assertTrue(PasswordHasher.verify("legacy-secret", "legacy-secret"));
        assertFalse(PasswordHasher.verify("legacy-secret", "other"));
    }
}
