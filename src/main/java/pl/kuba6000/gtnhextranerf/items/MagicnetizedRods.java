package pl.kuba6000.gtnhextranerf.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ShapedArcaneRecipe;

public final class MagicnetizedRods {

    private static final Item MANASTEEL = rod("manasteel");
    private static final Item VOID_METAL = rod("void_metal");
    private static ShapedArcaneRecipe recipe;

    private MagicnetizedRods() {}

    public static void register() {
        GameRegistry.registerItem(MANASTEEL, "magicnetized_manasteel_rod");
        GameRegistry.registerItem(VOID_METAL, "magicnetized_void_metal_rod");
    }

    public static ItemStack manasteel() {
        return new ItemStack(MANASTEEL);
    }

    public static ItemStack voidMetal() {
        return new ItemStack(VOID_METAL);
    }

    private static Item rod(String material) {
        String name = "magicnetized_" + material + "_rod";
        return new Item().setUnlocalizedName("gtnhextranerf." + name)
            .setTextureName(GTNHExtraNerf.MODID + ":" + name)
            .setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
    }

    public static void registerArcaneRecipe() {
        AuditManager.run(AuditScope.ADD_MAGICNETIZED_MANASTEEL_ROD, audit -> {
            recipe = ThaumcraftApi.addArcaneCraftingRecipe(
                "",
                manasteel(),
                new AspectList().add(Aspect.AIR, 10)
                    .add(Aspect.ORDER, 10),
                "   ",
                "MRM",
                " F ",
                'R',
                "stickManasteel",
                'M',
                "stickSteelMagnetic",
                'F',
                solegnolia());
            audit.recipeAdded();
        });
    }

    public static boolean isCrafting(IInventory inventory, EntityPlayer player, ItemStack result) {
        return recipe != null && result != null
            && result.getItem() == MANASTEEL
            && recipe.matches(inventory, player.worldObj, player);
    }

    public static ItemStack craftingRemainder(ItemStack input) {
        ItemStack magnetic = GTOreDictUnificator.get(OrePrefixes.stick, Materials.SteelMagnetic, 1);
        if (input.isItemEqual(magnetic)) {
            return GTOreDictUnificator.get(OrePrefixes.stick, Materials.Steel, 1);
        }
        ItemStack flower = solegnolia();
        if (input.isItemEqual(flower) && input.hasTagCompound()
            && "solegnolia".equals(
                input.getTagCompound()
                    .getString("type"))) {
            ItemStack remainder = input.copy();
            remainder.stackSize = 1;
            return remainder;
        }
        return null;
    }

    public static ItemStack solegnolia() {
        ItemStack flower = new ItemStack(GameRegistry.findItem("Botania", "specialFlower"));
        NBTTagCompound tags = new NBTTagCompound();
        tags.setString("type", "solegnolia");
        flower.setTagCompound(tags);
        return flower;
    }
}
