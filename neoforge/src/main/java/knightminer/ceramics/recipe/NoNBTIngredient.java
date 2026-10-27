package knightminer.ceramics.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.Registration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Ingredient that only matches if the stack has no extra data. In 1.20 that meant no NBT, in 1.21 no component changes.
 * JSON: {"type": "ceramics:no_nbt", "ingredient": ...}
 */
public record NoNBTIngredient(Ingredient ingredient) implements ICustomIngredient {
	public static final MapCodec<NoNBTIngredient> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(NoNBTIngredient::ingredient)
	).apply(inst, NoNBTIngredient::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, NoNBTIngredient> STREAM_CODEC = Ingredient.CONTENTS_STREAM_CODEC.map(NoNBTIngredient::new, NoNBTIngredient::ingredient);

	/** Creates a vanilla ingredient wrapping this */
	public static Ingredient of(Ingredient ingredient) {
		return new NoNBTIngredient(ingredient).toVanilla();
	}

	@Override
	public boolean test(@Nullable ItemStack stack) {
		return stack != null && stack.isComponentsPatchEmpty() && ingredient.test(stack);
	}

	@Override
	public Stream<ItemStack> getItems() {
		return Arrays.stream(ingredient.getItems());
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return knightminer.ceramics.neoforge.CeramicsNeoForge.NO_NBT_INGREDIENT.get();
	}
}
