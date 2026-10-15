package knightminer.ceramics.client.screen;

import knightminer.ceramics.menu.KilnMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Kiln screen, the furnace without the recipe book. Since 1.21.2 the furnace screen owns its book privately, so this
 * draws the furnace background itself instead of hiding the book after the fact.
 */
public class KilnScreen extends AbstractContainerScreen<KilnMenu> {
  private static final Identifier FURNACE_GUI_TEXTURES = Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
  private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
  private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

  public KilnScreen(KilnMenu kiln, Inventory inventory, Component name) {
    super(kiln, inventory, name);
  }

  @Override
  public void init() {
    super.init();
    this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    this.renderTooltip(graphics, mouseX, mouseY);
  }

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    int x = this.leftPos;
    int y = this.topPos;
    graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_GUI_TEXTURES, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    if (this.menu.isLit()) {
      int lit = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - lit, x + 56, y + 36 + 14 - lit, 14, lit);
    }
    int burn = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, x + 79, y + 34, burn, 16);
  }
}
