package knightminer.ceramics.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements.Strategy;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.ItemStackTemplate;
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
		// any stack of the item, matches only accepts cracked ones
		super(new Recipe.CommonInfo(true), new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, Ceramics.locationName("clay_repair")),
			new ItemStackTemplate(item.asItem()), List.of(Ingredient.of(item), repairIngredient));
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
	public ItemStack assemble(CraftingInput inv) {
		for (int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if (stack.getItem() == item) {
				stack = stack.copyWithCount(1);
				return setCracks(stack, Math.max(0, getCracks(stack) - 3));
			}
		}
		return super.assemble(inv);
	}

	@Override
	@SuppressWarnings("unchecked")
	public RecipeSerializer<ShapelessRecipe> getSerializer() {
		return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) Registration.CLAY_REPAIR_RECIPE_SERIALIZER.get();
	}

	/** Serializer of this recipe, since 26.1 a record of the codecs */
	private static final MapCodec<CrackedClayRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CrackedClayRepairRecipe::getItem),
		Ingredient.CODEC.fieldOf("ingredient").forGetter(CrackedClayRepairRecipe::getRepairIngredient)
	).apply(inst, CrackedClayRepairRecipe::new));
	private static final StreamCodec<RegistryFriendlyByteBuf, CrackedClayRepairRecipe> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM), CrackedClayRepairRecipe::getItem,
		Ingredient.CONTENTS_STREAM_CODEC, CrackedClayRepairRecipe::getRepairIngredient,
		CrackedClayRepairRecipe::new);
	public static final RecipeSerializer<CrackedClayRepairRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	/**
	 * Saves the recipe for datagens, replaces the finished recipe of 1.20
	 * @param output     Recipe output
	 * @param id         Recipe ID
	 * @param item       Crackable item
	 * @param ingredient Repair ingredient
	 * @param criteria   Criteria to unlock the recipe, null for no advancement
	 */
	public static void save(RecipeOutput output, Identifier location, ItemLike item, Ingredient ingredient, @Nullable Criterion<?> criteria) {
		ResourceKey<Recipe<?>> id = ResourceKey.create(Registries.RECIPE, location);
		CrackedClayRepairRecipe recipe = new CrackedClayRepairRecipe(item, ingredient);
		if (criteria != null) {
			Advancement.Builder advancement = output.advancement()
				.addCriterion("has_item", criteria)
				.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
				.rewards(AdvancementRewards.Builder.recipe(id))
				.requirements(Strategy.OR);
			output.accept(id, recipe, advancement.build(location.withPrefix("recipes/clay_repair/")));
		} else {
			output.accept(id, recipe, null);
		}
	}
}
