package me.modmuss50.containerUtils.container;

import net.minecraft.container.Container;
import net.minecraft.container.ContainerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Identifier;

public interface FabricContainerProvider extends ContainerProvider {

	@Override
	default String getContainerId() {
		return getContainerIdentifier().toString();
	}

	@Override
	default boolean hasCustomName() {
		return false;
	}

	@Override
	default TextComponent getName() {
		return new TranslatableTextComponent(getContainerIdentifier().toString());
	}

	@Override
	default Container createContainer(PlayerInventory playerInventory, PlayerEntity playerEntity){
		return createContainer(playerEntity);
	}

	Container createContainer(PlayerEntity playerEntity);

	Identifier getContainerIdentifier();
}
