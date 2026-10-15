package knightminer.ceramics.items;

import net.minecraft.core.HolderLookup;
import com.google.common.collect.ImmutableList;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SolidClayBucketItem extends BaseClayBucketItem {
	/** List of all blocks that can be placed in buckets */
	private static List<Block> BUCKETABLE_BLOCKS = Collections.emptyList();

	/** Loads all bucketable blocks from the item registry */
	public static void loadBucketableBlocks() {
		ImmutableList.Builder<Block> builder = ImmutableList.builder();
		for (Item item : BuiltInRegistries.ITEM) {
			if (item instanceof SolidBucketItem solidBucket) {
				builder.add(solidBucket.getBlock());
			}
		}
		BUCKETABLE_BLOCKS = builder.build();
	}

	/** Tag name for block in a bucket in 1.20, used to read old worlds */
	public static final String TAG_BLOCK = "block";

	public SolidClayBucketItem(boolean isCracked, Properties props) {
		super(isCracked, props);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		Block block = getBlock(stack);
		if (block == Blocks.AIR || !(block.asItem() instanceof BlockItem blockItem)) {
			return InteractionResult.FAIL;
		}

		ItemStack resultStack = context.getItemInHand().getCraftingRemainder();
		InteractionResult result = blockItem.useOn(new BlockPlaceContext(context));
		Player player = context.getPlayer();
		if (result.consumesAction() && player != null && !player.isCreative()) {
			player.setItemInHand(context.getHand(), resultStack);
		}
		return result;
	}


	/* Bucket properties */

	/**
	 * Gets the block from the given clay bucket container
	 * @param stack  Bucket stack
	 * @return  Block contained in the container, air if invalid
	 */
	public Block getBlock(ItemStack stack) {
		Block block = stack.get(Registration.BUCKET_BLOCK);
		if (block != null) {
			return block;
		}
		// bucket from a 1.20 world, not converted yet
		String legacy = getLegacyString(stack, TAG_BLOCK);
		if (legacy != null) {
			Identifier location = Identifier.tryParse(legacy);
			if (location != null) {
				return BuiltInRegistries.BLOCK.getValue(location);
			}
		}
		return Blocks.AIR;
	}

	/**
	 * Sets the fluid for the given stack
	 * @param stack  Item stack instance
	 * @param block  BLock instance
	 * @return  Modified stack
	 */
	protected static ItemStack setBlock(ItemStack stack, Block block) {
		stack.set(Registration.BUCKET_BLOCK, block);
		return stack;
	}

	@Override
	protected void migrateLegacy(ItemStack stack) {
		if (getLegacyString(stack, TAG_BLOCK) != null) {
			Block block = getBlock(stack);
			removeLegacyTag(stack, TAG_BLOCK);
			if (block != Blocks.AIR) {
				setBlock(stack, block);
			}
		}
	}


	/* Item stack properties */

	@Override
	public Component getName(ItemStack stack) {
		Block block = getBlock(stack);
		MutableComponent component;
		if(block == Blocks.AIR) {
			component = super.getName(stack).plainCopy();
		} else {
			// if the specific block is translatable, use that
			String key = this.getDescriptionId();
			Identifier location = BuiltInRegistries.BLOCK.getKey(block);
			String blockKey = String.format("%s.%s.%s", key, location.getNamespace(), location.getPath());
			if (Language.getInstance().has(blockKey)) {
				component = Component.translatable(blockKey);
			} else {
				component = Component.translatable(key + ".filled", Component.translatable(block.getDescriptionId()));
			}
		}
		// display name in red
		return component.withStyle(ChatFormatting.RED);
	}

	@Override
	public void addVariants(Consumer<ItemStack> consumer) {
		for (Block block : BUCKETABLE_BLOCKS) {
			if (isCracked == block.defaultBlockState().is(CeramicsTags.Blocks.BUCKET_CRACKING_BLOCKS)) {
				consumer.accept(setBlock(new ItemStack(this), block));
			}
		}
	}

	@Override
	public String getCreatorModId(HolderLookup.Provider registries, ItemStack stack) {
		Block block = getBlock(stack);
		if (block != Blocks.AIR) {
			String namespace = BuiltInRegistries.BLOCK.getKey(block).getNamespace();
			if (!"minecraft".equals(namespace)) {
				return namespace;
			}
		}
		return Ceramics.MOD_ID;
	}
}
