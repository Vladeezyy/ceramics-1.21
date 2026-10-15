package knightminer.ceramics.client.screen;

import net.minecraft.client.gui.components.ImageButton;
import knightminer.ceramics.menu.KilnMenu;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class KilnScreen extends AbstractFurnaceScreen<KilnMenu> {
  private static final ResourceLocation FURNACE_GUI_TEXTURES = ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
  private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
  private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

  public KilnScreen(KilnMenu kiln, Inventory inventory, Component name) {
    super(kiln, new SmeltingRecipeBookComponent(), inventory, name, FURNACE_GUI_TEXTURES, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE);
  }

  @Override
  public void init() {
    super.init();
    // remove the recipe book button as the kiln has no recipe book. 1.20 only took it out of the renderables, so the
    // invisible button could still be clicked
    this.children().stream().filter(child -> child instanceof ImageButton).toList().forEach(this::removeWidget);
    if (this.recipeBookComponent.isVisible()) {
      this.recipeBookComponent.toggleVisibility();
    }
  }
}