package com.onxon.auction;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class OnxonAuctionMod implements ClientModInitializer {
    public static final String MOD_ID = "onxons_auction_mod";

    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        AuctionConfig.load();
        AuctionHistory.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.onxons_auction_mod.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.onxons_auction_mod"
        ));

        // Payments arrive as "Name paid you $50M" in chat (and on the action bar).
        ClientReceiveMessageEvents.GAME.register(AuctionManager::onMessage);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AuctionManager.tick();
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AuctionScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register(AuctionHud::render);
    }
}
