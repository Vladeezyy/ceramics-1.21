package knightminer.ceramics.items;

import knightminer.ceramics.Registration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/** BlockItem for crackable blocks to show cracked amount */
public class CrackableBlockItem extends FixedTooltipBlockItem {
	private static final Component CRACKABLE = Ceramics.component("tooltip", "crackable").withStyle(ChatFormatting.GRAY);;
	private static final String TOOLTIP_KEY = Ceramics.lang("tooltip", "cracked");
	public CrackableBlockItem(Block blockIn, Properties builder, String tooltipSuffix) {
		super(blockIn, builder, tooltipSuffix);
	}

	/**
	 * Gets the cracks value for the given stack
	 * @param stack  Stack
	 * @return  Cracks
	 */
	public static int getCracks(ItemStack stack) {
		Integer cracks = stack.get(Registration.CRACKS);
		if (cracks != null) {
			return cracks;
		}
		// block item from a 1.20 world: the world upgrade keeps the old tag as custom data
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null) {
			return data.copyTag().getInt(CrackableBlockEntityHandler.TAG_CRACKS).map(value -> Mth.clamp(value, 0, 5)).orElse(0);
		}
		return 0;
	}

	/**
	 * Sets the number of cracks for the item
	 * @param stack  Stack to set
	 * @param cracks  New cracks value
	 */
	public static ItemStack setCracks(ItemStack stack, int cracks) {
		removeLegacyCracks(stack);
		if (cracks <= 0) {
			stack.remove(Registration.CRACKS);
		} else {
			stack.set(Registration.CRACKS, Math.min(cracks, 5));
		}
		return stack;
	}

	/** Removes the cracks tag left by a 1.20 world */
	private static void removeLegacyCracks(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null && data.contains(CrackableBlockEntityHandler.TAG_CRACKS)) {
			CompoundTag tag = data.copyTag();
			tag.remove(CrackableBlockEntityHandler.TAG_CRACKS);
			if (tag.isEmpty()) {
				stack.remove(DataComponents.CUSTOM_DATA);
			} else {
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
		// convert 1.20 cracks into the component
		if (!stack.has(Registration.CRACKS)) {
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			if (data != null && data.contains(CrackableBlockEntityHandler.TAG_CRACKS)) {
				setCracks(stack, getCracks(stack));
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		int cracks = getCracks(stack);
		if (cracks > 0) {
			tooltip.accept(Component.translatable(TOOLTIP_KEY, 6 - cracks, 6));
		}
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(CRACKABLE);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.getCount() == 1 && getCracks(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return 13 - getCracks(stack) * 2;
	}

	@Override
	public int getBarColor(ItemStack stack) {
		// the item has no durability, so the default color divides by zero; base it on the cracks instead
		return Mth.hsvToRgb((6 - getCracks(stack)) / 18f, 1.0F, 1.0F);
	}
}
