package knightminer.ceramics.items;

import knightminer.ceramics.Ceramics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Consumer;

/**
 * Adds a specific tooltip to a block, instead of basing it off the translation key as {@link dev.vladeezyy.kilnwork.item.BlockTooltipItem} does
 */
public class FixedTooltipBlockItem extends BlockItem {
	private final Component tooltipComponent;
	public FixedTooltipBlockItem(Block blockIn, Properties builder, String tooltipSuffix) {
		super(blockIn, builder);
		tooltipComponent = Component.translatable(Ceramics.lang("block", tooltipSuffix)).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
		tooltip.accept(tooltipComponent);
		super.appendHoverText(stack, context, display, tooltip, flagIn);
	}
}
