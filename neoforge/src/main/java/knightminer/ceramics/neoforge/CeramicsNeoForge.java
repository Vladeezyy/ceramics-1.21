package knightminer.ceramics.neoforge;

import dev.vladeezyy.kilnwork.neoforge.KilnworkNeoForge;
import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import knightminer.ceramics.datagen.BlockTagProvider;
import knightminer.ceramics.datagen.FluidTagProvider;
import knightminer.ceramics.datagen.ItemTagProvider;
import knightminer.ceramics.datagen.LootTableProvider;
import knightminer.ceramics.datagen.RecipeProvider;
import knightminer.ceramics.recipe.NoNBTIngredient;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.concurrent.CompletableFuture;

/** NeoForge entry of Ceramics */
@Mod(Ceramics.MOD_ID)
public class CeramicsNeoForge {
	private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Ceramics.MOD_ID);
	public static final DeferredHolder<IngredientType<?>, IngredientType<NoNBTIngredient>> NO_NBT_INGREDIENT = INGREDIENT_TYPES.register("no_nbt", () -> new IngredientType<>(NoNBTIngredient.CODEC, NoNBTIngredient.STREAM_CODEC));

	public CeramicsNeoForge(IEventBus bus, ModContainer container) {
		Ceramics.init();
		KilnworkNeoForge.register(Ceramics.MOD_ID, bus);
		INGREDIENT_TYPES.register(bus);
		bus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(Registration::setup));
		bus.addListener(this::gatherData);
	}

	/** Server data, since 1.21.4 the client and server data run as separate events */
	private void gatherData(GatherDataEvent.Server event) {
		DataGenerator gen = event.getGenerator();
		PackOutput packOutput = gen.getPackOutput();
		CompletableFuture<Provider> lookupProvider = event.getLookupProvider();

		BlockTagProvider blockTags = new BlockTagProvider(packOutput, lookupProvider);
		gen.addProvider(true, blockTags);
		gen.addProvider(true, new ItemTagProvider(packOutput, lookupProvider, blockTags.contentsGetter()));
		gen.addProvider(true, new FluidTagProvider(packOutput, lookupProvider));
		gen.addProvider(true, new RecipeProvider.Runner(packOutput, lookupProvider));
		gen.addProvider(true, new LootTableProvider(packOutput, lookupProvider));
		// the fluid shapes of faucets, channels and cisterns are plain JSON in src/main/resources (were Mantle datagen)
	}
}
