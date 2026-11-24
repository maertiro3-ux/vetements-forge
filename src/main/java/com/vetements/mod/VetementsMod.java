package com.vetements.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("vetements")
public class VetementsMod {
    public static KeyBinding fullbrightKey;
    public static KeyBinding freelookKey;

    public VetementsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);
        Runtime.getRuntime().addShutdownHook(new Thread(Fullbright::restore));
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        Config.load();

        fullbrightKey = new KeyBinding(
                "key.vetements.fullbright",
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "key.categories.vetements"
        );
        freelookKey = new KeyBinding(
                "key.vetements.freelook",
                InputMappings.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.vetements"
        );

        ClientRegistry.registerKeyBinding(fullbrightKey);
        ClientRegistry.registerKeyBinding(freelookKey);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        while (fullbrightKey != null && fullbrightKey.consumeClick()) {
            Fullbright.toggle(mc);
        }
        Fullbright.tick(mc);
        FreeLook.tick(mc, freelookKey != null && freelookKey.isDown());
        ChatModule.tick(mc);
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (FreeLook.isActive()) {
            event.setYaw(FreeLook.yaw());
            event.setPitch(FreeLook.pitch());
        }
    }

    @SubscribeEvent
    public void onOutgoingChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (ChatModule.handleCommand(msg)) {
            event.setCanceled(true);
            return;
        }
        String formatted = ChatModule.onOutgoing(msg);
        if (formatted != null && !formatted.equals(msg)) {
            event.setMessage(formatted);
        }
    }

    @SubscribeEvent
    public void onIncomingChat(ClientChatReceivedEvent event) {
        if (event.getMessage() != null) {
            ChatModule.onIncoming(event.getMessage().getString());
        }
    }
}
