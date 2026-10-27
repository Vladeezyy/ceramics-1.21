package knightminer.ceramics.client.renderer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vladeezyy.kilnwork.client.render.FluidCuboid;
import dev.vladeezyy.kilnwork.data.BlockDataLoader;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.Map;

/** JSON element representing fluids in a cistern, {@code assets/<namespace>/ceramics/cistern_fluids/<block>.json} */
public record CisternFluids(FluidCuboid center, FluidCuboid extension, Map<Direction,FluidCuboid> sides) {
  public static final Codec<CisternFluids> CODEC = RecordCodecBuilder.create(inst -> inst.group(
    FluidCuboid.CODEC.fieldOf("center").forGetter(CisternFluids::center),
    FluidCuboid.CODEC.fieldOf("extension").forGetter(CisternFluids::extension),
    Codec.unboundedMap(Direction.CODEC, FluidCuboid.CODEC).fieldOf("sides").forGetter(CisternFluids::sides)
  ).apply(inst, CisternFluids::new));
  public static final BlockDataLoader<CisternFluids> REGISTRY = new BlockDataLoader<>("cistern fluids", "ceramics/cistern_fluids", CODEC);

  /** Gets a side fluid */
  @Nullable
  public FluidCuboid side(Direction direction) {
    if (direction.getAxis().isVertical()) {
      throw new IllegalStateException("Direction must be horizontal");
    }
    return sides.get(direction);
  }

  /** Gets the center for either extension or non-extension */
  public FluidCuboid base(boolean extension) {
    return extension ? this.extension : this.center;
  }
}
