package dev.trickle.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.trickle.Trickle;
import dev.trickle.item.LootBoxItem;
import dev.trickle.net.BalancePayload;
import dev.trickle.net.OpenLootBoxPayload;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.CommonComponents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Client side: shows the coin balance in the corner and adds a key to buy a loot box. */
public class TrickleClient implements ClientModInitializer {
    /** Last balance the server sent, or null if this server doesn't run Trickle. */
    private static BalancePayload latest;

    private static KeyMapping buyKey;
    private static KeyMapping toggleHudKey;
    private static boolean hudVisible = true;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BalancePayload.TYPE,
                (payload, context) -> context.client().execute(() -> latest = payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> latest = null);

        LootBoxItem.clientConfirmHook = (item, stack, hand) -> {
            Minecraft client = Minecraft.getInstance();
            int count = stack.getCount();
            Component name = stack.getHoverName();
            // open the dialog after this tick's input handling finishes
            client.execute(() -> client.gui.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) ClientPlayNetworking.send(new OpenLootBoxPayload(hand));
                        client.gui.setScreen(null);
                    },
                    Component.translatable("trickle.confirm.title", name),
                    Component.translatable("trickle.confirm.message", count),
                    Component.translatable("trickle.confirm.open"),
                    CommonComponents.GUI_CANCEL)));
        };

        KeyMapping.Category category = KeyMapping.Category.register(Trickle.id("main"));
        buyKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.trickle.buy", InputConstants.KEY_B, category));
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.trickle.toggle_hud", InputConstants.UNKNOWN.getValue(), category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (buyKey.consumeClick()) {
                // The server does the purchase, so this can't be cheated from the client.
                if (latest != null && client.getConnection() != null) {
                    client.getConnection().sendCommand("trickle buy");
                }
            }
            while (toggleHudKey.consumeClick()) hudVisible = !hudVisible;
        });

        HudElementRegistry.addLast(Trickle.id("balance"), (graphics, deltaTracker) -> {
            Minecraft client = Minecraft.getInstance();
            if (latest == null || !hudVisible || client.getDebugOverlay().showDebugScreen()) return;
            Font font = client.font;
            int x = 6, y = 6;
            graphics.text(font, Component.translatable("trickle.hud.balance", latest.balance()), x, y, 0xFFFFD24A, true);
            // progress bar toward the next coin
            int barW = 60, barY = y + font.lineHeight + 2;
            graphics.fill(x, barY, x + barW, barY + 2, 0x80000000);
            graphics.fill(x, barY, x + Math.round(barW * latest.progress()), barY + 2, 0xFFFFD24A);
        });
    }
}
