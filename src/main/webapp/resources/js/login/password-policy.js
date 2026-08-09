(function (window) {
    "use strict";

    var letterPattern;
    var digitPattern;
    try {
        letterPattern = new RegExp("\\p{L}", "u");
        digitPattern = new RegExp("\\p{Nd}", "u");
    } catch (ignored) {
        letterPattern = /[A-Za-z]/;
        digitPattern = /[0-9]/;
    }

    function isValidPassword(password) {
        var characters = typeof password === "string" ? Array.from(password) : [];
        if (typeof password !== "string" || characters.length < 8 || characters.length > 20
                || utf8ByteLength(characters) > 72 || /\s/.test(password)) {
            return false;
        }

        var hasLetter = false;
        var hasDigit = false;
        var hasSpecial = false;
        characters.forEach(function (character) {
            if (letterPattern.test(character)) {
                hasLetter = true;
            } else if (digitPattern.test(character)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        });
        return hasLetter && hasDigit && hasSpecial;
    }

    function utf8ByteLength(characters) {
        return characters.reduce(function (length, character) {
            var codePoint = character.codePointAt(0);
            if (codePoint <= 0x7F) {
                return length + 1;
            }
            if (codePoint <= 0x7FF) {
                return length + 2;
            }
            if (codePoint <= 0xFFFF) {
                return length + 3;
            }
            return length + 4;
        }, 0);
    }

    window.RoadScannerCredentialPolicy = Object.freeze({
        isValidPassword: isValidPassword,
        passwordMessage: "비밀번호는 공백 없이 문자, 숫자, 특수문자를 각각 포함한 8~20자이며 UTF-8 기준 72바이트 이하여야 합니다."
    });
})(window);
