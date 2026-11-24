package com.vetements.mod;

import java.util.Locale;

public enum ChatStyle {
    PLAIN("plain", "обычный"),
    SMALLCAPS("smallcaps", "маленькие заглавные (только латиница)"),
    FULLWIDTH("fullwidth", "широкий (латиница и цифры)"),
    BOLD("bold", "жирный (латиница и цифры)");

    private static final String SMALL =
            "\u1D00\u0299\u1D04\u1D05\u1D07\uA730\u0262\u029C\u026A\u1D0A\u1D0B\u029F\u1D0D"
            + "\u0274\u1D0F\u1D18\u01EB\u0280\uA731\u1D1B\u1D1C\u1D20\u1D21x\u028F\u1D22";

    public final String id;
    public final String description;

    ChatStyle(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public static ChatStyle fromId(String id) {
        if (id != null) {
            String low = id.toLowerCase(Locale.ROOT);
            for (ChatStyle s : values()) {
                if (s.id.equals(low)) {
                    return s;
                }
            }
        }
        return PLAIN;
    }

    public static boolean exists(String id) {
        if (id == null) return false;
        String low = id.toLowerCase(Locale.ROOT);
        for (ChatStyle s : values()) {
            if (s.id.equals(low)) return true;
        }
        return false;
    }

    public String apply(String text) {
        switch (this) {
            case SMALLCAPS:
                return smallCaps(text);
            case FULLWIDTH:
                return fullWidth(text);
            case BOLD:
                return bold(text);
            default:
                return text;
        }
    }

    private static String smallCaps(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char l = Character.toLowerCase(c);
            if (l >= 'a' && l <= 'z') {
                sb.append(SMALL.charAt(l - 'a'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String fullWidth(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x21 && c <= 0x7E) {
                sb.append((char) (c + 0xFEE0));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String bold(String text) {
        StringBuilder sb = new StringBuilder(text.length() * 2);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                sb.appendCodePoint(0x1D400 + (c - 'A'));
            } else if (c >= 'a' && c <= 'z') {
                sb.appendCodePoint(0x1D41A + (c - 'a'));
            } else if (c >= '0' && c <= '9') {
                sb.appendCodePoint(0x1D7CE + (c - '0'));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
