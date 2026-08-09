package com.roadscanner.cmn.validation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/** Keeps only the formatting elements supported by the local Q&A editor. */
public final class RichTextSanitizer {
    private static final Safelist ALLOWED_FORMATTING = Safelist.none()
            .addTags("p", "div", "br", "strong", "b", "em", "i", "u", "s",
                    "h2", "h3", "blockquote", "ul", "ol", "li");

    private RichTextSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        return Jsoup.clean(value, "", ALLOWED_FORMATTING, outputSettings);
    }
}
