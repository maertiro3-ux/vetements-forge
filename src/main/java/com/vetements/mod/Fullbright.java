package com.vetements.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

public final class Fullbright {
    private static final double BRIGHT = 16.0D;
    private static boolean enabled;
    private static double savedGamma = 1.0D;

    private Fullbright() {
    }

    public static void toggle(Minecraft mc) {
        enabled = !enabled;
        if (enabled) {
            savedGamma = mc.options.gamma;
            mc.options.gamma = BRIGHT;
        } else {
            mc.options.gamma = savedGamma;
        }
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    new StringTextComponent("Fullbright: " + (enabled ? "\u00a7aвкл" : "\u00a7cвыкл")), true);
        }
    }

    public static void tick(Minecraft mc) {
        if (enabled && mc.options.gamma < BRIGHT) {
            mc.options.gamma = BRIGHT;
        }
    }

    public static void restore() {
        Minecraft mc = Minecraft.getInstance();
        if (enabled && mc != null && mc.options != null) {
            mc.options.gamma = savedGamma;
            enabled = false;
        }
    }
}
