package knightminer.ceramics;

import knightminer.ceramics.network.CeramicsNetwork;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/** Ceramics: ids and the registrations of the mod, the same on every loader; the loader parts call {@link #init()} */
@SuppressWarnings("WeakerAccess")
public class Ceramics {
	public static final String MOD_ID = "ceramics";
	public static final Logger LOG = LogManager.getLogger(MOD_ID);

	public static void init() {
		Registration.init();
		CeramicsTags.init();
		CeramicsNetwork.register();
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
