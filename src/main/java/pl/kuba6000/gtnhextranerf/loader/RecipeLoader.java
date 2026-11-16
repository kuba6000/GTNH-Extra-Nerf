package pl.kuba6000.gtnhextranerf.loader;

import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.loader.recipes.Ae2AssemblyLineRecipes;
import pl.kuba6000.gtnhextranerf.loader.recipes.Ae2RecipeNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.AluminiumNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.BlastFurnaceNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.ChemicalAndMiscNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.CoreModRecipeNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.CraftingNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.IntergalacticRecipeNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.IronBathAlloyRecipes;
import pl.kuba6000.gtnhextranerf.loader.recipes.IronBathMetalProcessingRecipes;
import pl.kuba6000.gtnhextranerf.loader.recipes.ProgressionRecipeNerf;
import pl.kuba6000.gtnhextranerf.loader.recipes.SlimeRecipeNerf;
import pl.kuba6000.gtnhextranerf.machines.Brainrotter;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.materials.TungstenSteelProduction;
import pl.kuba6000.gtnhextranerf.recipes.RecipeFailurePolicy;
import pl.kuba6000.gtnhextranerf.recipes.RecipeRegistry;

public final class RecipeLoader {

    private RecipeLoader() {}

    public static void run() {
        new RecipeRegistry(
            AuditManager.session(),
            GTNHExtraNerf.isDevelopmentEnvironment() ? RecipeFailurePolicy.CONTINUE_ON_VALIDATION_FAILURE
                : RecipeFailurePolicy.FAIL_FAST).register(new CraftingNerf())
                    .register(new AluminiumNerf())
                    .register(new BlastFurnaceNerf())
                    .register(new ChemicalAndMiscNerf())
                    .register(new CoreModRecipeNerf())
                    .register(new SlimeRecipeNerf())
                    .register(new Ae2RecipeNerf())
                    .register(new IntergalacticRecipeNerf())
                    .register(new ProgressionRecipeNerf())
                    .register(new TungstenSteelProduction())
                    .register(new Brainrotter())
                    .register(new IronBathFurnace())
                    .register(new IronBathAlloyRecipes())
                    .register(new IronBathMetalProcessingRecipes())
                    .run();
        Ae2AssemblyLineRecipes.register();
    }

}
