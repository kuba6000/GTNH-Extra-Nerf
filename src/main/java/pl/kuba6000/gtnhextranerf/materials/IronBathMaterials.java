package pl.kuba6000.gtnhextranerf.materials;

import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.enums.MaterialBuilder;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.SubTag;
import gregtech.api.enums.TextureSet;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GTOreDictUnificator;

/** Registers the furnace's independent materials before GT generates their forms. */
public final class IronBathMaterials {

    // These IDs become persisted item metadata when worlds first use the materials.
    private static final int WROUGHT_IRON_ID = 860;
    private static final int OVERCARBURIZED_IRON_ID = 863;
    private static final int SLAG_ID = 864;

    private static Materials wroughtIron;
    private static Materials overcarburizedIron;
    private static Materials slag;

    private IronBathMaterials() {}

    public static void register() {
        requireAvailable(WROUGHT_IRON_ID, "WroughtIron");
        requireAvailable(OVERCARBURIZED_IRON_ID, "OvercarburizedIron");
        requireAvailable(SLAG_ID, "Slag");

        wroughtIron = metal("WroughtIron", "Wrought Iron", 0x00a4afb4).addOrePrefix(OrePrefixes.plate)
            .setChemicalFormula("Fe100C0.2")
            .addMaterial(Materials.Iron, 20000)
            .addMaterial(Materials.Carbon, 39)
            .constructMaterial();
        wroughtIron.mMetaItemSubID = WROUGHT_IRON_ID;
        Materials.WroughtIron = wroughtIron;
        preserveIdentity(Materials.CastIron);
        preserveIdentity(Materials.PigIron);

        overcarburizedIron = metal("OvercarburizedIron", "Overcarburized Iron", 0x00434343)
            .setChemicalFormula("Fe100C4.5")
            .addMaterial(Materials.Iron, 2000)
            .addMaterial(Materials.Carbon, 91)
            .constructMaterial();
        overcarburizedIron.mMetaItemSubID = OVERCARBURIZED_IRON_ID;

        MaterialBuilder slagBuilder = base("Slag", "Slag", 0x0068735b).setIconSet(TextureSet.SET_DULL)
            .setMoltenARGB(0x00ffd58a)
            .setChemicalFormula("");
        slagBuilder.removeOrePrefix(OrePrefixes.ingot);
        slag = slagBuilder.constructMaterial();
        slag.mMetaItemSubID = SLAG_ID;
    }

    public static Materials wroughtIron() {
        return Objects.requireNonNull(wroughtIron, "Wrought Iron has not been registered");
    }

    public static Materials overcarburizedIron() {
        return Objects.requireNonNull(overcarburizedIron, "Overcarburized Iron has not been registered");
    }

    public static Materials slag() {
        return Objects.requireNonNull(slag, "Slag has not been registered");
    }

    public static void verify() {
        if (Materials.WroughtIron != wroughtIron() || Materials.CastIron == Materials.PigIron
            || wroughtIron() == Materials.CastIron
            || wroughtIron() == Materials.PigIron)
            throw new IllegalStateException("Iron bath metals do not have distinct native material identities");
        for (Materials material : new Materials[] { wroughtIron(), Materials.CastIron, Materials.PigIron }) {
            if (material.mMaterialInto != material || material.mSmeltInto != material
                || material.mMacerateInto != material
                || material.mArcSmeltInto != material
                || material.mDirectSmelting != material
                || material.mOreReRegistrations.contains(Materials.AnyIron)
                || (material.mExtraData & 3) != 0)
                throw new IllegalStateException("Iron bath processing changes material identity: " + material);
            for (OrePrefixes prefix : new OrePrefixes[] { OrePrefixes.ingot, OrePrefixes.dust }) {
                ItemStack stack = GTOreDictUnificator.get(prefix, material, 1);
                ItemData data = GTOreDictUnificator.getItemData(stack);
                if (stack == null || data == null
                    || data.mMaterial == null
                    || data.mMaterial.mMaterial != material
                    || OreDictionary.getOres(
                        prefix.get(Materials.AnyIron)
                            .toString())
                        .stream()
                        .anyMatch(stack::isItemEqual))
                    throw new IllegalStateException(
                        "Iron bath form is missing or registered as ordinary Iron: " + prefix + material);
            }
            if (material.getMolten(1) == null || material.getMolten(1)
                .isFluidEqual(Materials.Iron.getMolten(1)))
                throw new IllegalStateException("Iron bath molten identity is invalid: " + material);
        }
    }

    private static void preserveIdentity(Materials material) {
        material.mMaterialInto = material;
        material.mSmeltInto = material;
        material.mMacerateInto = material;
        material.mArcSmeltInto = material;
        material.mDirectSmelting = material;
        material.mArcSmeltIntoWithGas.clear();
        material.mOreReRegistrations.remove(Materials.AnyIron);
        // GT's decomposition bits generate electrolyzer and centrifuge recipes from the nominal material list
        material.mExtraData &= ~3;
    }

    private static MaterialBuilder metal(String name, String localName, int color) {
        return base(name, localName, color).addOrePrefix(OrePrefixes.ingot)
            .addSubTag(SubTag.METAL);
    }

    private static MaterialBuilder base(String name, String localName, int color) {
        return new MaterialBuilder().setName(name)
            .setDefaultLocalName(localName)
            .setIconSet(TextureSet.SET_METALLIC)
            .setARGB(color)
            .addDustItems()
            .removeOrePrefix(OrePrefixes.dustSmall)
            .removeOrePrefix(OrePrefixes.dustTiny)
            .addSubTag(SubTag.SMELTING_TO_FLUID)
            .setMeltingPoint(1700)
            .setBlastFurnaceTemp(1700)
            .setBlastFurnaceRequired(true)
            .setAutoGenerateBlastFurnaceRecipes(false)
            .setAutoGeneratedVacuumFreezerRecipe(false)
            .setAutoGeneratedRecycleRecipes(false);
    }

    private static void requireAvailable(int id, String name) {
        if (Materials.get(name) != Materials._NULL) {
            throw new IllegalStateException("Iron bath material is already registered: " + name);
        }
        for (Materials existing : Materials.getAll()) {
            if (existing.mMetaItemSubID == id) {
                throw new IllegalStateException("Iron bath material ID " + id + " is occupied by " + existing.mName);
            }
        }
    }
}
