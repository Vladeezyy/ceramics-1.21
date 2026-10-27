package knightminer.ceramics.items;

import knightminer.ceramics.Ceramics;
import knightminer.ceramics.recipe.CeramicsTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/** Armor materials, since 1.21.2 a plain record again with the textures in an equipment asset */
public final class ArmorMaterials {
  private ArmorMaterials() {}

  /** Equipment asset of clay armor, assets/ceramics/equipment/clay.json */
  public static final ResourceKey<EquipmentAsset> CLAY_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Ceramics.getResource("clay"));

  /** Clay material: durability factor 4, protection 1/3/2/1, enchantability 7, no toughness, same as the 1.20 enum */
  public static final ArmorMaterial CLAY = new ArmorMaterial(4, defense(), 7, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0f, 0.0f, CeramicsTags.Items.REPAIRS_CLAY_ARMOR, CLAY_ASSET);

  private static Map<ArmorType, Integer> defense() {
    Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
    map.put(ArmorType.HELMET, 1);
    map.put(ArmorType.CHESTPLATE, 3);
    map.put(ArmorType.LEGGINGS, 2);
    map.put(ArmorType.BOOTS, 1);
    map.put(ArmorType.BODY, 3);
    return map;
  }
}
