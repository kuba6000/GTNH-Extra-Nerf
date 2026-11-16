package pl.kuba6000.gtnhextranerf.client.nei;

import java.awt.Point;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.core.mixins.early.minecraft.GuiContainerAccessor;
import com.gtnewhorizons.modularui.api.drawable.UITexture;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;

/** Diagrams for the iron bath's material targets and carbon additives. */
@SideOnly(Side.CLIENT)
public final class IronBathRecipeHandler extends TemplateRecipeHandler {

    public static final String ID = "gtnhextranerf.iron_bath.diagram";
    public static final int WIDTH = 166;
    public static final int HEIGHT = 252;
    private static final UITexture VESSEL = UITexture.fullImage("gtnhextranerf", "gui/iron_bath_vessel");
    private static final UITexture ITEM_SLOT = UITexture.fullImage("gregtech", "gui/slot/item_standard");
    private static final UITexture FLUID_SLOT = UITexture.fullImage("gregtech", "gui/slot/fluid_standard");
    private static final UITexture ARROW = GTUITextures.PROGRESSBAR_ARROW.getSubArea(0, 0, 1, 0.5f);
    private static final UITexture OXYGEN_ARROW = UITexture.fullImage("gtnhextranerf", "gui/iron_bath_oxygen_arrow");
    private static final IronProduct[] METALS = { IronProduct.IRON, IronProduct.WROUGHT_IRON, IronProduct.STEEL,
        IronProduct.CAST_IRON, IronProduct.PIG_IRON, IronProduct.OVERCARBURIZED_IRON };

    @Override
    public String getRecipeName() {
        return text("title");
    }

    @Override
    public String getGuiTexture() {
        return "gregtech:textures/gui/background/nei_single_recipe.png";
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results) {
        if (ID.equals(outputId) || "all".equals(outputId)) {
            if (results.length > 0 && results[0] instanceof IronProduct product) addProduct(product);
            else for (IronProduct product : IronProduct.values()) addProduct(product);
        } else super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadCraftingRecipes(ItemStack result) {
        for (IronProduct product : IronProduct.values()) {
            if (matchesProduct(result, product)) addProduct(product);
        }
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        boolean common = GTUtility.areStacksEqual(ingredient, IronBathFurnace.controller())
            || matchesPart(ingredient, OrePrefixes.ingot, Materials.Iron)
            || matchesPart(ingredient, OrePrefixes.dust, Materials.Iron);
        boolean flux = matchesPart(ingredient, OrePrefixes.dust, Materials.Calcite);
        boolean oxygen = matchesFluid(ingredient, Materials.Oxygen.getGas(1));
        for (IronProduct product : IronProduct.values()) {
            if (product == IronProduct.IRON) {
                if (common || oxygen) arecipes.add(new BathPage(product, null));
                continue;
            }
            for (GTRecipe balance : IronBathFurnace.RECIPES.getAllRecipes()) {
                if (common || flux
                    || oxygen && product != IronProduct.SLAG
                    || GTUtility.areStacksEqual(ingredient, balance.mInputs[1]))
                    arecipes.add(new BathPage(product, balance));
            }
        }
    }

    @Override
    public IUsageHandler getUsageAndCatalystHandler(String inputId, Object... ingredients) {
        return getUsageHandler(inputId, ingredients);
    }

    private void addProduct(IronProduct product) {
        if (product == IronProduct.IRON) arecipes.add(new BathPage(product, null));
        else for (GTRecipe balance : IronBathFurnace.RECIPES.getAllRecipes())
            arecipes.add(new BathPage(product, balance));
    }

    private static boolean matchesProduct(ItemStack stack, IronProduct product) {
        Materials material = IronBathFurnace.material(product);
        return matchesFluid(stack, material.getMolten(1)) || matchesPart(stack, OrePrefixes.dust, material)
            || product != IronProduct.SLAG && matchesPart(stack, OrePrefixes.ingot, material);
    }

    private static boolean matchesPart(ItemStack stack, OrePrefixes prefix, Materials material) {
        return GTUtility
            .areStacksEqual(GTOreDictUnificator.get(true, stack), GTOreDictUnificator.get(prefix, material, 1));
    }

    private static boolean matchesFluid(ItemStack stack, FluidStack target) {
        FluidStack fluid = GTUtility.getFluidFromDisplayStack(stack);
        if (fluid == null) fluid = GTUtility.getFluidForFilledItem(stack, true);
        return fluid != null && fluid.isFluidEqual(target);
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1, 1, 1, 1);
        GTUITextures.BACKGROUND_NEI_SINGLE_RECIPE.draw(0, 0, WIDTH, HEIGHT);
        BathPage page = (BathPage) arecipes.get(recipe);
        int metalTop = page.balance == null ? 34 : 46;
        GuiDraw.drawRect(61, metalTop, 44, 86 - metalTop, 0xFFA4A4A4);
        if (page.balance != null) {
            GuiDraw.drawRect(61, 34, 44, 12, 0xFF707070);
            GuiDraw.drawRect(61, 34, 44, 1, 0xFFBFBFBF);
        }
        GuiDraw.drawRect(61, 34, 2, 52, 0x45000000);
        GuiDraw.drawRect(103, 34, 2, 52, 0x45000000);
        GuiDraw.drawRect(63, metalTop, 40, 1, 0xFFE6E6E6);
        GL11.glColor4f(1, 1, 1, 1);
        VESSEL.draw(53, 14, 60, 90);
        ARROW.draw(28, 20, 20, 18);
        if (page.balance != null) {
            ARROW.draw(28, 52, 20, 18);
            ARROW.draw(28, 84, 20, 18);
            ARROW.draw(118, 34, 20, 18);
        }
        ARROW.draw(118, 66, 20, 18);
        if (page.product != IronProduct.SLAG) {
            OXYGEN_ARROW.draw(74, 107, 18, 20);
        }
        GL11.glColor4f(1, 1, 1, 1);
        for (PositionedStack stack : page.inputs) slot(stack);
        slot(page.result);
        for (PositionedStack stack : page.other) slot(stack);
        GuiDraw.drawRect(4, 215, 158, 13, 0xFF404040);
        for (int i = 0; i < METALS.length; i++) {
            int x = 6 + i * 26;
            GuiDraw.drawRect(x, 217, 24, 9, METALS[i].displayColor());
            GuiDraw.drawRect(x, 217, 24, 1, 0x40FFFFFF);
            GuiDraw.drawRect(x, 225, 24, 1, 0x40000000);
            if (page.product == METALS[i]) {
                GuiDraw.drawRect(x - 2, 214, 28, 15, 0xFF404040);
                GuiDraw.drawRect(x - 1, 215, 26, 13, METALS[i].displayColor());
                GuiDraw.drawRect(x - 1, 215, 26, 1, 0x40FFFFFF);
                GuiDraw.drawRect(x - 1, 227, 26, 1, 0x40000000);
            }
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void slot(PositionedStack stack) {
        (stack.relx == 75 || stack.relx == 144 ? FLUID_SLOT : ITEM_SLOT).draw(stack.relx - 1, stack.rely - 1, 18, 18);
    }

    @Override
    public void drawForeground(int recipe) {
        GL11.glDisable(GL11.GL_LIGHTING);
        BathPage page = (BathPage) arecipes.get(recipe);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawString(text("feed"), 5, 8, 0x404040);
        font.drawString(text("tap"), 140, 22, 0x404040);
        String name = IronBathFurnace.material(page.product)
            .getLocalizedName();
        int y = 154;
        for (String line : font.listFormattedStringToWidth(name, WIDTH - 8)) {
            centered(font, line, y, 0x303030);
            y += font.FONT_HEIGHT;
        }
        centered(font, text("continuous"), 177, 0x606060);
        centered(
            font,
            page.product == IronProduct.SLAG ? text("separation")
                : text("reference", decimal(page.product.carbonPerIngot())),
            190,
            0x404040);
        centered(font, text("range"), 203, 0x606060);
        centered(font, text("heat", (int) IronBath.MELTING_TEMPERATURE), 238, 0x505050);
    }

    private static void centered(FontRenderer font, String text, int y, int color) {
        font.drawString(text, (WIDTH - font.getStringWidth(text)) / 2, y, color);
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> tooltip, int recipe) {
        if (!tooltip.isEmpty()) return tooltip;
        Point mouse = GuiDraw.getMousePosition();
        Point origin = gui.getRecipePosition(recipe);
        GuiContainerAccessor container = (GuiContainerAccessor) gui;
        int x = mouse.x - container.getGuiLeft() - origin.x;
        int y = mouse.y - container.getGuiTop() - origin.y;
        if (y >= 215 && y < 228 && x >= 6 && x < 162) {
            IronProduct product = METALS[(x - 6) / 26];
            tooltip.add(
                IronBathFurnace.material(product)
                    .getLocalizedName());
            tooltip.add(
                product == IronProduct.IRON ? text("zero")
                    : product == IronProduct.WROUGHT_IRON ? text("wrought_range")
                        : product == IronProduct.STEEL ? text("steel_range")
                            : product == IronProduct.OVERCARBURIZED_IRON
                                ? text("above", ratio(product.lowerCarbonRatio()))
                                : text(
                                    "between",
                                    ratio(product.lowerCarbonRatio()),
                                    ratio(product.upperCarbonRatio())));
            tooltip.add(text("ratio"));
        } else if (x >= 53 && x < 113 && y >= 14 && y < 104) {
            tooltip.add(text("electric"));
            tooltip.add(text("oxygen"));
            tooltip.add(text("flux"));
        }
        return tooltip;
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> tooltip, int recipe) {
        BathPage page = (BathPage) arecipes.get(recipe);
        if (matchesPart(stack, OrePrefixes.dust, Materials.Calcite)) {
            tooltip.add(text("flux"));
            tooltip.add(text("calcite_consumption", decimal(page.amounts.calciteItems())));
            tooltip.add(text("calcite_reserve", (int) Math.ceil(page.amounts.calciteItems())));
        } else if (matchesFluid(stack, Materials.Oxygen.getGas(1))) tooltip.add(text("oxygen"));
        else if (page.balance != null && GTUtility.areStacksEqual(stack, page.balance.mInputs[1])) {
            IronBath.Fuel fuel = IronBathFurnace.fuel(stack);
            tooltip.add(text("carbon", decimal(fuel.carbon()), fuel.impurities()));
            tooltip.add(text("heated"));
        } else if (matchesPart(stack, OrePrefixes.ingot, Materials.Iron)
            || matchesPart(stack, OrePrefixes.dust, Materials.Iron)) tooltip.add(text("iron"));
        else if (matchesFluid(
            stack,
            IronBathFurnace.material(IronProduct.SLAG)
                .getMolten(1)))
            tooltip.add(text("slag"));
        else if (matchesProduct(stack, page.product)) tooltip.add(text("tap_control"));
        return tooltip;
    }

    private static String ratio(double fraction) {
        return decimal(144 * fraction);
    }

    private static String decimal(double value) {
        return BigDecimal.valueOf(value)
            .stripTrailingZeros()
            .toPlainString();
    }

    private static String text(String key, Object... arguments) {
        return StatCollector.translateToLocalFormatted("gtnhextranerf.nei.iron_bath." + key, arguments);
    }

    public final class BathPage extends CachedRecipe {

        private final IronProduct product;
        private final GTRecipe balance;
        private final IronProduct.BatchBalance amounts;
        private final List<PositionedStack> inputs = new ArrayList<>();
        private final List<PositionedStack> other = new ArrayList<>();
        private final PositionedStack result;

        private BathPage(IronProduct product, GTRecipe balance) {
            this.product = product;
            this.balance = balance;
            IronProduct metal = product == IronProduct.SLAG ? IronProduct.STEEL : product;
            amounts = metal.balance(balance == null ? null : IronBathFurnace.fuel(balance.mInputs[1]));
            inputs.add(
                new FixedPositionedStack(
                    null,
                    new ItemStack[] {
                        GTUtility.copyAmountUnsafe(
                            amounts.ironItems(),
                            GTOreDictUnificator.get(OrePrefixes.ingot, Materials.Iron, 1)),
                        GTUtility.copyAmountUnsafe(
                            amounts.ironItems(),
                            GTOreDictUnificator.get(OrePrefixes.dust, Materials.Iron, 1)) },
                    true,
                    6,
                    20,
                    PositionedStack.CHANCE_FULL,
                    false,
                    true));
            if (balance != null) {
                ItemStack carbon = balance.mInputs[1].copy();
                carbon.stackSize = amounts.fuelItems();
                inputs
                    .add(new FixedPositionedStack(null, carbon, true, 6, 52, PositionedStack.CHANCE_FULL, false, true));
                inputs.add(new CalciteStack(amounts.calciteItems()));
            }
            if (product != IronProduct.SLAG) inputs.add(
                new PositionedStack(GTUtility.getFluidDisplayStack(Materials.Oxygen.getGas(0), false, true), 75, 130));
            result = new FixedPositionedStack(
                null,
                GTUtility.getFluidDisplayStack(
                    IronBathFurnace.material(product)
                        .getMolten(product == IronProduct.SLAG ? amounts.slagMb() : amounts.metalMb()),
                    true),
                true,
                144,
                product == IronProduct.SLAG ? 34 : 66,
                false);
            if (balance != null) other.add(
                new FixedPositionedStack(
                    null,
                    GTUtility.getFluidDisplayStack(
                        IronBathFurnace.material(product == IronProduct.SLAG ? metal : IronProduct.SLAG)
                            .getMolten(product == IronProduct.SLAG ? amounts.metalMb() : amounts.slagMb()),
                        true),
                    true,
                    144,
                    product == IronProduct.SLAG ? 66 : 34,
                    false));
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }

        @Override
        public List<PositionedStack> getIngredients() {
            return Collections.unmodifiableList(inputs);
        }

        @Override
        public List<PositionedStack> getOtherStacks() {
            return Collections.unmodifiableList(other);
        }
    }

    /** Shows fractional flux consumption while retaining the whole-dust load as the native item stack. */
    public static final class CalciteStack extends FixedPositionedStack {

        private final String consumption;

        private CalciteStack(double consumed) {
            super(
                null,
                GTUtility.copyAmountUnsafe(
                    (int) Math.ceil(consumed),
                    GTOreDictUnificator.get(OrePrefixes.dust, Materials.Calcite, 1)),
                true,
                6,
                84,
                PositionedStack.CHANCE_FULL,
                false,
                true);
            consumption = decimal(consumed);
        }

        public String consumptionText() {
            return consumption;
        }

        @Override
        public void draw(int mousex, int mousey) {
            GuiContainerManager
                .drawItem(relx + (width - 16) / 2, rely + (height - 16) / 2, item, true, consumptionText());
        }
    }
}
