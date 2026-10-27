package knightminer.ceramics.fabric.client;

import com.mojang.datafixers.util.Either;
import knightminer.ceramics.blocks.entity.CrackableBlockEntityHandler;
import knightminer.ceramics.items.CrackableBlockItem;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The {@code ceramics:cracked} model on Fabric: the block model as it is, plus five variants with the cracks texture
 * over every face, chosen from the cracks of the block entity (or of the item).
 */
public class CrackedModelFabric implements UnbakedModel {
  private final BlockModel model;

  public CrackedModelFabric(BlockModel model) {
    this.model = model;
  }

  @Override
  public Collection<ResourceLocation> getDependencies() {
    return model.getDependencies();
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    model.resolveParents(resolver);
  }

  @Nullable
  @Override
  public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
    BakedModel original = model.bake(baker, spriteGetter, state);
    if (original == null) {
      return null;
    }
    Material[] textures = new Material[5];
    for (int i = 0; i < 5; i++) {
      textures[i] = model.getMaterial("cracks_" + (i + 1));
      // missing texture errors show up now
      spriteGetter.apply(textures[i]);
    }
    // the elements with a copy of each face using the cracks texture
    List<BlockElement> elements = model.getElements();
    List<BlockElement> cracked = new ArrayList<>(elements.size() * 2);
    cracked.addAll(elements);
    for (BlockElement element : elements) {
      Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
      element.faces.forEach((side, face) -> faces.put(side, new BlockElementFace(face.cullForDirection(), -1, "#cracks", face.uv())));
      cracked.add(new BlockElement(element.from, element.to, faces, element.rotation, element.shade));
    }
    return new Baked(original, baker, cracked, textures, state);
  }

  private class Baked extends ForwardingBakedModel {
    private final BakedModel[] crackedModels = new BakedModel[5];
    private final ModelBaker baker;
    private final List<BlockElement> elements;
    private final Material[] textures;
    private final ModelState state;
    private final ItemOverrides overrides;

    Baked(BakedModel original, ModelBaker baker, List<BlockElement> elements, Material[] textures, ModelState state) {
      this.wrapped = original;
      this.baker = baker;
      this.elements = elements;
      this.textures = textures;
      this.state = state;
      this.overrides = new Overrides(baker);
    }

    /** Model with the given cracks, 1 to 5 */
    BakedModel getModel(int cracks) {
      int stage = cracks - 1;
      if (crackedModels[stage] == null) {
        BlockModel retextured = new BlockModel(null, elements, Map.of("cracks", Either.left(textures[stage])), null, null, ItemTransforms.NO_TRANSFORMS, List.of());
        retextured.parent = model;
        retextured.name = model.name + "#cracks_" + cracks;
        crackedModels[stage] = retextured.bake(baker, Material::sprite, state);
      }
      return crackedModels[stage];
    }

    @Override
    public boolean isVanillaAdapter() {
      return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState blockState, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
      int cracks = level.getBlockEntity(pos) instanceof CrackableBlockEntityHandler.ICrackableBlockEntity crackable ? crackable.getCracksHandler().getCracks() : 0;
      (cracks > 0 ? getModel(cracks) : wrapped).emitBlockQuads(level, blockState, pos, random, context);
    }

    @Override
    public ItemOverrides getOverrides() {
      return overrides;
    }

    private class Overrides extends ItemOverrides {
      Overrides(ModelBaker baker) {
        super(baker, null, List.of());
      }

      @Nullable
      @Override
      public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        int cracks = CrackableBlockItem.getCracks(stack);
        return cracks > 0 ? getModel(cracks) : model;
      }
    }
  }
}
