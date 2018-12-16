/*
 * Copyright (c) 2018 modmuss50 and Gigabit101
 *
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.modmuss50.containerUtils.container.builder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ContainerBuilder {

	private final String name;

	private Predicate<PlayerEntity> canInteract = player -> true;

	final List<Slot> slots;
	final List<Range<Integer>> playerInventoryRanges, tileInventoryRanges;

	final List<Pair<Supplier, Consumer>> objectValues;

	final List<Consumer<CraftingInventory>> craftEvents;

	public ContainerBuilder(final String name) {

		this.name = name;

		this.slots = new ArrayList<>();
		this.playerInventoryRanges = new ArrayList<>();
		this.tileInventoryRanges = new ArrayList<>();

		this.objectValues = new ArrayList<>();

		this.craftEvents = new ArrayList<>();
	}

	public ContainerBuilder interact(final Predicate<PlayerEntity> canInteract) {
		this.canInteract = canInteract;
		return this;
	}

	public ContainerPlayerInventoryBuilder player(final PlayerEntity player) {
		return new ContainerPlayerInventoryBuilder(this, player);
	}

	public ContainerTileInventoryBuilder blockEntity(final Inventory tile) {
		return new ContainerTileInventoryBuilder(this, tile);
	}

	void addPlayerInventoryRange(final Range<Integer> range) {
		this.playerInventoryRanges.add(range);
	}

	void addTileInventoryRange(final Range<Integer> range) {
		this.tileInventoryRanges.add(range);
	}

	@Deprecated
	/**
	 * The container have to know if the blockEntity is still available (the block was not destroyed)
	 * and if the player is not to far from him to close the GUI if necessary
	 */
	public BuiltContainer create() {
		final BuiltContainer built = new BuiltContainer(this.name, this.canInteract,
			this.playerInventoryRanges,
			this.tileInventoryRanges, null);
		if (!this.objectValues.isEmpty())
			built.addObjectSync(objectValues);
		if (!this.craftEvents.isEmpty())
			built.addCraftEvents(this.craftEvents);

		this.slots.forEach(built::addSlot);

		this.slots.clear();
		return built;
	}

	public BuiltContainer create(final BlockEntity tile) {
		final BuiltContainer built = new BuiltContainer(this.name, this.canInteract,
			this.playerInventoryRanges,
			this.tileInventoryRanges, tile);
		if (!this.craftEvents.isEmpty())
			built.addCraftEvents(this.craftEvents);
		if (!this.objectValues.isEmpty())
			built.addObjectSync(objectValues);

		this.slots.forEach(built::addSlot);

		this.slots.clear();
		return built;
	}
}
