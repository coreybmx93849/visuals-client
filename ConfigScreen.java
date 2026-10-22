package com.visualsclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Medium centered config panel with animated open, tab hover, toggle and slider easing.
 * Mouse clicks are read by polling GLFW so this does not depend on the Screen input signatures.
 */
public class ConfigScreen extends Screen {
    private static final int W = 340, H = 220;
    private static final int ACCENT = 0x6C5CE7, PANEL = 0x14161C, ROW = 0x1E212B, TEXT = 0xE8E8F0, MUTED = 0x8A8FA3;

    private abstract static class Row { String label; float hover; abstract int height(); }
    private static final class Toggle extends Row {
        final BooleanSupplier get; final Consumer<Boolean> set; float anim = -1;
        Toggle(String l, BooleanSupplier g, Consumer<Boolean> s) { label = l; get = g; set = s; }
        int height() { return 22; }
    }
    private static final class Slider extends Row {
        final int min, max; final IntSupplier get; final IntConsumer set; float anim = -1;
        Slider(String l, int mn, int mx, IntSupplier g, IntConsumer s) { label = l; min = mn; max = mx; get = g; set = s; }
        int height() { return 26; }
    }

    private final String[] tabNames = { "HUD", "Optimizations" };
    private final List<List<Row>> tabs = new ArrayList<>();
    private final float[] tabHover = new float[2];
    private int tab = 0;
    private float tabIndicator = 0;
    private float open = 0;
    private long last = System.nanoTime();
    private boolean wasDown = false;
    private Slider dragging = null;

    public ConfigScreen() {
        super(Text.literal("Visuals Client"));
        VisualsConfig c = VisualsConfig.I;
        List<Row> hud = new ArrayList<>();
        hud.add(new Toggle("Armor HUD", () -> c.armorHud, v -> c.armorHud = v));
        hud.add(new Toggle("Show durability", () -> c.showDurability, v -> c.showDurability = v));
        hud.add(new Toggle("Durability as percent", () -> c.durabilityAsPercent, v -> c.durabilityAsPercent = v));
        hud.add(new Toggle("Armor HUD on right side", () -> c.armorHudRight, v -> c.armorHudRight = v));
        hud.add(new Slider("Armor HUD height %", 5, 95, () -> c.armorHudY, v -> c.armorHudY = v));
        hud.add(new Toggle("Inventory display", () -> c.inventoryHud, v -> c.inventoryHud = v));
        List<Row> opt = new ArrayList<>();
        opt.add(new Toggle("Minimal particles", () -> c.minimalParticles, v -> { c.minimalParticles = v; reapply(); }));
        opt.add(new Toggle("Reduce entity distance", () -> c.tweakEntityDistance, v -> { c.tweakEntityDistance = v; reapply(); }));
        opt.add(new Slider("Entity distance %", 50, 100, () -> c.entityDistancePercent, v -> { c.entityDistancePercent = v; reapply(); }));
        tabs.add(hud);
        tabs.add(opt);
    }

    private static void reapply() { VisualsClient.applyOptimizations(MinecraftClient.getInstance()); }

    @Override public boolean shouldPause() { return false; }
    @Override public void removed() { VisualsConfig.save(); }

    private static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, (int) (a * 255)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }
    private static int lerpRgb(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
    private static float ease(float cur, float target, float dt, float speed) {
        return cur + (target - cur) * Math.min(1f, dt * speed);
    }
    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    /** Rounded-ish rectangle (corner pixels cut) */
    private static void rrect(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x + 2, y, x + w - 2, y + h, color);
        c.fill(x, y + 2, x + w, y + h - 2, color);
        c.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
    }
    private void text(DrawContext c, String s, int x, int y, int rgb, float a) {
        if (a < 0.06f) return;
        c.drawText(textRenderer, s, x, y, argb(a, rgb), false);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        open = ease(open, 1f, dt, 9f);
        float a = open;

        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean clicked = down && !wasDown;
        if (!down) dragging = null;
        wasDown = down;

        ctx.fill(0, 0, width, height, argb(0.45f * a, 0x000000));

        int x = (width - W) / 2;
        int y = (height - H) / 2 + (int) ((1 - a) * 14);
        rrect(ctx, x - 1, y - 1, W + 2, H + 2, argb(a * 0.6f, ACCENT));
        rrect(ctx, x, y, W, H, argb(a, PANEL));

        text(ctx, "Visuals Client", x + 12, y + 10, TEXT, a);
        text(ctx, "Right Shift to open", x + W - 100, y + 10, MUTED, a);

        // Tabs
        int tx = x + 12, ty = y + 28;
        int[] tabX = new int[2], tabW = new int[2];
        for (int i = 0; i < tabNames.length; i++) {
            int tw = textRenderer.getWidth(tabNames[i]) + 16;
            tabX[i] = tx; tabW[i] = tw;
            boolean over = in(mx, my, tx, ty, tw, 16);
            tabHover[i] = ease(tabHover[i], over ? 1 : 0, dt, 14f);
            rrect(ctx, tx, ty, tw, 16, argb(a * (0.0f + 0.5f * tabHover[i]), ROW));
            text(ctx, tabNames[i], tx + 8, ty + 4, i == tab ? TEXT : lerpRgb(MUTED, TEXT, tabHover[i]), a);
            if (clicked && over) tab = i;
            tx += tw + 4;
        }
        tabIndicator = ease(tabIndicator, tabX[tab], dt, 14f);
        ctx.fill((int) tabIndicator + 4, ty + 17, (int) tabIndicator + tabW[tab] - 4, ty + 19, argb(a, ACCENT));

        // Rows
        int ry = y + 54;
        int rx = x + 12, rw = W - 24;
        for (Row row : tabs.get(tab)) {
            int rh = row.height();
            boolean over = in(mx, my, rx, ry, rw, rh - 2);
            row.hover = ease(row.hover, over ? 1 : 0, dt, 14f);
            rrect(ctx, rx, ry, rw, rh - 2, argb(a, lerpRgb(ROW, 0x2A2E3C, row.hover)));
            text(ctx, row.label, rx + 8, ry + (row instanceof Slider ? 4 : 7), TEXT, a);

            if (row instanceof Toggle t) {
                boolean v = t.get.getAsBoolean();
                if (t.anim < 0) t.anim = v ? 1 : 0;
                t.anim = ease(t.anim, v ? 1 : 0, dt, 16f);
                int pw = 28, ph = 12, px = rx + rw - pw - 8, py = ry + 4;
                rrect(ctx, px, py, pw, ph, argb(a, lerpRgb(0x3A3E50, ACCENT, t.anim)));
                int kx = px + 2 + (int) ((pw - ph) * t.anim);
                rrect(ctx, kx, py + 2, ph - 4, ph - 4, argb(a, 0xFFFFFF));
                if (clicked && over) t.set.accept(!v);
            } else if (row instanceof Slider s) {
                int v = s.get.getAsInt();
                float frac = (v - s.min) / (float) (s.max - s.min);
                if (s.anim < 0) s.anim = frac;
                int bx = rx + 8, bw = rw - 16, by = ry + 16;
                if (clicked && in(mx, my, bx - 2, by - 4, bw + 4, 10)) dragging = s;
                if (dragging == s) {
                    float f = Math.max(0f, Math.min(1f, (mx - bx) / (float) bw));
                    int nv = s.min + Math.round(f * (s.max - s.min));
                    if (nv != v) s.set.accept(nv);
                    v = nv; frac = f;
                }
                s.anim = ease(s.anim, frac, dt, 20f);
                ctx.fill(bx, by, bx + bw, by + 3, argb(a, 0x3A3E50));
                ctx.fill(bx, by, bx + (int) (bw * s.anim), by + 3, argb(a, ACCENT));
                rrect(ctx, bx + (int) (bw * s.anim) - 3, by - 2, 6, 7, argb(a, 0xFFFFFF));
                String val = String.valueOf(v);
                text(ctx, val, rx + rw - 8 - textRenderer.getWidth(val), ry + 4, MUTED, a);
            }
            ry += rh;
        }
    }
}
