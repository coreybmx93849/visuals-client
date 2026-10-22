package com.visualsclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config saved to config/visualsclient.json */
public final class VisualsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("visualsclient.json");
    public static VisualsConfig I = new VisualsConfig();

    // HUD
    public boolean armorHud = true;
    public boolean showDurability = true;
    public boolean durabilityAsPercent = false;
    public boolean armorHudRight = false;
    public int armorHudY = 50;          // % of screen height
    public boolean inventoryHud = false;

    // Optimizations
    public boolean minimalParticles = false;
    public boolean tweakEntityDistance = false;
    public int entityDistancePercent = 75; // 50-100

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                VisualsConfig c = GSON.fromJson(Files.readString(FILE), VisualsConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception e) {
            System.err.println("[VisualsClient] Could not read config, using defaults: " + e);
        }
        save();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(I));
        } catch (Exception e) {
            System.err.println("[VisualsClient] Could not save config: " + e);
        }
    }
}
