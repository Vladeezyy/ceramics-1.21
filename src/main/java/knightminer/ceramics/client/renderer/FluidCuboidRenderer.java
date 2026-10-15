package knightminer.ceramics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import slimeknights.mantle.client.render.FaucetFluid;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidCuboid.FluidFace;

/**
 * Fluid cuboid rendering, copied from Mantle's FluidRenderer and RenderingHelper (MIT, SlimeKnights) with the texture
 * coordinates fixed for 1.21: Mantle 1.21.1-1.12.10 draws a piece of the whole block atlas instead of the fluid.
 */
public final class FluidCuboidRenderer {
  private FluidCuboidRenderer() {}

  /**
   * Forces the UV to be between 0 and 1
   * @param value  Original value
   * @param upper  If true, this is the larger UV. Needed to enforce integer values end up at 1
   * @return  UV mapped between 0 and 1
   */
  private static float boundUV(float value, boolean upper) {
    value = value % 1;
    if (value == 0) {
      // if it lands exactly on the 0 bound, map that to 1 instead for the larger UV
      return upper ? 1 : 0;
    }
    // modulo returns a negative result if the input is negative, so add 1 to account for that
    return value < 0 ? (value + 1) : value;
  }

  /**
   * Adds a quad to the renderer
   * @param renderer    Renderer instnace
   * @param matrix      Render matrix
   * @param sprite      Sprite to render
   * @param from        Quad start
   * @param to          Quad end
   * @param face        Face to render
   * @param color       Color to use in rendering
   * @param brightness  Face brightness
   * @param flowing     If true, half texture coordinates
   */
  public static void putTexturedQuad(VertexConsumer renderer, Matrix4f matrix, TextureAtlasSprite sprite, Vector3f from, Vector3f to, Direction face, int color, int brightness, int rotation, boolean flowing) {
    // start with texture coordinates
    float x1 = from.x(), y1 = from.y(), z1 = from.z();
    float x2 = to.x(), y2 = to.y(), z2 = to.z();
    // choose UV based on the directions, some need to negate UV due to the direction
    // note that we use -UV instead of 1-UV as its slightly simpler and the later logic deals with negatives
    float u1, u2, v1, v2;
    switch (face) {
      default -> { // DOWN
        u1 = x1; u2 = x2;
        v1 = z2; v2 = z1;
      }
      case UP -> {
        u1 = x1; u2 = x2;
        v1 = -z1; v2 = -z2;
      }
      case NORTH -> {
        u1 = -x1; u2 = -x2;
        v1 = y1; v2 = y2;
      }
      case SOUTH -> {
        u1 = x2; u2 = x1;
        v1 = y1; v2 = y2;
      }
      case WEST -> {
        u1 = z2; u2 = z1;
        v1 = y1; v2 = y2;
      }
      case EAST -> {
        u1 = -z1; u2 = -z2;
        v1 = y1; v2 = y2;
      }
    }

    // flip V when relevant
    if (rotation == 0 || rotation == 270) {
      float temp = v1;
      v1 = -v2;
      v2 = -temp;
    }
    // flip U when relevant
    if (rotation >= 180) {
      float temp = u1;
      u1 = -u2;
      u2 = -temp;
    }

    // bound UV to be between 0 and 1
    boolean reverse = u1 > u2;
    u1 = boundUV(u1, reverse);
    u2 = boundUV(u2, !reverse);
    reverse = v1 > v2;
    v1 = boundUV(v1, reverse);
    v2 = boundUV(v2, !reverse);

    // if rotating by 90 or 270, swap U and V
    float minU, maxU, minV, maxV;
    // 1.21: getU and getV take a 0-1 fraction of the sprite, Mantle 1.21.1-1.12.10 still passes 0-16 (1.20)
    float size = flowing ? 0.5f : 1f;
    if ((rotation % 180) == 90) {
      minU = sprite.getU(v1 * size);
      maxU = sprite.getU(v2 * size);
      minV = sprite.getV(u1 * size);
      maxV = sprite.getV(u2 * size);
    } else {
      minU = sprite.getU(u1 * size);
      maxU = sprite.getU(u2 * size);
      minV = sprite.getV(v1 * size);
      maxV = sprite.getV(v2 * size);
    }
    // based on rotation, put coords into place
    float u3, u4, v3, v4;
    switch(rotation) {
      default -> { // 0
        u1 = minU; v1 = maxV;
        u2 = minU; v2 = minV;
        u3 = maxU; v3 = minV;
        u4 = maxU; v4 = maxV;
      }
      case 90 -> {
        u1 = minU; v1 = minV;
        u2 = maxU; v2 = minV;
        u3 = maxU; v3 = maxV;
        u4 = minU; v4 = maxV;
      }
      case 180 -> {
        u1 = maxU; v1 = minV;
        u2 = maxU; v2 = maxV;
        u3 = minU; v3 = maxV;
        u4 = minU; v4 = minV;
      }
      case 270 -> {
        u1 = maxU; v1 = maxV;
        u2 = minU; v2 = maxV;
        u3 = minU; v3 = minV;
        u4 = maxU; v4 = minV;
      }
    }
    // add quads
    int light1 = brightness & 0xFFFF;
    int light2 = brightness >> 0x10 & 0xFFFF;
    int a = color >> 24 & 0xFF;
    int r = color >> 16 & 0xFF;
    int g = color >> 8 & 0xFF;
    int b = color & 0xFF;
    switch (face) {
      case DOWN -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
      case UP -> {
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
      case NORTH -> {
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
      case SOUTH -> {
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
      case WEST -> {
        renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
      case EAST -> {
        renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setUv2(light1, light2);
        renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setUv2(light1, light2);
      }
    }
  }

  /**
   * Renders a full fluid cuboid for the given data
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer type
   * @param still     Still sprite
   * @param flowing   Flowing sprite
   * @param cube      Fluid cuboid
   * @param from      Fluid start
   * @param to        Fluid end
   * @param color     Fluid color
   * @param light     Quad lighting
   * @param isGas     If true, fluid is a gas
   */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, boolean isGas) {
    Matrix4f matrix = matrices.last().pose();
    int rotation = isGas ? 180 : 0;
    for (Direction dir : Direction.values()) {
      FluidFace face = cube.getFace(dir);
      if (face != null) {
        boolean isFlowing = face.isFlowing();
        int faceRot = (rotation + face.rotation()) % 360;
        putTexturedQuad(buffer, matrix, isFlowing ? flowing : still, from, to, dir, color, light, faceRot, isFlowing);
      }
    }
  }

  /**
   * Renders a fluid cuboid with the given offset, used to manually place cuboids from a list for rendering renderCuboids in Mantle
   * @param matrices  Matrix stack instance
   * @param buffer    Buffer type
   * @param cube      Fluid cuboid
   * @param yOffset   Amount to offset the cube in the Y direction, used in faucets for rendering fluid in lower block
   * @param still     Still sprite
   * @param flowing   Flowing sprite
   * @param color     Fluid color
   * @param light     Quad lighting from TER
   * @param isGas     If true, fluid is a gas
   */
  public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float yOffset, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, boolean isGas) {
    if (yOffset != 0) {
      matrices.pushPose();
      matrices.translate(0, yOffset, 0);
    }
    renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, isGas);
    if (yOffset != 0) {
      matrices.popPose();
    }
  }

  /**
   * Renders the fluids of a faucet or channel pouring into the block(s) below, from Mantle's RenderingHelper
   * @param world      World instance
   * @param pos        Base position
   * @param direction  Direction to render
   * @param matrices   Matrix instance
   * @param buffer     Builder instance
   * @param still      Still fluid texture
   * @param flowing    Flowing fluid texture
   * @param color      Color to tint fluid
   * @param light      Fluid light value
   */
  public static void renderFaucetFluids(LevelAccessor world, BlockPos pos, Direction direction, PoseStack matrices, VertexConsumer buffer, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light) {
    int i = 0;
    FaucetFluid faucetFluid;
    do {
      // get the faucet data for the block
      i++;
      faucetFluid = FaucetFluid.REGISTRY.get(world.getBlockState(pos.below(i)));
      // render all down cubes with the given offset
      matrices.pushPose();
      matrices.translate(0, -i, 0);
      for (FluidCuboid cube : faucetFluid.getFluids(direction)) {
        renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, false);
      }
      matrices.popPose();
    } while (faucetFluid.isContinued());
  }
}
