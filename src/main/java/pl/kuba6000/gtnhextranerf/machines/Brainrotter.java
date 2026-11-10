package pl.kuba6000.gtnhextranerf.machines;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TierEU;
import gregtech.api.interfaces.ITexture;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.materials.NerfMaterials;
import pl.kuba6000.gtnhextranerf.materials.TungstenSteelProduction;
import pl.kuba6000.gtnhextranerf.recipes.RecipeContext;
import pl.kuba6000.gtnhextranerf.recipes.RecipeModule;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;
import pl.kuba6000.gtnhextranerf.recipes.machine.MachineRecipeStore;
import pl.kuba6000.gtnhextranerf.recipes.machine.RecipeMatch;
import thaumcraft.common.config.ConfigItems;

/** One machine's item catalog, recipe map and audited registration. */
public final class Brainrotter implements RecipeModule {

    private static final int MACHINE_ID = 32400;
    public static final int CASING_TEXTURE_INDEX = (127 << 7) | 124;
    public static final ITexture CASING_TEXTURE = TextureFactory
        .of(Textures.BlockIcons.custom(GTNHExtraNerf.MODID, "machines/brainrotter/casing"));
    public static final RecipeMap<RecipeMapBackend> RECIPES = RecipeMapBuilder.of("gtnhextranerf.recipe.brainrotter")
        .maxIO(3, 1, 1, 1)
        .minInputs(2, 0)
        .dontUseProgressBar()
        .neiRecipeBackgroundSize(176, 106)
        .neiRecipeBackgroundOffset(0, 0)
        .neiTransferRect(60, 31, 56, 40)
        .neiHandlerInfo(
            info -> info.setDisplayStack(controller())
                .setHeight(166))
        .frontend(BrainrotterFrontend::new)
        .build();
    private static final Item COMPILATION = new Item() {

        @Override
        @SideOnly(Side.CLIENT)
        public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("item.gtnhextranerf.brainrot_compilation.tooltip.0"));
            tooltip.add(
                EnumChatFormatting.GRAY
                    + StatCollector.translateToLocal("item.gtnhextranerf.brainrot_compilation.tooltip.1"));
        }
    }.setUnlocalizedName("gtnhextranerf.brainrot_compilation")
        .setTextureName(GTNHExtraNerf.MODID + ":brainrot_compilation")
        .setCreativeTab(GregTechAPI.TAB_GREGTECH_MATERIALS);
    private static ItemStack controller;

    public static void registerItem() {
        GameRegistry.registerItem(COMPILATION, "brainrot_compilation");
    }

    public static void registerMachine() {
        GTUtility.addTexturePage((byte) 127);
        if (Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE_INDEX) != null)
            throw new IllegalStateException("Brainrotter casing texture ID is occupied");
        Textures.BlockIcons.setCasingTextureForId(CASING_TEXTURE_INDEX, CASING_TEXTURE);
        if (GregTechAPI.METATILEENTITIES[MACHINE_ID] != null)
            throw new IllegalStateException("Brainrotter machine ID is already occupied: " + MACHINE_ID);
        controller = new MTEIndustrialBrainrotter(
            MACHINE_ID,
            "multimachine.gtnhextranerf.brainrotter",
            "Industrial Brainrotter").getStackForm(1);
    }

    public static ItemStack controller() {
        if (controller == null) throw new IllegalStateException("Brainrotter has not been registered");
        return controller.copy();
    }

    public static ItemStack compilation(int amount) {
        return new ItemStack(COMPILATION, amount);
    }

    @Override
    public void register(RecipeRegistry registry) {
        registry.register(AuditScope.ADD_BRAINROTTER_RECIPES, Brainrotter::recipes);
    }

    private static void recipes(RecipeContext recipes) {
        ItemStack finished = GTOreDictUnificator.get(OrePrefixes.ingot, Materials.TungstenSteel, 1);
        ItemStack raw = GTOreDictUnificator.get(OrePrefixes.ingot, NerfMaterials.rawTungstenSteel(), 1);
        ItemStack hot = GTOreDictUnificator.get(OrePrefixes.ingotHot, NerfMaterials.rawTungstenSteel(), 1);
        ItemStack pulp = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Wood, 1);
        // Reuse the pack's cooling cost rather than GT's cheaper autogenerated material recipe.
        GTRecipe cooling = recipes.edit(new MachineRecipeStore(RecipeMaps.vacuumFreezerRecipes))
            .requireExactlyOne(
                RecipeMatch.builder()
                    .itemInputs(GTOreDictUnificator.get(OrePrefixes.ingotHot, Materials.TungstenSteel, 1))
                    .itemOutputs(finished)
                    .fluidInputs()
                    .fluidOutputs()
                    .build());
        recipes.machine(RecipeMaps.vacuumFreezerRecipes)
            .addOne(
                GTRecipeBuilder.builder()
                    .itemInputs(hot)
                    .itemOutputs(raw)
                    .duration(cooling.mDuration)
                    .eut(cooling.mEUt)
                    .specialValue(cooling.mSpecialValue),
                RecipeMatch.builder()
                    .itemInputs(hot)
                    .itemOutputs(raw)
                    .fluidInputs()
                    .fluidOutputs()
                    .build());
        recipes.machine(RECIPES)
            .addOne(
                GTRecipeBuilder.builder()
                    .itemInputs(raw, compilation(0), pulp)
                    .itemOutputs(finished)
                    .duration(200)
                    .eut(TierEU.RECIPE_EV),
                RecipeMatch.builder()
                    .itemInputs(raw, compilation(0), pulp)
                    .itemOutputs(finished)
                    .fluidInputs()
                    .fluidOutputs()
                    .build());
        recipes.machine(RECIPES)
            .addOne(
                GTRecipeBuilder.builder()
                    .itemInputs(compilation(0), pulp)
                    .fluidInputs(
                        NerfMaterials.rawTungstenSteel()
                            .getMolten(144))
                    .fluidOutputs(Materials.TungstenSteel.getMolten(144))
                    .duration(200)
                    .eut(TierEU.RECIPE_EV),
                RecipeMatch.builder()
                    .itemInputs(compilation(0), pulp)
                    .fluidInputs(
                        NerfMaterials.rawTungstenSteel()
                            .getMolten(144))
                    .itemOutputs()
                    .fluidOutputs(Materials.TungstenSteel.getMolten(144))
                    .build());
        recipes.machine(RecipeMaps.assemblerRecipes)
            .addOne(
                GTRecipeBuilder.builder()
                    .itemInputs(
                        ItemList.Casing_EV.get(1),
                        ItemList.Sensor_EV.get(1),
                        ItemList.Electric_Piston_EV.get(2),
                        new Object[] { OrePrefixes.circuit.get(Materials.EV), 4 })
                    .itemOutputs(controller())
                    .duration(200)
                    .eut(TierEU.RECIPE_EV),
                RecipeMatch.builder()
                    .itemOutputs(controller())
                    .build());
        recipes.machine(RecipeMaps.assemblerRecipes)
            .addOne(
                GTRecipeBuilder.builder()
                    .itemInputs(
                        ItemList.Tool_DataStick.get(1),
                        new ItemStack(ConfigItems.itemZombieBrain, 64),
                        new ItemStack(Blocks.noteblock),
                        new ItemStack(Items.clock),
                        new ItemStack(Items.sugar, 16),
                        new ItemStack(Items.redstone, 16))
                    .fluidInputs(Materials.Glue.getFluid(144))
                    .itemOutputs(compilation(1))
                    .duration(400)
                    .eut(TierEU.RECIPE_EV),
                RecipeMatch.builder()
                    .itemOutputs(compilation(1))
                    .build());
    }

    public static void verify() {
        AuditManager.run(AuditScope.VERIFY_BRAINROTTER, audit -> {
            if (!(GregTechAPI.METATILEENTITIES[MACHINE_ID] instanceof MTEIndustrialBrainrotter)
                || Textures.BlockIcons.getCasingTextureForId(CASING_TEXTURE_INDEX) != CASING_TEXTURE
                || GameRegistry.findItem(GTNHExtraNerf.MODID, "brainrot_compilation") != COMPILATION
                || RECIPES.getAllRecipes()
                    .size() != 2)
                throw new IllegalStateException("Brainrotter registry or processing recipes are missing");
            ((MTEIndustrialBrainrotter) GregTechAPI.METATILEENTITIES[MACHINE_ID]).getStructureDefinition();
            TungstenSteelProduction.verifyPublished();
            audit.success();
        });
    }
}
