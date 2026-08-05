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
                || /\s/.test(password)) {
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

    window.RoadScannerCredentialPolicy = Object.freeze({
        isValidPassword: isValidPassword,
        passwordMessage: "비밀번호는 공백 없이 영문, 숫자, 특수문자를 각각 포함한 8~20자여야 합니다."
    });
})(window);
