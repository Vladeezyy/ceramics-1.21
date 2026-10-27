package knightminer.ceramics.fabric.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import knightminer.ceramics.Ceramics;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The model loaders of Ceramics on Fabric: model JSON with {@code "loader": "ceramics:cracked"} are vanilla block models
 * wrapped before baking (also the models that use one as parent, such as item models), clay bucket models are
 * replaced when loaded (their parent {@code neoforge:item/default} does not exist on Fabric).
 */
public final class CeramicsModelsFabric {
  private CeramicsModelsFabric() {}

  private record Loaded(Set<String> cracked, Map<ResourceLocation, UnbakedModel> buckets) {}

  public static void register() {
    PreparableModelLoadingPlugin.register(CeramicsModelsFabric::load, (data, context) -> {
      context.modifyModelOnLoad().register((model, ctx) -> {
        ResourceLocation id = ctx.resourceId();
        UnbakedModel bucket = id == null ? null : data.buckets().get(id);
        return bucket != null ? bucket : model;
      });
      context.modifyModelBeforeBake().register((model, ctx) -> {
        if (model instanceof BlockModel blockModel) {
          for (BlockModel current = blockModel; current != null; current = current.parent) {
            if (data.cracked().contains(current.name)) {
              return new CrackedModelFabric(blockModel);
            }
          }
        }
        return model;
      });
    });
  }

  private static CompletableFuture<Loaded> load(ResourceManager manager, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      Set<String> cracked = new HashSet<>();
      Map<ResourceLocation, UnbakedModel> buckets = new HashMap<>();
      Map<ResourceLocation, Resource> resources = manager.listResources("models", id -> id.getNamespace().equals(Ceramics.MOD_ID) && id.getPath().endsWith(".json"));
      resources.forEach((file, resource) -> {
        try (Reader reader = resource.openAsReader()) {
          JsonElement json = JsonParser.parseReader(reader);
          if (json.isJsonObject() && json.getAsJsonObject().has("loader")) {
            JsonObject object = json.getAsJsonObject();
            String path = file.getPath();
            ResourceLocation modelId = file.withPath(path.substring("models/".length(), path.length() - ".json".length()));
            switch (object.get("loader").getAsString()) {
              case "ceramics:cracked" -> cracked.add(modelId.toString());
              case "ceramics:fluid_bucket" -> buckets.put(modelId, new ClayBucketModelFabric(object, false));
              case "ceramics:solid_bucket" -> buckets.put(modelId, new ClayBucketModelFabric(object, true));
              default -> {}
            }
          }
        } catch (Exception e) {
          Ceramics.LOG.error("Could not read the model {}", file, e);
        }
      });
      return new Loaded(cracked, buckets);
    }, executor);
  }
}
