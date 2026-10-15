package knightminer.ceramics;

import knightminer.ceramics.datagen.BlockTagProvider;
import knightminer.ceramics.datagen.FluidTagProvider;
import knightminer.ceramics.datagen.ItemTagProvider;
import knightminer.ceramics.datagen.BlockLootTables;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import knightminer.ceramics.datagen.RecipeProvider;
import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("WeakerAccess")
@Mod(Ceramics.MOD_ID)
public class Ceramics {
	public static final String MOD_ID = "ceramics";
	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	public Ceramics(IEventBus bus, ModContainer container) {
		Registration.init(bus);
		CeramicsTags.init();
		bus.addListener(CeramicsNetwork::registerPackets);
		bus.addListener(this::gatherData);
		bus.addListener(this::gatherRegistries);
	}

	/** Recipes, their advancements and loot tables, since 26.3 datapack registry entries */
	private void gatherRegistries(GatherDataRegistryEntriesEvent event) {
		event.recipe(RecipeProvider::new)
			.lootTable(new LootTableProvider.SubProviderEntry(BlockLootTables::new, LootContextParamSets.BLOCK));
	}

	/** Tags, since 1.21.4 the client and server data run as separate events */
	private void gatherData(GatherDataEvent.Server event) {
		DataGenerator gen = event.getGenerator();
		PackOutput packOutput = gen.getPackOutput();
		CompletableFuture<Provider> lookupProvider = event.getReloadableLookupProvider();

		BlockTagProvider blockTags = new BlockTagProvider(packOutput, lookupProvider);
		gen.addProvider(true, blockTags);
		gen.addProvider(true, new ItemTagProvider(packOutput, lookupProvider, blockTags.contentsGetter()));
		gen.addProvider(true, new FluidTagProvider(packOutput, lookupProvider));
		// the fluid shapes of faucets, channels and cisterns are plain JSON in src/main/resources (were Mantle datagen)
	}


	/**
	 * Gets a resource locations as a string
	 * @param name  Name for the location
	 * @return  Resource location string
	 */
	public static String locationName(String name) {
		return MOD_ID + ":" + name;
	}

	/**
	 * Gets a resource location at the Ceramics namespace
	 * @param name  Resource path
	 * @return  Resource location for Ceramics
	 */
	public static Identifier getResource(String name) {
	  return Identifier.fromNamespaceAndPath(MOD_ID, name);
	}

	/**
	 * Forms the mod ID into a language key
	 * @param group Language key group
	 * @param name Name within group
	 * @return Language key
	 */
	public static String lang(String group, String name) {
		return String.format("%s.%s.%s", group, MOD_ID, name);
	}

	/**
	 * Forms a translation component with the following details
	 * @param group Language key group
	 * @param name Name within group
	 * @return Language key
	 */
	public static MutableComponent component(String group, String name) {
		return Component.translatable(lang(group, name));
	}
}
