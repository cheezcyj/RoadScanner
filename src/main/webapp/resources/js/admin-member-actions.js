(function (window, document) {
    "use strict";

    function selectedAccountIds() {
        return Array.prototype.slice.call(
            document.querySelectorAll('input[name="delcheckbox"]:checked')
        ).map(function (checkbox) {
            return checkbox.value;
        });
    }

    function updateMessage(message, isError) {
        var element = document.getElementById("accountActionMessage");
        if (!element) {
            return;
        }
        element.textContent = message;
        element.classList.toggle("text-danger", Boolean(isError));
    }

    function submitAccountAction(endpoint, id) {
        return window.fetch(endpoint, {
            method: "POST",
            credentials: "same-origin",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
            },
            body: new URLSearchParams({ id: id }).toString()
        }).then(function (response) {
            if (!response.ok) {
                throw new Error("HTTP " + response.status);
            }
            return response.json();
        }).then(function (result) {
            if (result.msgId !== "10") {
                throw new Error("Account action was rejected");
            }
        });
    }

    function refreshRelatedFrame(frameId) {
        if (!frameId || window.parent === window) {
            return;
        }
        try {
            var frame = window.parent.document.getElementById(frameId);
            if (frame && frame.contentWindow) {
                frame.contentWindow.location.reload();
            }
        } catch (ignored) {
            // The lists still refresh themselves when embedded outside the administrator page.
        }
    }

    document.addEventListener("DOMContentLoaded", function () {
        var button = document.querySelector("[data-account-action]");
        if (!button || !window.fetch) {
            return;
        }

        button.addEventListener("click", function () {
            var ids = selectedAccountIds();
            if (ids.length === 0) {
                updateMessage("처리할 계정을 선택해주세요.", true);
                return;
            }

            button.disabled = true;
            updateMessage("선택한 계정을 처리하고 있습니다.", false);

            Promise.all(ids.map(function (id) {
                return submitAccountAction(button.dataset.endpoint, id);
            })).then(function () {
                refreshRelatedFrame(button.dataset.refreshFrame);
                window.location.reload();
            }).catch(function () {
                button.disabled = false;
                updateMessage("요청을 완료하지 못했습니다. 잠시 후 다시 시도해주세요.", true);
            });
        });
    });
})(window, document);
