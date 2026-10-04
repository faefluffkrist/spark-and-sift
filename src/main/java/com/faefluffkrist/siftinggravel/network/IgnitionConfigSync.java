package com.faefluffkrist.siftinggravel.network;

import com.faefluffkrist.siftinggravel.config.SiftingGravelConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** The server owns ignition scope; a client config cannot enable it on its own. */
public final class IgnitionConfigSync {
    private static Boolean clientSetting;
    public record Payload(boolean enabled) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("siftinggravel", "ignition_scope"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.enabled()), buf -> new Payload(buf.readBoolean()));
        @Override public Type<Payload> type() { return TYPE; }
    }
    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(Payload.TYPE, Payload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.player, Payload.TYPE))
                ServerPlayNetworking.send(handler.player, new Payload(SiftingGravelConfig.get().allowGeneralIgnition));
        });
    }
    public static void setClientSetting(Boolean value) { clientSetting = value; }
    public static boolean allowed(Level level) {
        return level != null && level.isClientSide() && clientSetting != null
                ? clientSetting : SiftingGravelConfig.get().allowGeneralIgnition;
    }
    private IgnitionConfigSync() {}
}
