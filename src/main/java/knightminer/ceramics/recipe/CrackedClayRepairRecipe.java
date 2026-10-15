package knightminer.ceramics.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements.Strategy;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import static knightminer.ceramics.items.CrackableBlockItem.getCracks;
import static knightminer.ceramics.items.CrackableBlockItem.setCracks;

public class CrackedClayRepairRecipe extends ShapelessRecipe {
	private final Item item;
	private final Ingredient repairIngredient;
	public CrackedClayRepairRecipe(ItemLike item, Ingredient repairIngredient) {
		super(Ceramics.locationName("clay_repair"), CraftingBookCategory.MISC, new ItemStack(item), NonNullList.of(Ingredient.EMPTY, Ingredient.of(setCracks(new ItemStack(item), 3)), repairIngredient));
		this.item = item.asItem();
		this.repairIngredient = repairIngredient;
	}

	public Item getItem() {
		return item;
	}

	public Ingredient getRepairIngredient() {
		return repairIngredient;
	}

	@Override
	public boolean matches(CraftingInput inv, Level worldIn) {
		if (!super.matches(inv, worldIn)) {
			return false;
		}
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() == item) {
				return getCracks(stack) > 0;
			}
		}
		return false;
	}

	@Override
	public ItemStack assemble(CraftingInput inv, HolderLookup.Provider access) {
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() == item) {
				stack = stack.copyWithCount(1);
				return setCracks(stack, Math.max(0, getCracks(stack) - 3));
			}
		}
		return super.assemble(inv, access);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return Registration.CLAY_REPAIR_RECIPE_SERIALIZER.get();
	}

	/** Serializer class for this recipe */
	public static class Serializer implements RecipeSerializer<CrackedClayRepairRecipe> {
		private static final MapCodec<CrackedClayRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CrackedClayRepairRecipe::getItem),
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrackedClayRepairRecipe::getRepairIngredient)
		).apply(inst, CrackedClayRepairRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, CrackedClayRepairRecipe> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM), CrackedClayRepairRecipe::getItem,
			Ingredient.CONTENTS_STREAM_CODEC, CrackedClayRepairRecipe::getRepairIngredient,
			CrackedClayRepairRecipe::new);

		@Override
		public MapCodec<CrackedClayRepairRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CrackedClayRepairRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}

	/**
	 * Saves the recipe for datagens, replaces the finished recipe of 1.20
	 * @param output     Recipe output
	 * @param id         Recipe ID
	 * @param item       Crackable item
	 * @param ingredient Repair ingredient
	 * @param criteria   Criteria to unlock the recipe, null for no advancement
	 */
	public static void save(RecipeOutput output, ResourceLocation id, ItemLike item, Ingredient ingredient, @Nullable Criterion<?> criteria) {
		CrackedClayRepairRecipe recipe = new CrackedClayRepairRecipe(item, ingredient);
		if (criteria != null) {
			Advancement.Builder advancement = output.advancement()
				.addCriterion("has_item", criteria)
				.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
				.rewards(AdvancementRewards.Builder.recipe(id))
				.requirements(Strategy.OR);
			output.accept(id, recipe, advancement.build(id.withPrefix("recipes/clay_repair/")));
		} else {
			output.accept(id, recipe, null);
		}
	}
}
