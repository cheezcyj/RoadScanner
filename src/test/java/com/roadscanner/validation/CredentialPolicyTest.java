package com.roadscanner.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.roadscanner.cmn.validation.CredentialPolicy;

public class CredentialPolicyTest {

    @Test
    public void acceptsExpectedUserIdAndPassword() {
        assertTrue(CredentialPolicy.isValidUserId("member01"));
        assertTrue(CredentialPolicy.isValidPassword("Password1!"));
        assertTrue(CredentialPolicy.isValidPassword("Abcd1234+"));
        assertTrue(CredentialPolicy.isValidPassword(
                "\uD801\uDC00\uD801\uDC00\uD801\uDC00\uD801\uDC00\uD801\uDC00\uD801\uDC001!"));

		String seventyTwoBytes = repeat("\uD801\uDC00", 17) + "\u00E9" + "1!";
		assertTrue(CredentialPolicy.isValidPassword(seventyTwoBytes));
    }

    @Test
    public void rejectsInvalidUserIds() {
        assertFalse(CredentialPolicy.isValidUserId("short"));
        assertFalse(CredentialPolicy.isValidUserId("UpperCase1"));
        assertFalse(CredentialPolicy.isValidUserId("member-name"));
    }

    @Test
    public void rejectsWeakPasswords() {
        assertFalse(CredentialPolicy.isValidPassword("short1!"));
        assertFalse(CredentialPolicy.isValidPassword("onlyletters!"));
        assertFalse(CredentialPolicy.isValidPassword("OnlyLetters1"));
        assertFalse(CredentialPolicy.isValidPassword("Password 1!"));
        assertFalse(CredentialPolicy.isValidPassword("Password1!\u00A0"));
        assertFalse(CredentialPolicy.isValidPassword("\uD801\uDC00\uD801\uDC00\uD801\uDC00\uD801\uDC001!"));

		String seventyFourBytes = repeat("\uD801\uDC00", 18) + "1!";
		assertFalse(CredentialPolicy.isValidPassword(seventyFourBytes));
    }

	private String repeat(String value, int count) {
		StringBuilder repeated = new StringBuilder(value.length() * count);
		for (int index = 0; index < count; index++) {
			repeated.append(value);
		}
		return repeated.toString();
	}
}
