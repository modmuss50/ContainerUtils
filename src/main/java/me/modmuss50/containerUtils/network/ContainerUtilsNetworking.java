package me.modmuss50.containerUtils.network;

import io.netty.buffer.Unpooled;
import me.modmuss50.containerUtils.ContainerUtils;
import me.modmuss50.containerUtils.container.builder.IExtendedContainerListener;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.networking.CustomPayloadPacketRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ContainerGui;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.packet.CustomPayloadClientPacket;
import net.minecraft.container.Container;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class ContainerUtilsNetworking{

	public static final Identifier CONTAINER_SYNC = new Identifier(ContainerUtils.MOD_ID, "container_sync");

	public static void init() {
		CustomPayloadPacketRegistry.CLIENT.register(CONTAINER_SYNC, (packetContext, packetByteBuf) -> {
			Gui gui = MinecraftClient.getInstance().currentGui;
			if (gui instanceof ContainerGui) {
				Container container = ((ContainerGui) gui).container;
				if (container instanceof IExtendedContainerListener) {
					((IExtendedContainerListener) container).handleObject(packetByteBuf.readInt(), ObjectBufUtils.readObject(packetByteBuf));
				}
			}
		});
	}

	@Environment(EnvType.SERVER)
	public static void syncContainer(ServerPlayerEntity player, int id, Object value) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeInt(id);
		ObjectBufUtils.writeObject(value, buf);
		player.networkHandler.sendPacket(new CustomPayloadClientPacket(CONTAINER_SYNC, buf));
	}
}
