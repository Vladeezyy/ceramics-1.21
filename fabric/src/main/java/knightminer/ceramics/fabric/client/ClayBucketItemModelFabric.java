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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
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
import java.util.Map;
import java.util.function.Function;

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
  private static final ModelBaker.PartCache NO_CACHE = vector -> vector;
  /** Fluid is slightly inside the cover */
  private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 0.998f), new Quaternionf());
  /** Inner texture is half a pixel back */
  private static final Transformation INNER_TRANSFORM = new Transformation(new Vector3f(0, 0, -0.5f / 16f), new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf());
  /** Lighter than air fluids turn the bucket upside down */
  private static final Transformation UPSIDE_DOWN = new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null);
  private static final Function<ItemStack, RenderType> ITEM_ATLAS = stack -> Sheets.translucentItemSheet();
  private static final Function<ItemStack, RenderType> BLOCK_ATLAS = stack -> Sheets.translucentBlockItemSheet();

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

  private ClayBucketItemModelFabric(Unbaked unbaked, BakingContext context) {
    this.unbaked = unbaked;
    this.context = context;
    this.transforms = context.blockModelBaker().getModel(GENERATED).getTopTransforms();
  }

  private TextureAtlasSprite itemSprite(Identifier texture) {
    return context.blockModelBaker().sprites().get(new Material(TextureAtlas.LOCATION_ITEMS, texture), DEBUG_NAME);
  }

  /** Builds the model for the given contents, {@link Fluids#EMPTY} or {@link Blocks#AIR} for empty */
  private ItemModel bake(Object contents) {
    TextureAtlasSprite base = itemSprite(unbaked.textures.base);
    TextureAtlasSprite inner = itemSprite(unbaked.textures.inner);
    TextureAtlasSprite mask = itemSprite(unbaked.textures.fluid);
    ModelRenderProperties properties = new ModelRenderProperties(false, inner, transforms);
    Transformation root = Transformation.identity();

    // what goes inside, on the block atlas
    TextureAtlasSprite contentsSprite = null;
    int light = 0;
    boolean tintFluid = false;
    if (contents instanceof Fluid fluid && fluid != Fluids.EMPTY) {
      contentsSprite = KilnworkClientPlatform.get().stillSprite(new FluidStack(fluid, FluidStack.BUCKET_VOLUME));
      light = KilnworkPlatform.get().fluidLightLevel(new FluidStack(fluid, FluidStack.BUCKET_VOLUME));
      tintFluid = true;
      if (FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid))) {
        root = UPSIDE_DOWN;
      }
    } else if (contents instanceof Block block && block != Blocks.AIR) {
      contentsSprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(block.defaultBlockState()).particleIcon();
      light = block.defaultBlockState().getLightEmission();
    }

    List<ItemModel> parts = new ArrayList<>();
    if (contentsSprite == null) {
      // if empty, just render the inner sprite, looks better
      parts.add(new BlockModelWrapper(List.of(), bake(itemElements(inner), inner, root), properties, ITEM_ATLAS));
    } else {
      // base is the outer cover, the only layer in full 3D; inner is behind the contents, about a pixel back so it is
      // not cut off in hand
      List<BakedQuad> quads = new ArrayList<>(bake(itemElements(base), base, root));
      quads.addAll(bake(List.of(flatElement()), inner, root.compose(INNER_TRANSFORM)));
      parts.add(new BlockModelWrapper(List.of(), quads, properties, ITEM_ATLAS));

      // contents next, only the front and back
      int emission = light;
      List<BakedQuad> contentQuads = bake(maskElements(mask), contentsSprite, root.compose(FLUID_TRANSFORM)).stream()
        .filter(quad -> quad.direction().getAxis() == Axis.Z)
        .map(quad -> emission > 0 ? withLight(quad, emission) : quad).toList();
      boolean blockAtlas = contentsSprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS);
      parts.add(new BlockModelWrapper(tintFluid ? List.of(FluidTint.INSTANCE) : List.of(), contentQuads, properties, blockAtlas ? BLOCK_ATLAS : ITEM_ATLAS));
    }
    return new CompositeModel(parts);
  }

  /** Elements of a generated item for the sprite: front and back faces plus the edges of the opaque pixels */
  private static List<BlockElement> itemElements(TextureAtlasSprite sprite) {
    return ItemModelGenerator.processFrames(0, "layer0", sprite.contents());
  }

  /** Like {@link #itemElements}, but the front and back cover only the opaque pixels of the sprite */
  private static List<BlockElement> maskElements(TextureAtlasSprite sprite) {
    List<BlockElement> elements = new ArrayList<>(itemElements(sprite));
    elements.removeFirst(); // the full front and back faces
    SpriteContents contents = sprite.contents();
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
    // merge opaque pixels into rectangles, one element each
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
          elements.add(new BlockElement(from, to, Map.of(
            Direction.NORTH, new BlockElementFace(null, 0, "layer0", null, Quadrant.R0),
            Direction.SOUTH, new BlockElementFace(null, 0, "layer0", null, Quadrant.R0)), null, true, 0));
          xStart = -1;
        }
      }
    }
    return elements;
  }

  /** Bakes all faces of the elements with one sprite */
  private static List<BakedQuad> bake(List<BlockElement> elements, TextureAtlasSprite sprite, Transformation transform) {
    ModelState state = transform.equals(Transformation.identity()) ? BlockModelRotation.IDENTITY : new TransformState(transform);
    List<BakedQuad> quads = new ArrayList<>();
    for (BlockElement element : elements) {
      element.faces().forEach((side, face) -> quads.add(FaceBakery.bakeQuad(NO_CACHE, element.from(), element.to(), face, sprite, side, state, element.rotation(), element.shade(), element.lightEmission())));
    }
    return quads;
  }

  /** A single front face covering the item, untinted */
  private static BlockElement flatElement() {
    BlockElementFace face = new BlockElementFace(null, -1, "layer0", new BlockElementFace.UVs(0, 0, 16, 16), Quadrant.R0);
    return new BlockElement(new Vector3f(0, 0, 8.5f), new Vector3f(16, 16, 8.5f), Map.of(Direction.SOUTH, face), null, false, 0);
  }

  /** Makes the quad glow with the light of the contents */
  private static BakedQuad withLight(BakedQuad quad, int light) {
    return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
      quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
      quad.tintIndex(), quad.direction(), quad.sprite(), quad.shade(), Math.max(light, quad.lightEmission()));
  }

  @Override
  public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
    Object contents = switch (unbaked.contents) {
      case FLUID -> stack.getItem() instanceof FluidClayBucketItem bucket ? bucket.getFluid(stack) : Fluids.EMPTY;
      case BLOCK -> stack.getItem() instanceof SolidClayBucketItem bucket ? bucket.getBlock(stack) : Blocks.AIR;
    };
    cache.computeIfAbsent(contents, this::bake).update(renderState, stack, resolver, displayContext, level, owner, seed);
  }

  /** Model state of a plain transformation */
  private record TransformState(Transformation transformation) implements ModelState {
    @Override
    public Matrix4fc faceTransformation(Direction side) {
      return NO_TRANSFORM;
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
    public ItemModel bake(BakingContext context) {
      return new ClayBucketItemModelFabric(this, context);
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
      resolver.markDependency(GENERATED);
    }
  }
}
