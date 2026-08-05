(function (window, document) {
    "use strict";

    var EDITOR_SELECTOR = "[data-qna-editor]";
    var DEFAULT_MAX_LENGTH = 10000;
    var EMPTY_EDITOR_HTML = "<p><br></p>";
    var instanceSequence = 0;
    var instances = [];
    var instanceByRoot = new WeakMap();

    var allowedTags = {
        p: true,
        h2: true,
        strong: true,
        em: true,
        u: true,
        ul: true,
        ol: true,
        li: true,
        blockquote: true,
        br: true
    };

    var discardedTags = {
        script: true,
        style: true,
        iframe: true,
        object: true,
        embed: true,
        svg: true,
        math: true,
        form: true,
        input: true,
        button: true,
        textarea: true,
        select: true,
        option: true,
        meta: true,
        link: true,
        base: true,
        img: true,
        video: true,
        audio: true,
        source: true
    };

    var tagAliases = {
        b: "strong",
        i: "em",
        div: "p",
        h1: "h2",
        h3: "h2",
        h4: "h2",
        h5: "h2",
        h6: "h2"
    };

    var allowedCommands = {
        undo: true,
        redo: true,
        formatBlock: true,
        bold: true,
        italic: true,
        underline: true,
        insertUnorderedList: true,
        insertOrderedList: true,
        removeFormat: true
    };

    var statefulCommands = {
        bold: true,
        italic: true,
        underline: true,
        insertUnorderedList: true,
        insertOrderedList: true
    };

    var commandShortcuts = {
        undo: "Control+Z",
        redo: "Control+Y",
        bold: "Control+B",
        italic: "Control+I",
        underline: "Control+U"
    };

    function appendSanitizedNode(node, target) {
        if (node.nodeType === window.Node.TEXT_NODE) {
            target.appendChild(document.createTextNode(node.nodeValue || ""));
            return;
        }

        if (node.nodeType !== window.Node.ELEMENT_NODE) {
            return;
        }

        var sourceTag = node.tagName.toLowerCase();
        if (discardedTags[sourceTag]) {
            return;
        }

        var targetTag = tagAliases[sourceTag] || sourceTag;
        if (allowedTags[targetTag]) {
            var cleanElement = document.createElement(targetTag);
            Array.prototype.forEach.call(node.childNodes, function (child) {
                appendSanitizedNode(child, cleanElement);
            });
            target.appendChild(cleanElement);
            return;
        }

        Array.prototype.forEach.call(node.childNodes, function (child) {
            appendSanitizedNode(child, target);
        });
    }

    function sanitizeHtml(rawHtml) {
        var template = document.createElement("template");
        var cleanContainer = document.createElement("div");
        template.innerHTML = String(rawHtml || "");

        Array.prototype.forEach.call(template.content.childNodes, function (node) {
            appendSanitizedNode(node, cleanContainer);
        });

        return cleanContainer.innerHTML;
    }

    function editorText(surface) {
        return String(surface.textContent || "").replace(/\u00a0/g, " ");
    }

    function meaningfulText(surface) {
        return editorText(surface).replace(/[\s\u200B-\u200D\uFEFF]/g, "");
    }

    function parseMaxLength(source) {
        var configured = source.getAttribute("maxlength") || source.getAttribute("data-maxlength");
        var parsed = Number.parseInt(configured, 10);
        return Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULT_MAX_LENGTH;
    }

    function formattedNumber(value) {
        try {
            return value.toLocaleString("ko-KR");
        } catch (ignored) {
            return String(value);
        }
    }

    function selectionBelongsTo(surface) {
        var selection = window.getSelection();
        return Boolean(selection && selection.rangeCount && selection.anchorNode
            && (selection.anchorNode === surface || surface.contains(selection.anchorNode)));
    }

    function captureSelection(instance) {
        var selection = window.getSelection();
        if (!selectionBelongsTo(instance.surface) || !selection || !selection.rangeCount) {
            return;
        }
        instance.savedRange = selection.getRangeAt(0).cloneRange();
    }

    function restoreSelection(instance) {
        if (!instance.savedRange) {
            return;
        }

        var selection = window.getSelection();
        if (!selection) {
            return;
        }

        selection.removeAllRanges();
        selection.addRange(instance.savedRange);
    }

    function queryCommandState(command) {
        try {
            return document.queryCommandState(command);
        } catch (ignored) {
            return false;
        }
    }

    function currentBlockName() {
        try {
            return String(document.queryCommandValue("formatBlock") || "")
                .replace(/[<>]/g, "")
                .toLowerCase();
        } catch (ignored) {
            return "";
        }
    }

    function updateToolbarState(instance) {
        if (!selectionBelongsTo(instance.surface)) {
            return;
        }

        var blockName = currentBlockName();
        instance.buttons.forEach(function (button) {
            var command = button.getAttribute("data-editor-command");
            var value = String(button.getAttribute("data-editor-value") || "").toLowerCase();

            if (statefulCommands[command]) {
                button.setAttribute("aria-pressed", String(queryCommandState(command)));
            } else if (command === "formatBlock") {
                button.setAttribute("aria-pressed", String(blockName === value));
            }

            if (command === "undo" || command === "redo") {
                try {
                    button.disabled = !document.queryCommandEnabled(command);
                } catch (ignored) {
                    button.disabled = false;
                }
            }
        });
    }

    function updateCharacterCount(instance) {
        var visibleText = editorText(instance.surface).replace(/[\u200B-\u200D\uFEFF]/g, "");
        var length = meaningfulText(instance.surface).length === 0
            ? 0
            : Array.from(visibleText).length;
        var overLimit = length > instance.maxLength;
        instance.count.textContent = formattedNumber(length) + " / "
            + formattedNumber(instance.maxLength) + "자";
        instance.count.classList.toggle("is-over-limit", overLimit);
        instance.surface.classList.toggle("is-over-limit", overLimit);
        instance.surface.setAttribute("aria-invalid", String(overLimit));
        instance.surface.setAttribute("data-empty", String(meaningfulText(instance.surface).length === 0));
        instance.source.setCustomValidity(overLimit ? "내용은 " + instance.maxLength + "자 이하여야 합니다." : "");
    }

    function syncInstance(instance) {
        var cleanHtml = sanitizeHtml(instance.surface.innerHTML);
        instance.source.value = meaningfulText(instance.surface).length ? cleanHtml : "";
        updateCharacterCount(instance);
        return instance.source.value;
    }

    function syncAll() {
        var values = [];
        instances = instances.filter(function (instance) {
            return document.documentElement.contains(instance.root);
        });
        instances.forEach(function (instance) {
            values.push(syncInstance(instance));
        });
        return values;
    }

    function insertPlainText(instance, text) {
        instance.surface.focus();
        restoreSelection(instance);

        var inserted = false;
        try {
            inserted = document.execCommand("insertText", false, String(text || ""));
        } catch (ignored) {
            inserted = false;
        }

        if (!inserted) {
            var selection = window.getSelection();
            if (!selection || !selection.rangeCount) {
                return;
            }

            var range = selection.getRangeAt(0);
            range.deleteContents();
            var fragment = document.createDocumentFragment();
            String(text || "").split(/\r\n?|\n/).forEach(function (line, index) {
                if (index > 0) {
                    fragment.appendChild(document.createElement("br"));
                }
                fragment.appendChild(document.createTextNode(line));
            });
            range.insertNode(fragment);
            range.collapse(false);
            selection.removeAllRanges();
            selection.addRange(range);
        }

        captureSelection(instance);
        syncInstance(instance);
        updateToolbarState(instance);
    }

    function executeTool(instance, button) {
        var command = button.getAttribute("data-editor-command");
        var value = button.getAttribute("data-editor-value");
        if (!allowedCommands[command]) {
            return;
        }
        if (command === "formatBlock" && ["p", "h2", "blockquote"].indexOf(value) === -1) {
            return;
        }

        instance.surface.focus();
        restoreSelection(instance);
        try {
            document.execCommand("defaultParagraphSeparator", false, "p");
            document.execCommand(command, false, value || null);
        } catch (ignored) {
            return;
        }

        captureSelection(instance);
        syncInstance(instance);
        updateToolbarState(instance);
    }

    function configureButton(instance, button) {
        var command = button.getAttribute("data-editor-command");
        if (!allowedCommands[command]) {
            button.disabled = true;
            return;
        }

        if (commandShortcuts[command]) {
            button.setAttribute("aria-keyshortcuts", commandShortcuts[command]);
        }
        if (statefulCommands[command] || command === "formatBlock") {
            button.setAttribute("aria-pressed", "false");
        }
        if (command === "undo" || command === "redo") {
            button.disabled = true;
        }

        button.addEventListener("pointerdown", function (event) {
            event.preventDefault();
        });
        button.addEventListener("click", function () {
            executeTool(instance, button);
        });
    }

    function initEditor(root) {
        if (instanceByRoot.has(root)) {
            return instanceByRoot.get(root);
        }

        var source = root.querySelector(".qna-editor-source");
        var surface = root.querySelector(".qna-editor-surface");
        var count = root.querySelector("[data-editor-count]");
        var toolbar = root.querySelector(".qna-editor-toolbar");
        if (!source || !surface || !count || !toolbar) {
            return null;
        }

        instanceSequence += 1;
        var maxLength = parseMaxLength(source);
        var cleanInitialHtml = sanitizeHtml(source.value);
        surface.innerHTML = cleanInitialHtml || EMPTY_EDITOR_HTML;
        source.value = cleanInitialHtml;
        // The stored value contains formatting markup; only the visible text is length-limited.
        source.removeAttribute("maxlength");
        source.setAttribute("data-maxlength", String(maxLength));
        source.hidden = true;
        source.tabIndex = -1;
        source.setAttribute("aria-hidden", "true");
        surface.setAttribute("spellcheck", "true");

        var countId = count.id || "qna-editor-count-" + instanceSequence;
        count.id = countId;
        count.setAttribute("role", "status");
        count.setAttribute("aria-atomic", "true");
        surface.setAttribute("aria-describedby", countId);

        Array.prototype.forEach.call(root.querySelectorAll(".qna-editor-tool-group"), function (group) {
            group.setAttribute("role", "group");
        });

        var instance = {
            root: root,
            source: source,
            surface: surface,
            count: count,
            toolbar: toolbar,
            maxLength: maxLength,
            buttons: Array.prototype.slice.call(toolbar.querySelectorAll("[data-editor-command]")),
            savedRange: null
        };

        instance.buttons.forEach(function (button) {
            configureButton(instance, button);
        });

        surface.addEventListener("input", function () {
            captureSelection(instance);
            syncInstance(instance);
            updateToolbarState(instance);
        });
        surface.addEventListener("keyup", function () {
            captureSelection(instance);
            updateToolbarState(instance);
        });
        surface.addEventListener("mouseup", function () {
            captureSelection(instance);
            updateToolbarState(instance);
        });
        surface.addEventListener("focus", function () {
            try {
                document.execCommand("defaultParagraphSeparator", false, "p");
            } catch (ignored) {
                // The browser will keep its native paragraph separator.
            }
            captureSelection(instance);
            updateToolbarState(instance);
        });
        surface.addEventListener("paste", function (event) {
            event.preventDefault();
            var clipboard = event.clipboardData || window.clipboardData;
            insertPlainText(instance, clipboard ? clipboard.getData("text/plain") : "");
        });
        surface.addEventListener("drop", function (event) {
            var transfer = event.dataTransfer;
            if (!transfer) {
                return;
            }
            event.preventDefault();
            if (!transfer.files || transfer.files.length === 0) {
                insertPlainText(instance, transfer.getData("text/plain") || "");
            }
        });

        var form = root.closest("form");
        if (form) {
            form.addEventListener("submit", syncAll, true);
            form.addEventListener("reset", function () {
                window.setTimeout(function () {
                    var resetHtml = sanitizeHtml(source.defaultValue);
                    surface.innerHTML = resetHtml || EMPTY_EDITOR_HTML;
                    syncInstance(instance);
                }, 0);
            });
        }

        root.setAttribute("data-editor-initialized", "true");
        instanceByRoot.set(root, instance);
        instances.push(instance);
        syncInstance(instance);
        return instance;
    }

    function initAll(root) {
        var context = root && root.querySelectorAll ? root : document;
        var editorRoots = [];
        if (context.matches && context.matches(EDITOR_SELECTOR)) {
            editorRoots.push(context);
        }
        Array.prototype.push.apply(editorRoots, context.querySelectorAll(EDITOR_SELECTOR));

        return editorRoots.map(function (editorRoot) {
            return initEditor(editorRoot);
        }).filter(Boolean);
    }

    document.addEventListener("selectionchange", function () {
        instances.forEach(function (instance) {
            if (selectionBelongsTo(instance.surface)) {
                captureSelection(instance);
                updateToolbarState(instance);
            }
        });
    });

    var api = {
        initAll: initAll,
        syncAll: syncAll,
        sanitizeHtml: sanitizeHtml,
        getInstance: function (root) {
            return instanceByRoot.get(root) || null;
        }
    };

    window.roadscannerQnaEditors = api;
    window.RoadScannerQnaEditor = api;
    window.qnaEditorSyncAll = syncAll;

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            initAll(document);
        });
    } else {
        initAll(document);
    }
})(window, document);
