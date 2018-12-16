package me.modmuss50.containerUtils;

import me.modmuss50.containerUtils.container.AssemblyContainerHelper;
import me.modmuss50.containerUtils.network.ContainerUtilsNetworking;
import net.fabricmc.api.ModInitializer;

public class ContainerUtils implements ModInitializer {

	public static final String MOD_ID = "container_utils";

	@Override
	public void onInitialize() {
		AssemblyContainerHelper.init();
		ContainerUtilsNetworking.init();
	}
}
