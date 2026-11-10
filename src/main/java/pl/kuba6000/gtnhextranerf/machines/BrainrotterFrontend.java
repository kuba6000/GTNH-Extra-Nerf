package pl.kuba6000.gtnhextranerf.machines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.modularui.api.drawable.IDrawable;
import com.gtnewhorizons.modularui.api.drawable.UITexture;
import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;

import codechicken.nei.PositionedStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.nei.GTNEIDefaultHandler;
import gregtech.nei.GTNEIDefaultHandler.CachedDefaultRecipe;
import gregtech.nei.GTNEIDefaultHandler.FixedPositionedStack;
import gregtech.nei.GTNEIDefaultHandler.NEITemplateContext;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/** Presentation of the native GT table; the record is reusable and the direction ingredient is consumed. */
public final class BrainrotterFrontend extends RecipeMapFrontend {

    private static final Pos2d SOURCE = new Pos2d(20, 48);
    private static final Pos2d RECORD = new Pos2d(79, 12);
    private static final Pos2d DIRECTION = new Pos2d(79, 76);
    private static final Pos2d OUTPUT = new Pos2d(138, 48);
    private static final UITexture BACKGROUND = UITexture.fullImage(GTNHExtraNerf.MODID, "gui/brainrotter_nei");
    private static final UITexture PLAYER = UITexture.fullImage(GTNHExtraNerf.MODID, "gui/brainrotter_player");

    public BrainrotterFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui, nei);
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        return Arrays.asList(SOURCE, RECORD, DIRECTION);
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        return Collections.singletonList(OUTPUT);
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return Collections.singletonList(SOURCE);
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return Collections.singletonList(OUTPUT);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularWindow.Builder createNEITemplate(NEITemplateContext ctx) {
        ModularWindow.Builder window = ModularWindow.builder(neiProperties.recipeBackgroundSize)
            .setBackground(BACKGROUND);
        // Phantom bindings let GT build its normal PositionedStacks, including fluid alternatives and badges.
        for (int i = 0; i < uiProperties.maxItemInputs; i++) window.widget(
            SlotWidget.phantom(ctx.itemInputsInventory, i)
                .setBackground(IDrawable.EMPTY)
                .setPos(
                    getItemInputPositions(uiProperties.maxItemInputs).get(i)
                        .add(ctx.windowOffset))
                .setSize(18, 18));
        window.widget(
            SlotWidget.phantom(ctx.fluidInputsInventory, 0)
                .setBackground(IDrawable.EMPTY)
                .setPos(SOURCE.add(ctx.windowOffset))
                .setSize(18, 18));
        window.widget(
            SlotWidget.phantom(ctx.itemOutputsInventory, 0)
                .setBackground(IDrawable.EMPTY)
                .setPos(OUTPUT.add(ctx.windowOffset))
                .setSize(18, 18));
        window.widget(
            SlotWidget.phantom(ctx.fluidOutputsInventory, 0)
                .setBackground(IDrawable.EMPTY)
                .setPos(OUTPUT.add(ctx.windowOffset))
                .setSize(18, 18));
        window.widget(new DrawableWidget().setDrawable(() -> {
            int frame = Math.floorMod(GTNEIDefaultHandler.getDrawTicks() / 5, 8);
            int x = frame % 4, y = frame / 4;
            return PLAYER.getSubArea(x / 4f, y / 2f, (x + 1) / 4f, (y + 1) / 2f);
        })
            .setPos(new Pos2d(60, 31).add(ctx.windowOffset))
            .setSize(56, 40));
        return window;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void prepareRecipe(CachedDefaultRecipe recipe) {
        // The output anchor carries GT's window offset; never assume screen or GUI-scale coordinates.
        PositionedStack output = recipe.mOutputs.get(0);
        Pos2d offset = new Pos2d(output.relx - OUTPUT.x, output.rely - OUTPUT.y);
        boolean solidInput = recipe.mRecipe.mFluidInputs.length == 0;
        for (PositionedStack input : recipe.mInputs) {
            FixedPositionedStack stack = (FixedPositionedStack) input;
            Pos2d position;
            if (stack.isNotConsumed()) {
                position = RECORD;
                stack.customBadge = Collections.emptyList();
            } else if (stack.isFluid() || solidInput) {
                position = SOURCE;
                solidInput = false;
            } else position = DIRECTION;
            input.relx = position.x + offset.x;
            input.rely = position.y + offset.y;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void drawNEIOverlays(CachedDefaultRecipe recipe) {
        super.drawNEIOverlays(recipe);
        PositionedStack output = recipe.mOutputs.get(0);
        int x = output.relx - OUTPUT.x, y = output.rely - OUTPUT.y;
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        font.drawString(
            StatCollector.translateToLocal("gtnhextranerf.brainrotter.nei.reusable"),
            104 + x,
            16 + y,
            0x57432B);
        font.drawString(
            StatCollector.translateToLocal("gtnhextranerf.brainrotter.nei.consumed"),
            104 + x,
            80 + y,
            0x49443E);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected List<String> handleNEIItemInputTooltip(List<String> tooltip, FixedPositionedStack stack) {
        super.handleNEIItemInputTooltip(tooltip, stack);
        if (stack.isNotConsumed()) return tooltip;
        String key;
        if (stack.relx - stack.recipe.mOutputs.get(0).relx == DIRECTION.x - OUTPUT.x) key = "direction_hint";
        else key = "source_hint";
        Minecraft client = Minecraft.getMinecraft();
        int width = Math.max(
            16,
            Math.min(
                240,
                new ScaledResolution(client, client.displayWidth, client.displayHeight).getScaledWidth() - 32));
        tooltip.addAll(
            client.fontRenderer.listFormattedStringToWidth(
                EnumChatFormatting.GRAY + StatCollector.translateToLocal("gtnhextranerf.brainrotter.nei." + key),
                width));
        return tooltip;
    }
}
