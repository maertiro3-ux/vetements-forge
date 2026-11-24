package com.vetements.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Logger LOGGER = LogManager.getLogger("Vetements");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config instance = new Config();

    public boolean chatEnabled = true;
    public String prefix = "";
    public String suffix = "";
    public String style = "plain";
    public boolean mathSolver = false;
    public int mathDelayTicks = 20;
    public String authmePassword = "";

    public static Config get() {
        return instance;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("vetements.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Config loaded = GSON.fromJson(r, Config.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not read config, using defaults", e);
            }
        }
        if (instance.prefix == null) instance.prefix = "";
        if (instance.suffix == null) instance.suffix = "";
        if (instance.style == null) instance.style = "plain";
        if (instance.authmePassword == null) instance.authmePassword = "";
        save();
    }

    public static void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
        } catch (IOException e) {
            LOGGER.error("Could not save config", e);
        }
    }
}
