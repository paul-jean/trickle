package dev.trickle.client;

import dev.trickle.net.BalancePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Client side: shows the coin balance in the corner and adds a key to buy a loot box. */
public class TrickleClient implements ClientModInitializer {
    /** Last balance the server sent, or null if this server doesn't run Trickle. */
    private static BalancePayload latest;

    private static KeyBinding buyKey;
    private static KeyBinding toggleHudKey;
    private static boolean hudVisible = true;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BalancePayload.ID,
                (payload, context) -> context.client().execute(() -> latest = payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> latest = null);

        buyKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.trickle.buy", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.trickle"));
        toggleHudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.trickle.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.trickle"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (buyKey.wasPressed()) {
                // The server does the purchase, so this can't be cheated from the client.
                if (latest != null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("trickle buy");
                }
            }
            while (toggleHudKey.wasPressed()) hudVisible = !hudVisible;
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (latest == null || !hudVisible || client.options.hudHidden || client.getDebugHud().shouldShowDebugHud()) return;
            TextRenderer font = client.textRenderer;
            int x = 6, y = 6;
            Text line = Text.translatable("trickle.hud.balance", latest.balance());
            ctx.drawTextWithShadow(font, line, x, y, 0xFFD24A);
            // progress bar toward the next coin
            int barW = 60, barY = y + font.fontHeight + 2;
            ctx.fill(x, barY, x + barW, barY + 2, 0x80000000);
            ctx.fill(x, barY, x + Math.round(barW * latest.progress()), barY + 2, 0xFFFFD24A);
        });
    }
}
