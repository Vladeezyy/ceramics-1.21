package knightminer.ceramics.fabric.client;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import dev.vladeezyy.kilnwork.fabric.transfer.FluidAmounts;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The {@code ceramics:fluid_bucket} and {@code ceramics:solid_bucket} models on Fabric, built from vanilla item model
 * layers: the cover (3D), the inside behind it and the contents through the fluid mask. The contents are tinted with
 * tint index 1 (item colors registered by CeramicsFabricClient). Unlike NeoForge, glowing contents do not glow.
 */
public class ClayBucketModelFabric implements UnbakedModel {
  private static final ResourceLocation ITEM_GENERATED = ResourceLocation.withDefaultNamespace("item/generated");
  private static final ItemModelGenerator GENERATOR = new ItemModelGenerator();

  private final boolean solid;
  private final ResourceLocation contentsId;
  private final boolean flipGas;
  private final Map<String, Material> textures = new HashMap<>();

  public ClayBucketModelFabric(JsonObject json, boolean solid) {
    this.solid = solid;
    String key = solid ? "block" : "fluid";
    this.contentsId = json.has(key) ? ResourceLocation.parse(GsonHelper.getAsString(json, key)) : null;
    this.flipGas = GsonHelper.getAsBoolean(json, "flip_gas", json.has("flipGas") && json.get("flipGas").getAsBoolean());
    JsonObject jsonTextures = GsonHelper.getAsJsonObject(json, "textures", new JsonObject());
    for (String name : jsonTextures.keySet()) {
      textures.put(name, new Material(InventoryMenu.BLOCK_ATLAS, ResourceLocation.parse(jsonTextures.get(name).getAsString())));
    }
  }

  @Override
  public Collection<ResourceLocation> getDependencies() {
    return Set.of(ITEM_GENERATED);
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    resolver.apply(ITEM_GENERATED).resolveParents(resolver);
  }

  private Object defaultContents() {
    if (contentsId == null) {
      return solid ? Blocks.AIR : Fluids.EMPTY;
    }
    return solid ? BuiltInRegistries.BLOCK.get(contentsId) : BuiltInRegistries.FLUID.get(contentsId);
  }

  private static boolean isEmpty(Object contents) {
    return contents == Fluids.EMPTY || contents == Blocks.AIR;
  }

  @Nullable
  @Override
  public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
    ItemTransforms transforms = baker.getModel(ITEM_GENERATED) instanceof BlockModel generated ? generated.getTransforms() : ItemTransforms.NO_TRANSFORMS;
    Object contents = defaultContents();
    BakedModel model = bakeContents(baker, spriteGetter, state, transforms, contents);
    if (isEmpty(contents)) {
      return new OverrideBaked(model, new Overrides(baker, state, transforms));
    }
    return model;
  }

  /** Texture of the contents on the block atlas */
  private static Material contentsTexture(Object contents) {
    TextureAtlasSprite sprite;
    if (contents instanceof Fluid fluid) {
      sprite = KilnworkClientPlatform.get().stillSprite(new FluidStack(fluid, FluidStack.BUCKET_VOLUME));
    } else {
      sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(((Block) contents).defaultBlockState()).getParticleIcon();
    }
    return new Material(InventoryMenu.BLOCK_ATLAS, sprite.contents().name());
  }

  private BakedModel bakeContents(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state, ItemTransforms transforms, Object contents) {
    Map<String, Either<Material, String>> modelTextures = new HashMap<>();
    textures.forEach((name, material) -> modelTextures.put(name, Either.left(material)));
    if (!modelTextures.containsKey("particle")) {
      Material particle = textures.getOrDefault("inner", textures.get("base"));
      if (particle != null) {
        modelTextures.put("particle", Either.left(particle));
      }
    }
    List<BlockElement> elements = new ArrayList<>();
    Material inner = textures.get("inner");
    Material base = textures.get("base");
    Material mask = textures.get("fluid");
    if (isEmpty(contents)) {
      // no contents: just the inside (or the cover), looks better
      Material empty = inner != null ? inner : base;
      if (empty != null) {
        elements.addAll(GENERATOR.processFrames(0, empty == inner ? "inner" : "base", spriteGetter.apply(empty).contents()));
      }
    } else {
      // the cover is the only layer in full 3D
      if (base != null) {
        elements.addAll(GENERATOR.processFrames(0, "base", spriteGetter.apply(base).contents()));
      }
      // the inside, flat behind the contents
      if (inner != null) {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        faces.put(Direction.SOUTH, new BlockElementFace(null, -1, "#inner", new BlockFaceUV(new float[] {0, 0, 16, 16}, 0)));
        elements.add(new BlockElement(new Vector3f(0, 0, 7.25f), new Vector3f(16, 16, 7.25f), faces, null, true));
      }
      // the contents through the mask, slightly inside the cover, only the front and back
      if (mask != null) {
        modelTextures.put("contents", Either.left(contentsTexture(contents)));
        for (BlockElement element : GENERATOR.processFrames(1, "contents", spriteGetter.apply(mask).contents())) {
          Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
          element.faces.forEach((side, face) -> {
            if (side.getAxis() == Direction.Axis.Z) {
              faces.put(side, face);
            }
          });
          if (!faces.isEmpty()) {
            elements.add(new BlockElement(new Vector3f(element.from.x(), element.from.y(), 7.51f), new Vector3f(element.to.x(), element.to.y(), 8.49f), faces, element.rotation, element.shade));
          }
        }
      }
    }
    BlockModel model = new BlockModel(null, elements, modelTextures, false, BlockModel.GuiLight.FRONT, transforms, List.of());
    model.name = Ceramics.locationName("clay_bucket#" + contents);
    // gases are upside down in their bucket
    if (flipGas && contents instanceof Fluid fluid && FluidVariantAttributes.isLighterThanAir(FluidAmounts.toVariant(new FluidStack(fluid, 1)))) {
      ModelState original = state;
      state = new ModelState() {
        @Override
        public Transformation getRotation() {
          return original.getRotation().compose(new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
        }

        @Override
        public boolean isUvLocked() {
          return original.isUvLocked();
        }
      };
    }
    return model.bake(baker, model, spriteGetter, state, false);
  }

  /** The empty bucket, which turns into the bucket of its contents */
  private static class OverrideBaked extends net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel {
    private final ItemOverrides overrides;

    OverrideBaked(BakedModel model, ItemOverrides overrides) {
      this.wrapped = model;
      this.overrides = overrides;
    }

    @Override
    public ItemOverrides getOverrides() {
      return overrides;
    }
  }

  private class Overrides extends ItemOverrides {
    private final ModelBaker baker;
    private final ModelState state;
    private final ItemTransforms transforms;
    private final Map<Object, BakedModel> cache = new HashMap<>();

    Overrides(ModelBaker baker, ModelState state, ItemTransforms transforms) {
      super(baker, null, List.of());
      this.baker = baker;
      this.state = state;
      this.transforms = transforms;
    }

    @Nullable
    @Override
    public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
      Object contents;
      if (stack.getItem() instanceof FluidClayBucketItem bucket) {
        contents = bucket.getFluid(stack);
      } else if (stack.getItem() instanceof SolidClayBucketItem bucket) {
        contents = bucket.getBlock(stack);
      } else {
        return model;
      }
      if (isEmpty(contents)) {
        return model;
      }
      return cache.computeIfAbsent(contents, c -> bakeContents(baker, Material::sprite, state, transforms, c));
    }
  }
}
