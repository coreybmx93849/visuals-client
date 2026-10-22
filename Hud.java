package com.visualsclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class Hud {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private Hud() {}

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null || mc.options.hudHidden) return;
        VisualsConfig c = VisualsConfig.I;
        if (c.armorHud) renderArmor(ctx, mc, player, c);
        if (c.inventoryHud) renderInventory(ctx, mc, player);
    }

    private static void renderArmor(DrawContext ctx, MinecraftClient mc, PlayerEntity p, VisualsConfig c) {
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int rowH = 20;
        int boxW = c.showDurability ? 62 : 24;
        int x = c.armorHudRight ? sw - boxW - 4 : 4;
        int y = (int) (sh * (c.armorHudY / 100.0)) - (rowH * 4) / 2;

        for (int i = 0; i < ARMOR.length; i++) {
            ItemStack s = p.getEquippedStack(ARMOR[i]);
            int ry = y + i * rowH;
            ctx.fill(x, ry, x + boxW, ry + rowH - 2, 0x66000000);
            if (s.isEmpty()) continue;
            ctx.drawItem(s, x + 2, ry);
            if (c.showDurability && s.isDamageable()) {
                int max = s.getMaxDamage();
                int left = max - s.getDamage();
                float frac = left / (float) max;
                String text = c.durabilityAsPercent ? Math.round(frac * 100) + "%" : String.valueOf(left);
                int color = 0xFF000000 | java.awt.Color.HSBtoRGB(frac * 0.33f, 0.8f, 1f);
                ctx.drawText(tr, text, x + 22, ry + 4, color, true);
                // slim bar
                int bw = boxW - 4;
                ctx.fill(x + 2, ry + rowH - 4, x + 2 + bw, ry + rowH - 3, 0x88000000);
                ctx.fill(x + 2, ry + rowH - 4, x + 2 + (int) (bw * frac), ry + rowH - 3, color);
            }
        }
    }

    private static void renderInventory(DrawContext ctx, MinecraftClient mc, PlayerEntity p) {
        int sw = ctx.getScaledWindowWidth();
        int x = sw / 2 + 100, y = 4; // top, right of center
        ctx.fill(x - 2, y - 2, x + 9 * 18 + 2, y + 3 * 18 + 2, 0x66000000);
        for (int i = 0; i < 27; i++) {
            ItemStack s = p.getInventory().getStack(9 + i);
            int ix = x + (i % 9) * 18, iy = y + (i / 9) * 18;
            if (s.isEmpty()) continue;
            ctx.drawItem(s, ix, iy);
            ctx.drawStackOverlay(mc.textRenderer, s, ix, iy);
        }
    }
}
