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
import net.minecraft.container.Container;
import net.minecraft.container.ContainerListener;
import net.minecraft.container.Slot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BuiltContainer extends Container implements IExtendedContainerListener {

	private final String name;

	private final Predicate<PlayerEntity> canInteract;
	private final List<Range<Integer>> playerSlotRanges;
	private final List<Range<Integer>> tileSlotRanges;

	private final ArrayList<MutableTriple<Supplier, Consumer, Object>> objectValues;
	private List<Consumer<CraftingInventory>> craftEvents;
	private Integer[] integerParts;

	private final BlockEntity tile;

	public BuiltContainer(final String name, final Predicate<PlayerEntity> canInteract,
	                      final List<Range<Integer>> playerSlotRange,
	                      final List<Range<Integer>> tileSlotRange, BlockEntity tile) {
		this.name = name;

		this.canInteract = canInteract;

		this.playerSlotRanges = playerSlotRange;
		this.tileSlotRanges = tileSlotRange;

		this.objectValues = new ArrayList<>();

		this.tile = tile;

		this.syncId = 120; //Wee :) Thanks asie, this is required to stop the odd container de-sycns
	}

	@Override
	public Slot addSlot(Slot slot) {
		return super.addSlot(slot);
	}

	public void addObjectSync(final List<Pair<Supplier, Consumer>> syncables) {

		for (final Pair<Supplier, Consumer> syncable : syncables)
			this.objectValues.add(MutableTriple.of(syncable.getLeft(), syncable.getRight(), null));
		this.objectValues.trimToSize();
	}

	public void addCraftEvents(final List<Consumer<CraftingInventory>> craftEvents) {
		this.craftEvents = craftEvents;
	}

	@Override
	public boolean canUse(final PlayerEntity playerIn) {
		return this.canInteract.test(playerIn);
	}

	@Override
	public final void onContentChanged(final Inventory inv) {
		if (!this.craftEvents.isEmpty())
			this.craftEvents.forEach(consumer -> consumer.accept((CraftingInventory) inv));
	}

	@Override
	public void sendContentUpdates() {
		super.sendContentUpdates();

		for (final ContainerListener listener : this.listeners) {
			if (!this.objectValues.isEmpty()) {
				int objects = 0;
				for (final MutableTriple<Supplier, Consumer, Object> value : this.objectValues) {
					final Object supplied = value.getLeft().get();
					if (supplied != value.getRight()) {
						sendObject(listener, this, objects, supplied);
						value.setRight(supplied);
					}
					objects++;
				}
			}
		}
	}

	@Override
	public void addListener(ContainerListener listener) {
		super.addListener(listener);

		if (!this.objectValues.isEmpty()) {
			int objects = 0;
			for (final MutableTriple<Supplier, Consumer, Object> value : this.objectValues) {
				final Object supplied = value.getLeft();
				sendObject(listener, this, objects, supplied);
				value.setRight(supplied);
				objects++;
			}
		}
	}

	@Override
	public void handleObject(int var, Object value) {
		this.objectValues.get(var).getMiddle().accept(value);
	}

	@Override
	public ItemStack transferSlot(PlayerEntity player, int index) {
		ItemStack originalStack = ItemStack.EMPTY;

		final Slot slot = this.slotList.get(index);

		if (slot != null && slot.hasStack()) {

			final ItemStack stackInSlot = slot.getStack();
			originalStack = stackInSlot.copy();

			boolean shifted = false;

			for (final Range<Integer> range : this.playerSlotRanges)
				if (range.contains(index)) {

					if (this.shiftToTile(stackInSlot))
						shifted = true;
					break;
				}

			if (!shifted)
				for (final Range<Integer> range : this.tileSlotRanges)
					if (range.contains(index)) {
						if (this.shiftToPlayer(stackInSlot))
							shifted = true;
						break;
					}

			slot.onStackChanged(stackInSlot, originalStack);
			if (stackInSlot.getAmount() <= 0)
				slot.setStack(ItemStack.EMPTY);
			else
				slot.markDirty();
			if (stackInSlot.getAmount() == originalStack.getAmount())
				return ItemStack.EMPTY;
			slot.onTakeItem(player, stackInSlot);
		}
		return originalStack;
	}

	protected boolean shiftItemStack(final ItemStack stackToShift, final int start, final int end) {
		boolean changed = false;
		if (stackToShift.canStack()) {
			for (int slotIndex = start; stackToShift.getAmount() > 0 && slotIndex < end; slotIndex++) {
				final Slot slot = this.slotList.get(slotIndex);
				final ItemStack stackInSlot = slot.getStack();
				if (!stackInSlot.isEmpty() && ItemStack.areEqual(stackInSlot, stackToShift)
					&& slot.canInsert(stackToShift)) {
					final int resultingStackSize = stackInSlot.getAmount() + stackToShift.getAmount();
					final int max = Math.min(stackToShift.getMaxAmount(), slot.getMaxStackAmount());
					if (resultingStackSize <= max) {
						stackToShift.setAmount(0);
						stackInSlot.setAmount(resultingStackSize);
						slot.markDirty();
						changed = true;
					} else if (stackInSlot.getAmount() < max) {
						stackToShift.subtractAmount(max - stackInSlot.getAmount());
						stackInSlot.setAmount(max);
						slot.markDirty();
						changed = true;
					}
				}
			}
		}
		if (stackToShift.getAmount() > 0) {
			for (int slotIndex = start; stackToShift.getAmount() > 0 && slotIndex < end; slotIndex++) {
				final Slot slot = this.slotList.get(slotIndex);
				ItemStack stackInSlot = slot.getStack();
				if (stackInSlot.isEmpty() && slot.canInsert(stackToShift)) {
					final int max = Math.min(stackToShift.getMaxAmount(), slot.getMaxStackAmount());
					stackInSlot = stackToShift.copy();
					stackInSlot.setAmount(Math.min(stackToShift.getAmount(), max));
					stackToShift.subtractAmount(stackInSlot.getAmount());
					slot.setStack(stackInSlot);
					slot.markDirty();
					changed = true;
				}
			}
		}
		return changed;
	}

	private boolean shiftToTile(final ItemStack stackToShift) {
		for (final Range<Integer> range : this.tileSlotRanges)
			if (this.shiftItemStack(stackToShift, range.getMinimum(), range.getMaximum() + 1))
				return true;
		return false;
	}

	private boolean shiftToPlayer(final ItemStack stackToShift) {
		for (final Range<Integer> range : this.playerSlotRanges)
			if (this.shiftItemStack(stackToShift, range.getMinimum(), range.getMaximum() + 1))
				return true;
		return false;
	}

	public String getName() {
		return this.name;
	}
}
