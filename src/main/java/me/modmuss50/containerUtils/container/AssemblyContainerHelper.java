package me.modmuss50.containerUtils.container;

import io.netty.buffer.Unpooled;
import me.modmuss50.containerUtils.ContainerUtils;
import me.modmuss50.containerUtils.network.ObjectBufUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.networking.CustomPayloadPacketRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.ContainerGui;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.packet.CustomPayloadClientPacket;
import net.minecraft.container.Container;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;


import java.util.HashMap;
import java.util.Map;

public class AssemblyContainerHelper {

	public static final Identifier OPEN_CONTAINER = new Identifier(ContainerUtils.MOD_ID, "open_container");
	private static final Map<Identifier, Pair<ContainerSupplier<Container>, ContainerSupplier<ContainerGui>>> containerMap = new HashMap<>();

	public static final ContainerSupplier<Container> DEFAULT_CONTAINER_SUPPLIER = (playerEntity, pos) -> {
		BlockEntity blockEntity = playerEntity.world.getBlockEntity(pos);
		if (blockEntity instanceof FabricContainerProvider) {
			return ((FabricContainerProvider) blockEntity).createContainer(playerEntity.inventory, playerEntity);
		}
		return null;
	};

	public static void init() {
		CustomPayloadPacketRegistry.CLIENT.register(OPEN_CONTAINER, (packetContext, packetByteBuf) -> {
			Identifier identifier = new Identifier(packetByteBuf.readString(64));
			BlockPos pos = (BlockPos) ObjectBufUtils.readObject(packetByteBuf);
			openGui(identifier, (ClientPlayerEntity) packetContext.getPlayer(), pos);
		});

//		addContainerMapping(new Identifier("assembly", "grinder"), DEFAULT_CONTAINER_SUPPLIER, (playerEntity, pos) -> {
//			GrinderBlockEntity blockEntity = (GrinderBlockEntity) playerEntity.world.getBlockEntity(pos);
//			return new GrinderGui(playerEntity, blockEntity);
//		});
	}

	public static void openGui(FabricContainerProvider containerProvider, BlockPos pos, ServerPlayerEntity playerEntity) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		Identifier identifier = containerProvider.getContainerIdentifier();
		if (!containerMap.containsKey(identifier)) {
			throw new RuntimeException("No gui found for " + identifier);
		}
		buf.writeString(identifier.toString());
		ObjectBufUtils.writeObject(pos, buf);
		playerEntity.networkHandler.sendPacket(new CustomPayloadClientPacket(OPEN_CONTAINER, buf));

		playerEntity.container = containerMap.get(identifier).getKey().get(playerEntity, pos);
		playerEntity.container.addListener(playerEntity);

	}

	public static void addContainerMapping(Identifier identifier, ContainerSupplier<Container> containerSupplier, ContainerSupplier<ContainerGui> guiSupplier) {
		Validate.isTrue(!containerMap.containsKey(identifier));
		containerMap.put(identifier, Pair.of(containerSupplier, guiSupplier));
	}

	private static void openGui(Identifier container, ClientPlayerEntity playerEntity, BlockPos pos) {
		Pair<ContainerSupplier<Container>, ContainerSupplier<ContainerGui>> supplierPair = containerMap.get(container);
		if (supplierPair == null) {
			throw new RuntimeException("No container found for " + container);
		}
		MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().openGui(supplierPair.getRight().get(playerEntity, pos)));

	}


	public interface ContainerSupplier<T> {
		T get(PlayerEntity playerEntity, BlockPos pos);
	}

}
