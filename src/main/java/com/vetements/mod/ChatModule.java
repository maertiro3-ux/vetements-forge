package com.vetements.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class ChatModule {
    private static final int MAX_CHAT_LENGTH = 256;
    private static final int LOGIN_WINDOW_TICKS = 20 * 60;
    private static final String[] MATH_KEYWORDS = {
            "реши", "пример", "сколько", "вычисл", "посчита", "решить",
            "solve", "calc", "math", "answer", "=?", "= ?"
    };

    private static final List<Pending> PENDING = new ArrayList<>();
    private static ClientPlayerEntity lastPlayer;
    private static boolean loginHandled;
    private static int joinTicks;
    private static String lastExpression = "";
    private static long lastSolveTime;

    private ChatModule() {
    }

    private static final class Pending {
        int ticks;
        final String text;

        Pending(int ticks, String text) {
            this.ticks = ticks;
            this.text = text;
        }
    }

    public static String onOutgoing(String message) {
        Config cfg = Config.get();
        if (message == null || !cfg.chatEnabled) {
            return message;
        }
        if (message.startsWith("/") || message.startsWith(".")) {
            return message;
        }
        String body = ChatStyle.fromId(cfg.style).apply(message);
        StringBuilder sb = new StringBuilder();
        if (!cfg.prefix.isEmpty()) {
            sb.append(cfg.prefix).append(' ');
        }
        sb.append(body);
        if (!cfg.suffix.isEmpty()) {
            sb.append(' ').append(cfg.suffix);
        }
        String result = sb.toString();
        return result.length() > MAX_CHAT_LENGTH ? message : result;
    }

    public static boolean handleCommand(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (!lower.equals(".vet") && !lower.startsWith(".vet ")) {
            return false;
        }
        Config cfg = Config.get();
        String rest = message.length() > 4 ? message.substring(4).trim() : "";
        String[] parts = rest.split("\\s+", 2);
        String sub = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (sub) {
            case "chat":
                cfg.chatEnabled = parseOnOff(arg, cfg.chatEnabled);
                info("Оформление чата: " + onOff(cfg.chatEnabled));
                break;
            case "prefix":
                cfg.prefix = isOff(arg) ? "" : arg;
                info("Префикс: " + (cfg.prefix.isEmpty() ? "выключен" : cfg.prefix));
                break;
            case "suffix":
                cfg.suffix = isOff(arg) ? "" : arg;
                info("Суффикс: " + (cfg.suffix.isEmpty() ? "выключен" : cfg.suffix));
                break;
            case "style":
                if (ChatStyle.exists(arg)) {
                    cfg.style = arg.toLowerCase(Locale.ROOT);
                    info("Стиль: " + cfg.style);
                } else {
                    info("\u00a7cТакого стиля нет. Доступные:");
                    for (ChatStyle s : ChatStyle.values()) {
                        info("\u00a77 " + s.id + " - " + s.description);
                    }
                }
                break;
            case "math":
                cfg.mathSolver = parseOnOff(arg, cfg.mathSolver);
                info("Авторешение примеров: " + onOff(cfg.mathSolver));
                break;
            case "authme":
                cfg.authmePassword = isOff(arg) ? "" : arg;
                info("Автовход AuthMe: " + (cfg.authmePassword.isEmpty() ? "выключен" : "включён"));
                break;
            case "status":
                info("\u00a76Vetements: \u00a7fчат " + onOff(cfg.chatEnabled)
                        + "\u00a7f, стиль " + cfg.style
                        + ", префикс [" + cfg.prefix + "], суффикс [" + cfg.suffix + "]"
                        + ", примеры " + onOff(cfg.mathSolver)
                        + ", AuthMe " + (cfg.authmePassword.isEmpty() ? "выкл" : "вкл"));
                break;
            default:
                help();
                return true;
        }
        Config.save();
        return true;
    }

    private static void help() {
        info("\u00a76Vetements - команды (пишутся в чат):");
        info("\u00a7e.vet chat on/off \u00a77- оформление чата");
        info("\u00a7e.vet prefix <текст> \u00a77(или off) - префикс перед сообщением");
        info("\u00a7e.vet suffix <текст> \u00a77(или off) - суффикс после сообщения");
        info("\u00a7e.vet style <plain|smallcaps|fullwidth|bold> \u00a77- стиль текста");
        info("\u00a7e.vet math on/off \u00a77- авторешение примеров");
        info("\u00a7e.vet authme <пароль> \u00a77(или off) - автовход");
        info("\u00a7e.vet status \u00a77- текущие настройки");
    }

    private static boolean isOff(String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        return a.isEmpty() || a.equals("off") || a.equals("выкл");
    }

    private static boolean parseOnOff(String arg, boolean current) {
        String a = arg.toLowerCase(Locale.ROOT);
        if (a.equals("on") || a.equals("вкл") || a.equals("1")) return true;
        if (a.equals("off") || a.equals("выкл") || a.equals("0")) return false;
        return !current;
    }

    private static String onOff(boolean value) {
        return value ? "\u00a7aвкл" : "\u00a7cвыкл";
    }

    private static void info(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(new StringTextComponent(text));
        }
    }

    public static void onIncoming(String raw) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || raw == null) {
            return;
        }
        Config cfg = Config.get();
        String stripped = TextFormatting.stripFormatting(raw);
        String text = stripped == null ? raw : stripped;
        String lower = text.toLowerCase(Locale.ROOT);

        if (!cfg.authmePassword.isEmpty() && !loginHandled
                && joinTicks < LOGIN_WINDOW_TICKS && lower.contains("/login")) {
            loginHandled = true;
            queue(20, "/login " + cfg.authmePassword);
            return;
        }

        if (cfg.mathSolver && containsKeyword(lower)) {
            String answer = MathSolver.solve(text);
            if (answer != null) {
                long now = System.currentTimeMillis();
                if (answer.equals(lastExpression) && now - lastSolveTime < 5000) {
                    return;
                }
                lastExpression = answer;
                lastSolveTime = now;
                queue(Math.max(1, cfg.mathDelayTicks), answer);
            }
        }
    }

    private static boolean containsKeyword(String lower) {
        for (String k : MATH_KEYWORDS) {
            if (lower.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static void queue(int ticks, String text) {
        PENDING.add(new Pending(ticks, text));
    }

    public static void tick(Minecraft mc) {
        if (mc.player != lastPlayer) {
            lastPlayer = mc.player;
            loginHandled = false;
            joinTicks = 0;
            PENDING.clear();
        }
        if (mc.player == null) {
            return;
        }
        joinTicks++;
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            p.ticks--;
            if (p.ticks <= 0) {
                it.remove();
                sendRaw(mc, p.text);
            }
        }
    }

    private static void sendRaw(Minecraft mc, String text) {
        ClientPlayNetHandler handler = mc.getConnection();
        if (handler != null) {
            handler.send(new CChatMessagePacket(text));
        }
    }
}
