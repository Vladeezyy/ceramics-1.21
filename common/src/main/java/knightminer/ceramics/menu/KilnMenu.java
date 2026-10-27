package knightminer.ceramics.menu;

import knightminer.ceramics.Registration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class KilnMenu extends AbstractFurnaceMenu {
  private final Level level;

  @SuppressWarnings("unused")
  public KilnMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
    super(Registration.KILN_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, id, inventory);
    this.level = inventory.player.level();
  }

  public KilnMenu(int id, Inventory inventory, Container tileEntity, ContainerData furnaceData) {
    super(Registration.KILN_MENU.get(), RecipePropertySet.FURNACE_INPUT, RecipeBookType.FURNACE, id, inventory, tileEntity, furnaceData);
    this.level = inventory.player.level();
  }

  /**
   * Since 1.21.2 the menu asks a synced set of inputs, and only vanilla has those sets. The server checks the kiln
   * recipes directly, the client guesses with the furnace inputs until the server corrects it.
   */
  @Override
  protected boolean canSmelt(ItemStack stack) {
    if (level instanceof ServerLevel server) {
      return server.recipeAccess().getRecipeFor(Registration.KILN_RECIPE.get(), new SingleRecipeInput(stack), server).isPresent();
    }
    return super.canSmelt(stack);
  }
}
