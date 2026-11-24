package com.vetements.mod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MathSolver {
    private static final Pattern EXPR = Pattern.compile(
            "(?:\\(?\\s*\\d+(?:[.,]\\d+)?\\s*\\)?\\s*[+\\-\\u2212*/xX\\u00d7\\u00f7]\\s*)+"
            + "\\(?\\s*\\d+(?:[.,]\\d+)?\\s*\\)?");

    private MathSolver() {
    }

    public static String solve(String text) {
        Matcher m = EXPR.matcher(text);
        while (m.find()) {
            String result = evaluate(m.group());
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    static String evaluate(String expr) {
        String s = expr
                .replace('\u2212', '-')
                .replace('\u00d7', '*')
                .replace('\u00f7', '/')
                .replace('x', '*')
                .replace('X', '*')
                .replace(',', '.')
                .replaceAll("\\s+", "");
        try {
            Parser p = new Parser(s);
            double v = p.parseExpression();
            if (!p.atEnd() || Double.isNaN(v) || Double.isInfinite(v)) {
                return null;
            }
            return format(v);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String format(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return Long.toString((long) Math.rint(v));
        }
        return new BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static final class Parser {
        private final String s;
        private int i = 0;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return i == s.length();
        }

        double parseExpression() {
            double v = parseTerm();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '+') {
                    i++;
                    v += parseTerm();
                } else if (c == '-') {
                    i++;
                    v -= parseTerm();
                } else {
                    break;
                }
            }
            return v;
        }

        double parseTerm() {
            double v = parseFactor();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '*') {
                    i++;
                    v *= parseFactor();
                } else if (c == '/') {
                    i++;
                    v /= parseFactor();
                } else {
                    break;
                }
            }
            return v;
        }

        double parseFactor() {
            if (i >= s.length()) {
                throw new IllegalStateException("end");
            }
            char c = s.charAt(i);
            if (c == '-') {
                i++;
                return -parseFactor();
            }
            if (c == '(') {
                i++;
                double v = parseExpression();
                if (i >= s.length() || s.charAt(i) != ')') {
                    throw new IllegalStateException("paren");
                }
                i++;
                return v;
            }
            int start = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
                i++;
            }
            if (start == i) {
                throw new IllegalStateException("number");
            }
            return Double.parseDouble(s.substring(start, i));
        }
    }
}
