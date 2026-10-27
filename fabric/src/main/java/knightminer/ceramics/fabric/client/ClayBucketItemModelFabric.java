package knightminer.ceramics.fabric.client;

import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.fluid.api.FluidStack;
import dev.vladeezyy.kilnwork.platform.KilnworkClientPlatform;
import dev.vladeezyy.kilnwork.platform.KilnworkPlatform;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item model {@code ceramics:clay_bucket} on Fabric, the same JSON and look as the NeoForge one (ClayBucketItemModel):
 * the outer cover ({@code base}) is the full 3D item, the {@code inner} texture is a flat quad half a pixel behind it,
 * and the contents are drawn inside, masked by {@code fluid}. Built from vanilla parts, Fabric has no item model helpers.
 * <pre>{"type": "ceramics:clay_bucket", "contents": "fluid", "textures": {"base": ..., "inner": ..., "fluid": ...}}</pre>
 */
public class ClayBucketItemModelFabric implements ItemModel {
  public static final Identifier ID = Identifier.fromNamespaceAndPath("ceramics", "clay_bucket");
  private static final Identifier GENERATED = Identifier.withDefaultNamespace("item/generated");
  private static final ModelDebugName DEBUG_NAME = () -> "ClayBucketItemModel";
  /** Fluid is slightly inside the cover */
  private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 0.998f), new Quaternionf());
  /** Inner texture is half a pixel back */
  private static final Transformation INNER_TRANSFORM = new Transformation(new Vector3f(0, 0, -0.5f / 16f), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf());
  /** Lighter than air fluids turn the bucket upside down */
  private static final Transformation UPSIDE_DOWN = new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null);

  /** What the bucket holds */
  public enum Contents implements StringRepresentable {
    FLUID, BLOCK;
    public static final Codec<Contents> CODEC = StringRepresentable.fromEnum(Contents::values);

    @Override
    public String getSerializedName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Textures of the bucket, all on the item atlas */
  public record Textures(Material base, Material inner, Material fluid) {
    public static final Codec<Textures> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Material.CODEC.fieldOf("base").forGetter(Textures::base),
      Material.CODEC.fieldOf("inner").forGetter(Textures::inner),
      Material.CODEC.fieldOf("fluid").forGetter(Textures::fluid)
    ).apply(inst, Textures::new));
  }

  private final Unbaked unbaked;
  private final BakingContext context;
  private final Matrix4fc transformation;
  private final ItemTransforms transforms;
  /** Baked model for each contents, the contents are fluids or blocks */
  private final Map<Object, ItemModel> cache = new IdentityHashMap<>();

  private ClayBucketItemModelFabric(Unbaked unbaked, BakingContext context, Matrix4fc transformation) {
    this.unbaked = unbaked;
    this.context = context;
    this.transformation = transformation;
    this.transforms = context.blockModelBaker().getModel(GENERATED).getTopTransforms();
  }

  /** Builds the model for the given contents, {@link Fluids#EMPTY} or {@link Blocks#AIR} for empty */
  private ItemModel bake(Object contents) {
    ModelBaker baker = context.blockModelBaker();
    ModelBaker.Interner interner = baker.interner();
    Material.Baked base = baker.materials().get(unbaked.textures.base, DEBUG_NAME);
    Material.Baked inner = baker.materials().get(unbaked.textures.inner, DEBUG_NAME);
    Material.Baked mask = baker.materials().get(unbaked.textures.fluid, DEBUG_NAME);
    ModelRenderProperties properties = new ModelRenderProperties(false, inner, transforms);
    ModelState state = BlockModelRotation.IDENTITY;

    // what goes inside, on the block atlas
    Material.Baked contentsMaterial = null;
    int light = 0;
    boolean tintFluid = false;
    if (contents instanceof Fluid fluid && fluid != Fluids.EMPTY) {
      FluidStack stack = new FluidStack(fluid, FluidStack.BUCKET_VOLUME);
      contentsMaterial = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial();
      light = KilnworkPlatform.get().fluidLightLevel(stack);
      tintFluid = true;
      if (FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid))) {
        state = new Composed(state, UPSIDE_DOWN);
      }
    } else if (contents instanceof Block block && block != Blocks.AIR) {
      contentsMaterial = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(block.defaultBlockState());
      light = block.defaultBlockState().getLightEmission();
    }

    List<ItemModel> parts = new ArrayList<>();
    if (contentsMaterial == null) {
      // if empty, just render the inner sprite, looks better
      parts.add(new CuboidItemModelWrapper(List.of(), extruded(interner, inner, state), properties, transformation));
    } else {
      // base is the outer cover, the only layer in full 3D; inner is behind the contents, about a pixel back so it is
      // not cut off in hand
      QuadCollection.Builder quads = new QuadCollection.Builder();
      quads.addAll(extruded(interner, base, state));
      BakedQuad.MaterialInfo innerInfo = interner.materialInfo(BakedQuad.MaterialInfo.of(inner, inner.sprite().transparency(), -1, false, 0));
      Vector3f from = new Vector3f(0, 0, 8.5f);
      Vector3f to = new Vector3f(16, 16, 8.5f);
      quads.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, new CuboidFace.UVs(0, 0, 16, 16), Quadrant.R0, innerInfo, Direction.SOUTH, new Composed(state, INNER_TRANSFORM), null));
      parts.add(new CuboidItemModelWrapper(List.of(), quads.build(), properties, transformation));

      // contents next, only the front and back
      BakedQuad.MaterialInfo info = interner.materialInfo(new BakedQuad.MaterialInfo(contentsMaterial.sprite(), ChunkSectionLayer.SOLID, renderType(contentsMaterial), 0, light <= 0, light));
      QuadCollection masked = maskQuads(interner, mask, info, new Composed(state, FLUID_TRANSFORM));
      parts.add(new CuboidItemModelWrapper(tintFluid ? List.of(FluidTint.INSTANCE) : List.of(), masked, properties, transformation));
    }
    return new CompositeModel(parts);
  }

  /** The sprite as a generated item layer: front, back and the edges of its pixels */
  private static QuadCollection extruded(ModelBaker.Interner interner, Material.Baked material, ModelState state) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    BakedQuad.MaterialInfo info = interner.materialInfo(BakedQuad.MaterialInfo.of(material, material.sprite().transparency(), 0, true, 0));
    ItemModelGenerator.bakeExtrudedSprite(builder, interner, state, info);
    return builder.build();
  }

  /** Front and back quads with the given material covering only the opaque pixels of the mask */
  private static QuadCollection maskQuads(ModelBaker.Interner interner, Material.Baked mask, BakedQuad.MaterialInfo info, ModelState state) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    SpriteContents contents = mask.sprite().contents();
    int width = contents.width();
    int height = contents.height();
    BitSet bits = new BitSet(width * height);
    contents.getUniqueFrames().forEach(frame -> {
      for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
          if (!contents.isTransparent(frame, x, y)) {
            bits.set(x + y * width);
          }
        }
      }
    });
    // merge opaque pixels into rectangles, a front and a back quad each
    for (int y = 0; y < height; y++) {
      int xStart = -1;
      for (int x = 0; x < width; x++) {
        boolean opaque = bits.get(x + y * width);
        if (opaque == (xStart == -1)) {
          if (xStart == -1) {
            xStart = x;
            continue;
          }
          int yEnd = y + 1;
          expand:
          for (; yEnd < height; yEnd++) {
            for (int x2 = xStart; x2 <= x; x2++) {
              if (!bits.get(x2 + yEnd * width)) {
                break expand;
              }
            }
          }
          for (int i = xStart; i < x; i++) {
            for (int j = y; j < yEnd; j++) {
              bits.clear(i + j * width);
            }
          }
          Vector3f from = new Vector3f(16 * xStart / (float) width, 16 - 16 * yEnd / (float) height, 7.5f);
          Vector3f to = new Vector3f(16 * x / (float) width, 16 - 16 * y / (float) height, 8.5f);
          builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, FaceBakery.defaultFaceUV(from, to, Direction.NORTH), Quadrant.R0, info, Direction.NORTH, state, null));
          builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, FaceBakery.defaultFaceUV(from, to, Direction.SOUTH), Quadrant.R0, info, Direction.SOUTH, state, null));
          xStart = -1;
        }
      }
    }
    return builder.build();
  }

  /** Item render type of the contents (Fabric has no unlit item render types: glowing fluids keep their light level) */
  private static RenderType renderType(Material.Baked material) {
    boolean translucent = material.forceTranslucent() || material.sprite().transparency().hasTranslucent();
    return translucent ? Sheets.translucentBlockItemSheet() : Sheets.cutoutBlockItemSheet();
  }

  @Override
  public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    Object contents = switch (unbaked.contents) {
      case FLUID -> stack.getItem() instanceof FluidClayBucketItem bucket ? bucket.getFluid(stack) : Fluids.EMPTY;
      case BLOCK -> stack.getItem() instanceof SolidClayBucketItem bucket ? bucket.getBlock(stack) : Blocks.AIR;
    };
    cache.computeIfAbsent(contents, this::bake).update(renderState, stack, resolver, displayContext, level, owner, seed);
  }

  /** Model state with one more transformation (NeoForge has ComposedModelState) */
  private record Composed(ModelState parent, Transformation transformation) implements ModelState {
    Composed(ModelState parent, Transformation transformation) {
      this.parent = parent;
      this.transformation = parent.transformation().compose(transformation);
    }

    @Override
    public Matrix4fc faceTransformation(Direction side) {
      return parent.faceTransformation(side);
    }

    @Override
    public Matrix4fc inverseFaceTransformation(Direction side) {
      return parent.inverseFaceTransformation(side);
    }
  }

  /** Tint of the fluid in the bucket (NeoForge has FluidContentsTint) */
  private enum FluidTint implements ItemTintSource {
    INSTANCE;
    static final MapCodec<FluidTint> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity) {
      if (stack.getItem() instanceof FluidClayBucketItem bucket) {
        Fluid fluid = bucket.getFluid(stack);
        if (fluid != Fluids.EMPTY) {
          return KilnworkClientPlatform.get().tint(new FluidStack(fluid, FluidStack.BUCKET_VOLUME), level, null);
        }
      }
      return -1;
    }

    @Override
    public MapCodec<FluidTint> type() {
      return MAP_CODEC;
    }
  }

  /** JSON form of the model */
  public record Unbaked(Contents contents, Textures textures) implements ItemModel.Unbaked {
    public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
      Contents.CODEC.fieldOf("contents").forGetter(Unbaked::contents),
      Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures)
    ).apply(inst, Unbaked::new));

    @Override
    public MapCodec<Unbaked> type() {
      return MAP_CODEC;
    }

    @Override
    public ItemModel bake(BakingContext context, Matrix4fc transformation) {
      return new ClayBucketItemModelFabric(this, context, transformation);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(GENERATED);
    }
  }
}
