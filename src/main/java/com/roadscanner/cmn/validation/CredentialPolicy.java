package com.roadscanner.cmn.validation;

public final class CredentialPolicy {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 20;

    private CredentialPolicy() {
    }

    public static boolean isValidUserId(String userId) {
        return userId != null && userId.matches("[a-z0-9]{6,20}");
    }

    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        int codePointLength = password.codePointCount(0, password.length());
        if (codePointLength < MIN_PASSWORD_LENGTH
                || codePointLength > MAX_PASSWORD_LENGTH) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int index = 0; index < password.length();) {
            int codePoint = password.codePointAt(index);
            index += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)
                    || Character.isSpaceChar(codePoint)
                    || codePoint == 0xFEFF) {
                return false;
            }
            if (Character.isLetter(codePoint)) {
                hasLetter = true;
            } else if (Character.isDigit(codePoint)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }
        return hasLetter && hasDigit && hasSpecial;
    }
}
