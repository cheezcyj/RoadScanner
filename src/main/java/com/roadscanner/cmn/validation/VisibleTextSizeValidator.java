package com.roadscanner.cmn.validation;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class VisibleTextSizeValidator implements ConstraintValidator<VisibleTextSize, String> {

    private int max;

    @Override
    public void initialize(VisibleTextSize constraintAnnotation) {
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (value.length() > QuestionContentLimits.MAX_RAW_LENGTH) {
            return false;
        }

        StringBuilder visibleText = new StringBuilder();
        appendTextNodes(Jsoup.parseBodyFragment(value).body(), visibleText);
        int visibleLength = 0;
        for (int offset = 0; offset < visibleText.length();) {
            int codePoint = visibleText.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint == 0x200B || codePoint == 0x200C
                    || codePoint == 0x200D || codePoint == 0xFEFF) {
                continue;
            }
            visibleLength++;
            if (visibleLength > max) {
                return false;
            }
        }
        return true;
    }

    private void appendTextNodes(Node node, StringBuilder target) {
        if (node instanceof TextNode) {
            target.append(((TextNode) node).getWholeText());
            return;
        }
        for (Node child : node.childNodes()) {
            appendTextNodes(child, target);
        }
    }
}
