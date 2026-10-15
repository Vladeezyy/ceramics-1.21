package knightminer.ceramics.client.model;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import knightminer.ceramics.items.FluidClayBucketItem;
import knightminer.ceramics.items.SolidClayBucketItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
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
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.color.item.FluidContentsTint;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
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
  private static final RenderTypeGroup RENDER_TYPES = new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes::getUnsortedTranslucent);
  private static final RenderTypeGroup RENDER_TYPES_UNLIT = new RenderTypeGroup(ChunkSectionLayer.TRANSLUCENT, NeoForgeRenderTypes::getUnlitTranslucent);

  /** What the bucket holds */
  public enum Contents implements StringRepresentable {
    FLUID, BLOCK;
    public static final Codec<Contents> CODEC = StringRepresentable.fromEnum(Contents::values);

    @Override
    public String getSerializedName() {
      return name().toLowerCase(java.util.Locale.ROOT);
    }
  }

  /** Textures of the bucket, all on the item atlas */
  public record Textures(Identifier base, Identifier inner, Identifier fluid) {
    public static final Codec<Textures> CODEC = RecordCodecBuilder.create(inst -> inst.group(
      Identifier.CODEC.fieldOf("base").forGetter(Textures::base),
      Identifier.CODEC.fieldOf("inner").forGetter(Textures::inner),
      Identifier.CODEC.fieldOf("fluid").forGetter(Textures::fluid)
    ).apply(inst, Textures::new));
  }

  private final Unbaked unbaked;
  private final BakingContext context;
  private final ItemTransforms transforms;
  /** Baked model for each contents, the contents are fluids or blocks */
  private final Map<Object, ItemModel> cache = new IdentityHashMap<>();

  private ClayBucketItemModel(Unbaked unbaked, BakingContext context) {
    this.unbaked = unbaked;
    this.context = context;
    var generated = context.blockModelBaker().getModel(Identifier.withDefaultNamespace("item/generated"));
    if (generated == null) {
      throw new IllegalStateException("Failed to access item/generated model");
    }
    this.transforms = generated.getTopTransforms();
  }

  private TextureAtlasSprite itemSprite(Identifier texture) {
    return context.blockModelBaker().sprites().get(ClientHooks.getItemMaterial(texture), DEBUG_NAME);
  }

  /** Builds the model for the given contents, {@link Fluids#EMPTY} or {@link Blocks#AIR} for empty */
  private ItemModel bake(Object contents) {
    TextureAtlasSprite base = itemSprite(unbaked.textures.base);
    TextureAtlasSprite inner = itemSprite(unbaked.textures.inner);
    TextureAtlasSprite mask = itemSprite(unbaked.textures.fluid);
    ModelRenderProperties properties = new ModelRenderProperties(false, inner, transforms);
    ModelState state = BlockModelRotation.IDENTITY;

    // what goes inside, on the block atlas
    TextureAtlasSprite contentsSprite = null;
    int light = 0;
    boolean tintFluid = false;
    if (contents instanceof Fluid fluid && fluid != Fluids.EMPTY) {
      contentsSprite = context.blockModelBaker().sprites().get(ClientHooks.getBlockMaterial(IClientFluidTypeExtensions.of(fluid).getStillTexture()), DEBUG_NAME);
      light = fluid.getFluidType().getLightLevel();
      tintFluid = true;
      // if the fluid is lighter than air, turn the bucket upside down
      if (fluid.getFluidType().isLighterThanAir()) {
        state = new ComposedModelState(state, new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null));
      }
    } else if (contents instanceof Block block && block != Blocks.AIR) {
      contentsSprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).particleIcon();
      light = block.defaultBlockState().getLightEmission();
    }

    List<ItemModel> parts = new ArrayList<>();
    if (contentsSprite == null) {
      // if empty, just render the inner sprite, looks better
      List<BakedQuad> quads = UnbakedElementsHelper.bakeElements(UnbakedElementsHelper.createUnbakedItemElements(0, inner), $ -> inner, state);
      parts.add(new BlockModelWrapper(List.of(), quads, properties, RenderTypeHelper.detectItemModelRenderType(quads, RENDER_TYPES)));
    } else {
      // base is the outer cover, the only layer in full 3D
      List<BakedQuad> quads = new ArrayList<>(UnbakedElementsHelper.bakeElements(UnbakedElementsHelper.createUnbakedItemElements(0, base), $ -> base, state));
      // inner is behind the contents, about a pixel back so it is not cut off in hand
      quads.addAll(UnbakedElementsHelper.bakeElements(List.of(flatElement()), $ -> inner, new ComposedModelState(state, INNER_TRANSFORM)));
      parts.add(new BlockModelWrapper(List.of(), quads, properties, RenderTypeHelper.detectItemModelRenderType(quads, RENDER_TYPES)));

      // contents next, only the front and back
      TextureAtlasSprite sprite = contentsSprite;
      List<BakedQuad> contentQuads = UnbakedElementsHelper.bakeElements(UnbakedElementsHelper.createUnbakedItemMaskElements(0, mask), $ -> sprite, new ComposedModelState(state, FLUID_TRANSFORM))
        .stream().filter(quad -> quad.direction().getAxis() == Axis.Z).toList();
      if (light > 0) {
        int emission = light;
        contentQuads = contentQuads.stream().map(quad -> withLight(quad, emission)).toList();
      }
      RenderTypeGroup types = light > 0 ? RENDER_TYPES_UNLIT : RENDER_TYPES;
      parts.add(new BlockModelWrapper(tintFluid ? List.of(FluidContentsTint.INSTANCE) : List.of(), contentQuads, properties, RenderTypeHelper.detectItemModelRenderType(contentQuads, types)));
    }
    return new CompositeModel(parts);
  }

  /** A single front face covering the item, untinted */
  private static BlockElement flatElement() {
    BlockElementFace face = new BlockElementFace(null, -1, "layer0", new BlockElementFace.UVs(0, 0, 16, 16), com.mojang.math.Quadrant.R0);
    return new BlockElement(new Vector3f(0, 0, 8.5f), new Vector3f(16, 16, 8.5f), Map.of(Direction.SOUTH, face), null, false, 0);
  }

  /** Makes the quad glow with the light of the contents */
  private static BakedQuad withLight(BakedQuad quad, int light) {
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
      quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
      quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), Math.max(light, quad.lightEmission()),
      quad.bakedNormals(), quad.bakedColors(), quad.hasAmbientOcclusion());
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
    public ItemModel bake(BakingContext context) {
      return new ClayBucketItemModel(this, context);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {}
  }
}
