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

public final class MagicnetizedManasteelRod {

    private static final Item ITEM = new Item().setUnlocalizedName("gtnhextranerf.magicnetized_manasteel_rod")
        .setTextureName(GTNHExtraNerf.MODID + ":magicnetized_manasteel_rod")
        .setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
    private static ShapedArcaneRecipe recipe;

    private MagicnetizedManasteelRod() {}

    public static void register() {
        GameRegistry.registerItem(ITEM, "magicnetized_manasteel_rod");
    }

    public static ItemStack stack() {
        return new ItemStack(ITEM);
    }

    public static void registerArcaneRecipe() {
        AuditManager.run(AuditScope.ADD_MAGICNETIZED_MANASTEEL_ROD, audit -> {
            recipe = ThaumcraftApi.addArcaneCraftingRecipe(
                "",
                stack(),
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
            && result.getItem() == ITEM
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

    private static ItemStack solegnolia() {
        ItemStack flower = new ItemStack(GameRegistry.findItem("Botania", "specialFlower"));
        NBTTagCompound tags = new NBTTagCompound();
        tags.setString("type", "solegnolia");
        flower.setTagCompound(tags);
        return flower;
    }
}
