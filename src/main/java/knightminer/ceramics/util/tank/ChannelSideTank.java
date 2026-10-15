package knightminer.ceramics.util.tank;

import knightminer.ceramics.blocks.entity.ChannelBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Tank for each side connection, for the sake of rendering: marks the side flowing once a fill commits */
public class ChannelSideTank extends FillOnlyFluidHandler {
  private final ChannelBlockEntity channel;
  private final Direction side;
  private final FlowJournal journal = new FlowJournal();

  public ChannelSideTank(ChannelBlockEntity channel, ChannelTank tank, Direction side) {
    super(tank);
    // only horizontals
    assert side.getAxis() != Axis.Y;
    this.channel = channel;
    this.side = side;
  }

  @Override
  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    int filled = super.insert(index, resource, amount, transaction);
    if (filled > 0) {
      journal.mark(transaction);
    }
    return filled;
  }

  /** Remembers a fill in the transaction, marks the flow on commit */
  private class FlowJournal extends SnapshotJournal<Boolean> {
    private boolean filled;

    void mark(TransactionContext transaction) {
      updateSnapshots(transaction);
      filled = true;
    }

    @Override
    protected Boolean createSnapshot() {
      return filled;
    }

    @Override
    protected void revertToSnapshot(Boolean snapshot) {
      filled = snapshot;
    }

    @Override
    protected void onRootCommit(Boolean original) {
      if (filled) {
        filled = false;
        channel.setFlow(side, true);
      }
    }
  }
}
