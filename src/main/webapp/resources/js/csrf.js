(function (window, document) {
    "use strict";

    var tokenElement = document.querySelector('meta[name="csrf-token"]');
    var headerElement = document.querySelector('meta[name="csrf-header"]');
    var parameterElement = document.querySelector('meta[name="csrf-parameter"]');
    var contextElement = document.querySelector('meta[name="application-context"]');

    if (!tokenElement || !headerElement || !parameterElement) {
        return;
    }

    var token = tokenElement.getAttribute("content");
    var headerName = headerElement.getAttribute("content");
    var parameterName = parameterElement.getAttribute("content");
    var applicationContext = contextElement ? contextElement.getAttribute("content") : "";
    var unsafeMethods = { POST: true, PUT: true, PATCH: true, DELETE: true };

    function isSameOrigin(url) {
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (ignored) {
            return false;
        }
    }

    window.roadscannerPost = function (url) {
        if (!isSameOrigin(url)) {
            throw new Error("Cross-origin form submission is not allowed");
        }

        var form = document.createElement("form");
        form.method = "POST";
        form.action = url;

        var hiddenToken = document.createElement("input");
        hiddenToken.type = "hidden";
        hiddenToken.name = parameterName;
        hiddenToken.value = token;
        form.appendChild(hiddenToken);

        document.body.appendChild(form);
        form.submit();
    };

    window.roadscannerLogout = function () {
        window.roadscannerPost(applicationContext + "/logout");
    };

    var originalOpen = window.XMLHttpRequest.prototype.open;
    var originalSend = window.XMLHttpRequest.prototype.send;

    window.XMLHttpRequest.prototype.open = function (method, url) {
        this.__roadscannerCsrfRequired = Boolean(
            unsafeMethods[String(method).toUpperCase()] && isSameOrigin(url)
        );
        return originalOpen.apply(this, arguments);
    };

    window.XMLHttpRequest.prototype.send = function () {
        if (this.__roadscannerCsrfRequired) {
            this.setRequestHeader(headerName, token);
        }
        return originalSend.apply(this, arguments);
    };

    if (window.fetch) {
        var originalFetch = window.fetch;
        window.fetch = function (input, options) {
            var requestOptions = options || {};
            var method = String(requestOptions.method || (input && input.method) || "GET").toUpperCase();
            var url = typeof input === "string"
                ? input
                : input && (typeof input.url === "string" ? input.url : input.href);

            if (url && unsafeMethods[method] && isSameOrigin(url)) {
                requestOptions = Object.assign({}, requestOptions);
                requestOptions.headers = new Headers(requestOptions.headers || (input && input.headers) || {});
                if (!requestOptions.headers.has(headerName)) {
                    requestOptions.headers.set(headerName, token);
                }
            }

            return originalFetch.call(window, input, requestOptions);
        };
    }

    document.addEventListener("submit", function (event) {
        var form = event.target;
        var method = String(form.method || "GET").toUpperCase();
        if (!unsafeMethods[method] || !isSameOrigin(form.action || window.location.href)) {
            return;
        }

        var hiddenToken = form.querySelector('input[name="' + parameterName + '"]');
        if (!hiddenToken) {
            hiddenToken = document.createElement("input");
            hiddenToken.type = "hidden";
            hiddenToken.name = parameterName;
            form.appendChild(hiddenToken);
        }
        hiddenToken.value = token;
    }, true);
})(window, document);
