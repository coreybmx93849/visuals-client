package com.visualsclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    public static final String ID = "visualsclient";
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        VisualsConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.visualsclient.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                mc.setScreen(new ConfigScreen());
            }
        });

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                Identifier.of(ID, "hud"),
                (ctx, tickCounter) -> Hud.render(ctx));

        ClientLifecycleEvents.CLIENT_STARTED.register(VisualsClient::applyOptimizations);
    }

    /** Applies the optimization toggles to vanilla video settings. */
    public static void applyOptimizations(MinecraftClient mc) {
        VisualsConfig c = VisualsConfig.I;
        if (mc.options == null) return;
        mc.options.getParticles().setValue(c.minimalParticles ? ParticlesMode.MINIMAL : ParticlesMode.ALL);
        if (c.tweakEntityDistance) {
            mc.options.getEntityDistanceScaling().setValue(c.entityDistancePercent / 100.0);
        }
    }
}
