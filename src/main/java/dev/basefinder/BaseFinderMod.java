package dev.basefinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public final class BaseFinderMod implements ClientModInitializer {
    private static KeyMapping K_TOGGLE, K_FREECAM, K_SNIPER;

    @Override
    public void onInitializeClient() {
        Config.load();
        PriceBook.load();
        Commands.register();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("basefinder", "main"));
        K_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.basefinder.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));
        K_FREECAM = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.basefinder.freecam", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, cat));
        K_SNIPER = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.basefinder.sniper", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, cat));

        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> BaseManager.onChunkLoad(chunk));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (K_TOGGLE.consumeClick()) {
                Config.I.enabled = !Config.I.enabled; Config.save();
                if (mc.player != null) mc.player.displayClientMessage(Component.literal("Base Finder " + (Config.I.enabled ? "ON" : "OFF")), true);
            }
            while (K_FREECAM.consumeClick()) Freecam.toggle(mc);
            while (K_SNIPER.consumeClick()) AuctionSniper.toggle(mc);
            if (Config.I.enabled) BaseManager.tick(mc);
            Freecam.tick(mc);
            AuctionSniper.tick(mc);
            LootLog.sample(mc);
        });

        // remember which block we opened (so chest contents can be attached to a base)
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (Freecam.active && !Freecam.bypass) return InteractionResult.FAIL;
            if (level.isClientSide()) { LootLog.last = hit.getBlockPos(); LootLog.lastTick = level.getGameTime(); }
            return InteractionResult.PASS;
        });
        // while in freecam the character must not do anything
        UseItemCallback.EVENT.register((p, l, h) -> Freecam.active ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackBlockCallback.EVENT.register((p, l, h, pos, dir) -> Freecam.active ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((p, l, h, e, hit) -> Freecam.active ? InteractionResult.FAIL : InteractionResult.PASS);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("basefinder", "overlay"), Overlay::render);
    }
}
