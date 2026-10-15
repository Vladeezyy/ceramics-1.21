package knightminer.ceramics.client.model;

import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Item model of the clay buckets, replacing the geometry loaders {@code ceramics:fluid_bucket} and
 * {@code ceramics:solid_bucket} of 1.20 since item models are their own JSON since 1.21.4.
 * <p>
 * The outer cover ({@code base}) is the full 3D item, the {@code inner} texture is a flat quad half a pixel behind it,
 * and the contents are drawn inside, masked by {@code fluid}. Unlike the NeoForge fluid container the cover is outside
 * of the contents, so transparent fluids look right.
 * <pre>{"type": "ceramics:clay_bucket", "contents": "fluid", "textures": {"base": ..., "inner": ..., "fluid": ...}}</pre>
 */
public class ClayBucketItemModel implements ItemModel {
  public static final Identifier ID = Identifier.fromNamespaceAndPath("ceramics", "clay_bucket");
  private static final ModelDebugName DEBUG_NAME = () -> "ClayBucketItemModel";
  /** Fluid is slightly inside the cover */
  private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 0.998f), new Quaternionf());
  /** Inner texture is half a pixel back */
  private static final Transformation INNER_TRANSFORM = new Transformation(new Vector3f(0, 0, -0.5f / 16f), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf());

  /** What the bucket holds */
  public enum Contents implements StringRepresentable {
    FLUID, BLOCK;
    public static final Codec<Contents> CODEC = StringRepresentable.fromEnum(Contents::values);

    @Override
    public String getSerializedName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /** Textures of the bucket */
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

  private ClayBucketItemModel(Unbaked unbaked, BakingContext context, Matrix4fc transformation) {
    this.unbaked = unbaked;
    this.context = context;
    this.transformation = transformation;
    this.transforms = context.blockModelBaker().getModel(Identifier.withDefaultNamespace("item/generated")).getTopTransforms();
  }

  /** Builds the model for the given contents, {@link Fluids#EMPTY} or {@link Blocks#AIR} for empty */
  private ItemModel bake(Object contents) {
    ModelBaker baker = context.blockModelBaker();
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
      contentsMaterial = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial();
      light = fluid.getFluidType().getLightLevel();
      tintFluid = true;
      // if the fluid is lighter than air, turn the bucket upside down
      if (fluid.getFluidType().isLighterThanAir()) {
        state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
      }
    } else if (contents instanceof Block block && block != Blocks.AIR) {
      contentsMaterial = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(block.defaultBlockState());
      light = block.defaultBlockState().getLightEmission();
    }

    List<ItemModel> parts = new ArrayList<>();
    if (contentsMaterial == null) {
      // if empty, just render the inner sprite, looks better
      parts.add(new CuboidItemModelWrapper(List.of(), baker.compute(new ItemModelGenerator.ItemLayerKey(inner, state, 0)), properties, transformation));
    } else {
      // base is the outer cover, the only layer in full 3D
      QuadCollection.Builder quads = new QuadCollection.Builder();
      quads.addAll(baker.compute(new ItemModelGenerator.ItemLayerKey(base, state, 0)));
      // inner is behind the contents, about a pixel back so it is not cut off in hand
      for (BakedQuad quad : UnbakedElementsHelper.bakeElements(baker, List.of(flatElement()), $ -> inner, new ComposedModelState(state, INNER_TRANSFORM))) {
        quads.addUnculledFace(quad);
      }
      parts.add(new CuboidItemModelWrapper(List.of(), quads.build(), properties, transformation));

      // contents next, only the front and back
      boolean emissive = light > 0;
      RenderTypes types = renderTypes(contentsMaterial, emissive);
      BakedQuad.MaterialInfo info = baker.interner().materialInfo(new BakedQuad.MaterialInfo(
        contentsMaterial.sprite(), ChunkSectionLayer.SOLID, types.item, types.glint, types.glintSpecial, 0, emissive ? Direction.UP : null, light, !emissive));
      QuadCollection masked = UnbakedElementsHelper.bakeItemMaskQuads(baker, mask, info, new ComposedModelState(state, FLUID_TRANSFORM), ExtraFaceData.DEFAULT);
      QuadCollection.Builder contentQuads = new QuadCollection.Builder();
      for (BakedQuad quad : masked.getAll()) {
        if (quad.direction().getAxis() == Axis.Z) {
          contentQuads.addUnculledFace(quad);
        }
      }
      parts.add(new CuboidItemModelWrapper(tintFluid ? List.of(FluidContentsTint.INSTANCE) : List.of(), contentQuads.build(), properties, transformation));
    }
    return new CompositeModel(parts);
  }

  /** Render types of an item quad, with and without enchantment glint */
  private record RenderTypes(RenderType item, RenderType glint, RenderType glintSpecial) {}

  /** Render types of the contents, they are on the block atlas while the bucket is on the item atlas */
  private static RenderTypes renderTypes(Material.Baked material, boolean emissive) {
    boolean translucent = material.forceTranslucent() || material.sprite().transparency().hasTranslucent();
    Identifier atlas = TextureAtlas.LOCATION_BLOCKS;
    if (translucent) {
      return emissive
        ? new RenderTypes(NeoForgeRenderTypes.getItemTranslucentUnlit(atlas), NeoForgeRenderTypes.getItemGlintTranslucentUnlit(atlas), NeoForgeRenderTypes.getItemGlintSpecialTranslucentUnlit(atlas))
        : new RenderTypes(Sheets.translucentBlockItemSheet(), Sheets.translucentBlockItemGlintSheet(), Sheets.translucentBlockItemGlintSpecialSheet());
    }
    return emissive
      ? new RenderTypes(NeoForgeRenderTypes.getItemCutoutUnlit(atlas), NeoForgeRenderTypes.getItemGlintCutoutUnlit(atlas), NeoForgeRenderTypes.getItemGlintSpecialCutoutUnlit(atlas))
      : new RenderTypes(Sheets.cutoutBlockItemSheet(), Sheets.cutoutBlockItemGlintSheet(), Sheets.cutoutBlockItemGlintSpecialSheet());
  }

  /** A single front face covering the item, untinted and lit as a top face (unshaded) */
  private static CuboidModelElement flatElement() {
    CuboidFace face = new CuboidFace(null, -1, "layer0", new CuboidFace.UVs(0, 0, 16, 16), Quadrant.R0);
    return new CuboidModelElement(new Vector3f(0, 0, 8.5f), new Vector3f(16, 16, 8.5f), Map.of(Direction.SOUTH, face), null, Direction.UP, 0);
  }

  @Override
  public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    Object contents = switch (unbaked.contents) {
      case FLUID -> stack.getItem() instanceof FluidClayBucketItem bucket ? bucket.getFluid(stack) : Fluids.EMPTY;
      case BLOCK -> stack.getItem() instanceof SolidClayBucketItem bucket ? bucket.getBlock(stack) : Blocks.AIR;
    };
    cache.computeIfAbsent(contents, this::bake).update(renderState, stack, resolver, displayContext, level, owner, seed);
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
      return new ClayBucketItemModel(this, context, transformation);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {}
  }
}
