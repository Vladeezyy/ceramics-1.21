package knightminer.ceramics.items;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.Registration;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/** Armor materials, in 1.21 a registry entry instead of an enum */
public final class ArmorMaterials {
  private ArmorMaterials() {}

  /** Durability multiplier for clay armor, same as the 1.20 enum */
  private static final int CLAY_DURABILITY_FACTOR = 4;

  /** Creates the clay material: protection 1/3/2/1, enchantability 7, no toughness */
  public static ArmorMaterial createClay() {
    return new ArmorMaterial(
      Util.make(new EnumMap<>(Type.class), map -> {
        map.put(Type.HELMET, 1);
        map.put(Type.CHESTPLATE, 3);
        map.put(Type.LEGGINGS, 2);
        map.put(Type.BOOTS, 1);
        map.put(Type.BODY, 3);
      }),
      7, SoundEvents.ARMOR_EQUIP_GENERIC, () -> Ingredient.of(Registration.CLAY_PLATE),
      List.of(new ArmorMaterial.Layer(Ceramics.getResource("clay"))),
      0.0f, 0.0f);
  }

  /** Item properties for the given clay armor piece, durability is no longer on the material */
  public static Item.Properties armorProps(ArmorItem.Type type) {
    return new Item.Properties().stacksTo(1).durability(type.getDurability(CLAY_DURABILITY_FACTOR));
  }
}
