package pl.kuba6000.gtnhextranerf.items;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;

public final class BlightedSoybean {

    private static final Item ITEM = new Item().setUnlocalizedName("gtnhextranerf.blighted_soybean")
        .setTextureName("gtnhextranerf:blighted_soybean")
        .setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);

    private BlightedSoybean() {}

    public static void register() {
        GameRegistry.registerItem(ITEM, "blighted_soybean");
    }

    public static Item item() {
        return ITEM;
    }

    public static Item replaceCropDrop(Item original) {
        return original == GameRegistry.findItem("harvestcraft", "soybeanItem") ? ITEM : original;
    }
}
