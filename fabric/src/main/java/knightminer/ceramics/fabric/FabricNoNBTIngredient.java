package knightminer.ceramics.fabric;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.Ceramics;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.List;

/**
 * Ingredient that only matches if the stack has no component changes, the Fabric side of NoNBTIngredient.
 * JSON: {"fabric:type": "ceramics:no_nbt", "ingredient": ...} (the build writes "fabric:type" for NeoForge's "type").
 */
public record FabricNoNBTIngredient(Ingredient ingredient) implements CustomIngredient {
  public static final ResourceLocation ID = Ceramics.getResource("no_nbt");
  public static final MapCodec<FabricNoNBTIngredient> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
    Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FabricNoNBTIngredient::ingredient)
  ).apply(inst, FabricNoNBTIngredient::new));
  public static final StreamCodec<RegistryFriendlyByteBuf, FabricNoNBTIngredient> STREAM_CODEC = Ingredient.CONTENTS_STREAM_CODEC.map(FabricNoNBTIngredient::new, FabricNoNBTIngredient::ingredient);

  public static final CustomIngredientSerializer<FabricNoNBTIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public MapCodec<FabricNoNBTIngredient> getCodec(boolean allowEmpty) {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, FabricNoNBTIngredient> getPacketCodec() {
      return STREAM_CODEC;
    }
  };

  @Override
  public boolean test(ItemStack stack) {
    return stack.getComponentsPatch().isEmpty() && ingredient.test(stack);
  }

  @Override
  public List<ItemStack> getMatchingStacks() {
    return Arrays.asList(ingredient.getItems());
  }

  @Override
  public boolean requiresTesting() {
    return true;
  }

  @Override
  public CustomIngredientSerializer<?> getSerializer() {
    return SERIALIZER;
  }
}
