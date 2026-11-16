package pl.kuba6000.gtnhextranerf.machines;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.value.ObjectValue;
import com.cleanroommc.modularui.value.sync.GenericListSyncHandler;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.LongSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import gregtech.api.util.GTUtility;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath.ChargeGroup;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;

/** Bath quantities are synchronized only while a player views this panel. */
final class IronBathGui extends MTEMultiBlockBaseGui<MTEIronBathFurnace> {

    private static final int CHARGE_GROUPS_PER_PAGE = 24;
    private static final int SLOTS_PER_ROW = 12;
    private static final IronProduct[] PRODUCTS = IronProduct.values();
    // CIE-derived colors through 2200 K, extended with approximate white and blue incandescence
    private static final int[] INCANDESCENCE_TEMPERATURES = { 800, 1000, 1200, 1400, 1600, 1800, 2000, 2200, 3000, 4000,
        6500, 10000, 14000 };
    private static final int[] INCANDESCENCE_COLORS = { 0xFFFF0000, 0xFFFF1700, 0xFFFF4400, 0xFFFF5C00, 0xFFFF6F00,
        0xFFFF7E00, 0xFFFF8B16, 0xFFFF962F, 0xFFFFD6AE, 0xFFFFF1D9, 0xFFFFFDF9, 0xFFDCE8FF, 0xFFC7DBFF };

    private final IntSyncValue temperature;
    private final IntSyncValue metal;
    private final IntSyncValue liquid;
    private final IntSyncValue capacity;
    private final IntSyncValue metalCapacity;
    private final IntSyncValue slag;
    private final IntSyncValue liquidSlag;
    private final IntSyncValue product;
    private final LongSyncValue carbon;
    private final IntSyncValue impurities;
    private final IntSyncValue calcite;
    private final IntSyncValue calciteMaximum;
    private final LongSyncValue power;
    private final IntSyncValue maximumTemperature;
    private final IntSyncValue meltingItems;
    private final IntSyncValue chargePage;
    private final IntSyncValue chargeGroupCount;
    private final GenericListSyncHandler<ChargeGroup> chargeGroups;
    private int pageIndex;

    IronBathGui(MTEIronBathFurnace machine) {
        super(machine);
        temperature = new IntSyncValue(
            () -> (int) machine.bath()
                .temperature());
        metal = new IntSyncValue(
            () -> machine.bath()
                .metalMb());
        liquid = new IntSyncValue(
            () -> machine.bath()
                .liquidMetalMb());
        capacity = new IntSyncValue(
            () -> machine.bath()
                .capacityMb());
        metalCapacity = new IntSyncValue(
            () -> machine.bath()
                .metalCapacityMb());
        slag = new IntSyncValue(
            () -> machine.bath()
                .slagMb());
        liquidSlag = new IntSyncValue(
            () -> machine.bath()
                .liquidSlagMb());
        product = new IntSyncValue(
            () -> machine.bath()
                .currentProduct()
                .ordinal());
        carbon = new LongSyncValue(
            () -> Math.round(
                machine.bath()
                    .carbon() * 1000));
        impurities = new IntSyncValue(
            () -> machine.bath()
                .impurities());
        calcite = new IntSyncValue(
            () -> machine.bath()
                .calciteCapacity());
        calciteMaximum = new IntSyncValue(
            () -> machine.bath()
                .calciteMaximum());
        power = new LongSyncValue(machine::actualPower);
        maximumTemperature = new IntSyncValue(
            () -> (int) machine.bath()
                .maximumTemperature());
        meltingItems = new IntSyncValue(
            () -> machine.bath()
                .meltingItems());
        chargeGroupCount = new IntSyncValue(
            () -> machine.bath()
                .chargeGroupCount());
        chargePage = new IntSyncValue(
            () -> pageIndex,
            value -> pageIndex = Math.max(0, value),
            () -> Math.min(
                pageIndex,
                (machine.bath()
                    .chargeGroupCount() - 1) / CHARGE_GROUPS_PER_PAGE),
            value -> pageIndex = Math.max(
                0,
                Math.min(
                    value,
                    (machine.bath()
                        .chargeGroupCount() - 1) / CHARGE_GROUPS_PER_PAGE))).allowC2S();
        chargeGroups = new GenericListSyncHandler<>(
            () -> machine.bath()
                .chargeGroups(
                    Math.min(
                        pageIndex,
                        Math.max(
                            0,
                            (machine.bath()
                                .chargeGroupCount() - 1) / CHARGE_GROUPS_PER_PAGE))
                        * CHARGE_GROUPS_PER_PAGE,
                    CHARGE_GROUPS_PER_PAGE),
            null,
            buffer -> new ChargeGroup(buffer.readItemStackFromBuffer(), buffer.readInt(), buffer.readDouble()),
            (buffer, group) -> {
                buffer.writeItemStackToBuffer(group.stack());
                buffer.writeInt(group.count());
                buffer.writeDouble(group.progress());
            },
            (a, b) -> a.count() == b.count() && Math.round(a.progress() * 1000) == Math.round(b.progress() * 1000)
                && ItemStack.areItemStacksEqual(a.stack(), b.stack()),
            null);
    }

    @Override
    protected void registerSyncValues(PanelSyncManager sync) {
        super.registerSyncValues(sync);
        sync.syncValue("bathTemperature", temperature);
        sync.syncValue("bathMetal", metal);
        sync.syncValue("bathLiquid", liquid);
        sync.syncValue("bathCapacity", capacity);
        sync.syncValue("bathMetalCapacity", metalCapacity);
        sync.syncValue("bathSlag", slag);
        sync.syncValue("bathLiquidSlag", liquidSlag);
        sync.syncValue("bathProduct", product);
        sync.syncValue("bathCarbon", carbon);
        sync.syncValue("bathImpurities", impurities);
        sync.syncValue("bathCalcite", calcite);
        sync.syncValue("bathCalciteMaximum", calciteMaximum);
        sync.syncValue("bathPower", power);
        sync.syncValue("bathMaximumTemperature", maximumTemperature);
        sync.syncValue("bathMeltingItems", meltingItems);
        sync.syncValue("bathChargePage", chargePage);
        sync.syncValue("bathChargeGroupCount", chargeGroupCount);
        sync.syncValue("bathChargeGroups", chargeGroups);
    }

    @Override
    protected ListWidget<IWidget, ?> createTerminalTextWidget(PanelSyncManager sync, ModularPanel parent) {
        return new ListWidget<>().fullWidth()
            .child(createShutdownReasonWidget(sync))
            .child(createStructureErrorWidget(sync));
    }

    @Override
    public Flow createMainColumn(ModularPanel panel, PanelSyncManager sync) {
        return Flow.column()
            .paddingLeft(4)
            .paddingRight(4)
            .paddingTop(2)
            .paddingBottom(2)
            .child(createBathDisplay())
            .child(createChargeGroups(panel, sync))
            .child(createPanelGap(panel, sync))
            .child(createInventoryRow(panel, sync));
    }

    private ChargeGroup displayedGroup(int index) {
        List<ChargeGroup> groups = chargeGroups.getValue();
        return index < groups.size() ? groups.get(index) : null;
    }

    private ButtonWidget<?> chargePageButton(int direction) {
        return new ButtonWidget<>().name(direction < 0 ? "bathChargePreviousPage" : "bathChargeNextPage")
            .size(14, 16)
            .overlay(IKey.str(direction < 0 ? "<" : ">"))
            .setEnabledIf(
                widget -> direction < 0 ? chargePage.getValue() > 0
                    : (chargePage.getValue() + 1L) * CHARGE_GROUPS_PER_PAGE < chargeGroupCount.getValue())
            .addTooltipLine(
                IKey.dynamic(
                    () -> text(
                        "charge_page",
                        chargePage.getValue() + 1,
                        Math.max(1, (chargeGroupCount.getValue() - 1) / CHARGE_GROUPS_PER_PAGE + 1))))
            .onMousePressed(button -> {
                if (button != 0) return false;
                chargePage.setValue(chargePage.getValue() + direction);
                return true;
            });
    }

    private ParentWidget<?> createChargeGroups(ModularPanel panel, PanelSyncManager sync) {
        ParentWidget<?> grid = new ParentWidget<>().size(278, 66)
            .child(
                new ParentWidget<>().size(278, 22)
                    .child(
                        label(() -> text("melting_items", meltingItems.getValue()), 4, 4, 136)
                            .addTooltipLine(IKey.lang("gtnhextranerf.iron_bath.gui.charge_help")))
                    .child(chargePageButton(-1).pos(142, 3))
                    .child(chargePageButton(1).pos(158, 3))
                    .child(
                        createTerminalTextWidget(sync, panel).pos(176, 0)
                            .size(78, 22))
                    .child(
                        new ParentWidget<>().pos(256, 0)
                            .size(18, 22)
                            .child(createMaintIssueHoverableTerminal(sync))));
        ParentWidget<?> scroll = new ParentWidget<>().name("bathChargeGroups")
            .pos(4, 22)
            .size(270, 44);
        for (int index = 0; index < CHARGE_GROUPS_PER_PAGE; index++) {
            final int slot = index;
            scroll.child(
                new ParentWidget<>().pos(slot % SLOTS_PER_ROW * 22, slot / SLOTS_PER_ROW * 22)
                    .size(20, 21)
                    .setEnabledIf(widget -> displayedGroup(slot) != null)
                    .child(new ItemDisplayWidget() {

                        @Override
                        public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
                            super.draw(context, theme);
                            ChargeGroup group = displayedGroup(slot);
                            if (group != null && group.count() > 1) {
                                GuiDraw.drawStandardSlotAmountText(group.count(), null, getArea());
                            }
                        }
                    }.name("bathChargeItem" + slot)
                        .item(
                            new ObjectValue.Dynamic<>(
                                ItemStack.class,
                                () -> displayedGroup(slot) == null ? null : displayedGroup(slot).stack(),
                                null))
                        .size(18, 18)
                        .tooltipAutoUpdate(true)
                        .tooltipDynamic(t -> {
                            ChargeGroup group = displayedGroup(slot);
                            ItemStack stack = group == null ? null : group.stack();
                            if (stack == null) {
                                t.add(text("charge_empty"));
                            } else {
                                t.addFromItem(stack)
                                    .newLine()
                                    .add(text("charge_count", group.count()))
                                    .newLine()
                                    .add(
                                        text(
                                            "charge_progress",
                                            String.format(Locale.ROOT, "%.1f", group.progress() * 100)));
                            }
                        }))
                    .child(new IDrawable.DrawableWidget((context, x, y, width, height, theme) -> {
                        GuiDraw.drawRect(x, y, width, height, 0xFF34373B);
                        ChargeGroup group = displayedGroup(slot);
                        GuiDraw
                            .drawRect(x, y, group == null ? 0 : width * (float) group.progress(), height, 0xFFE8A44A);
                    }).pos(0, 18)
                        .size(18, 3)));
        }
        return grid.child(scroll);
    }

    private ParentWidget<?> createBathDisplay() {
        FluidStack[] fluids = new FluidStack[IronProduct.values().length];
        for (IronProduct type : IronProduct.values()) {
            fluids[type.ordinal()] = IronBathFurnace.material(type)
                .getMolten(1);
        }
        return new ParentWidget<>().size(278, 174)
            .child(label(() -> text("vessel"), 4, 2, 98))
            .child(
                new IDrawable.DrawableWidget(tank(fluids)).name("bathContents")
                    .pos(4, 17)
                    .size(96, 140)
                    .tooltipAutoUpdate(true)
                    .tooltipDynamic(
                        t -> t.add(layerDescription(productName(), metal.getValue(), liquid.getValue()))
                            .newLine()
                            .add(
                                layerDescription(
                                    IronBathFurnace.material(IronProduct.SLAG)
                                        .getLocalizedName(),
                                    slag.getValue(),
                                    liquidSlag.getValue()))))
            .child(
                label(
                    () -> text(
                        "fill",
                        GTUtility.formatShortenedLong(metal.getValue() + slag.getValue()),
                        GTUtility.formatShortenedLong(capacity.getValue())),
                    4,
                    160,
                    98).scale(0.85f)
                        .tooltipAutoUpdate(true)
                        .tooltipDynamic(
                            t -> t.add(text("fill", metal.getValue() + slag.getValue(), capacity.getValue()))
                                .newLine()
                                .add(text("metal_capacity", metalCapacity.getValue()))))
            .child(
                label(
                    () -> text("temperature_range", temperature.getValue(), maximumTemperature.getValue()),
                    112,
                    2,
                    164).scale(0.85f))
            .child(
                new IDrawable.DrawableWidget(temperatureGauge()).pos(112, 17)
                    .size(160, 7)
                    .tooltipAutoUpdate(true)
                    .tooltipDynamic(
                        t -> t.add(text("temperature", temperature.getValue()))
                            .newLine()
                            .add(text("maximum_temperature", maximumTemperature.getValue()))))
            .child(label(() -> text("ratio", String.format(Locale.ROOT, "%.3f%%", carbonContent())), 112, 34, 164))
            .child(
                new IDrawable.DrawableWidget(compositionGauge()).name("bathComposition")
                    .pos(112, 48)
                    .size(160, 25))
            .child(label(() -> text("carbon", Math.round(carbon.getValue() / 1000.0)), 112, 80, 164))
            .child(label(() -> text("impurities", impurities.getValue()), 112, 96, 164))
            .child(label(() -> text("calcite", calcite.getValue(), calciteMaximum.getValue()), 112, 112, 164))
            .child(label(() -> text("power", power.getValue()), 112, 128, 164))
            .child(label(() -> text("slag", slag.getValue()), 112, 144, 164));
    }

    private IDrawable tank(FluidStack[] fluids) {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, 0xFF494A4D);
            GuiDraw.drawRect(x + 2, y + 2, width - 4, height - 4, 0xFFAAA7A0);
            GuiDraw.drawRect(x + 5, y + 3, width - 10, height - 9, 0xFF22252A);
            int innerHeight = height - 9;
            float heightPerMb = innerHeight / (float) Math.max(1, capacity.getValue());
            float bottom = y + height - 6;
            float layerTop = bottom;
            float warmth = Math.max(0, Math.min(1, (temperature.getValue() - 900) / 1300f));
            float pulse = 0.92f + 0.08f * (float) Math.sin(System.nanoTime() / 800_000_000.0);
            for (int layer = 0; layer < 2; layer++) {
                boolean slagLayer = layer == 1;
                int total = slagLayer ? slag.getValue() : metal.getValue();
                int molten = slagLayer ? liquidSlag.getValue() : liquid.getValue();
                layerTop -= total * heightPerMb;
                float solidHeight = (total - molten) * heightPerMb;
                float liquidHeight = molten * heightPerMb;
                FluidStack fluid = fluids[slagLayer ? IronProduct.SLAG.ordinal() : product.getValue()];
                if (solidHeight > 0) {
                    int tint = fluid.getFluid()
                        .getColor(fluid);
                    if (slagLayer) {
                        short[] solid = IronBathFurnace.material(IronProduct.SLAG).mRGBa;
                        tint = Color.rgb(solid[0], solid[1], solid[2]);
                    }
                    GlStateManager.color(Color.getRedF(tint), Color.getGreenF(tint), Color.getBlueF(tint), 1);
                    // The native 16 x 320 sheet contains twenty frames. Sample its first frame outside the animated
                    // atlas.
                    GuiDraw.drawTiledTexture(
                        IronBathFurnace.FROZEN_BATH_TEXTURE,
                        x + 5,
                        layerTop,
                        width - 10,
                        solidHeight,
                        0,
                        0,
                        1,
                        0.05f,
                        16,
                        16,
                        context.getCurrentDrawingZ());
                    GlStateManager.color(1, 1, 1, 1);
                    GuiDraw.drawRect(x + 5, layerTop, width - 10, solidHeight, glowColor(warmth, 0.45f, slagLayer));
                }
                if (liquidHeight > 0) {
                    GuiDraw.drawFluidTexture(
                        fluid,
                        x + 5,
                        layerTop + solidHeight,
                        width - 10,
                        liquidHeight,
                        context.getCurrentDrawingZ());
                    GuiDraw.drawRect(
                        x + 5,
                        layerTop + solidHeight,
                        width - 10,
                        liquidHeight,
                        glowColor(warmth, pulse, slagLayer));
                }
                if (slagLayer && total > 0) {
                    // The line marks the surface, while the textured layer keeps its true volume.
                    GuiDraw.drawRect(x + 5, layerTop, width - 10, 1, Color.lerp(0xFFB59C71, 0xFFFFD58A, warmth));
                }
            }
            for (int tick = 1; tick < 5; tick++) {
                GuiDraw.drawRect(x + 5, bottom - innerHeight * tick / 5f, 5, 1, 0xFFBDBBB4);
                GuiDraw.drawRect(x + width - 10, bottom - innerHeight * tick / 5f, 5, 1, 0xFFBDBBB4);
            }
        };
    }

    /** Temperature-driven glow over native fluid textures. */
    private static int glowColor(float warmth, float pulse, boolean slagLayer) {
        int red = slagLayer ? 0xFF863922 : 0xFFCC3822;
        int orange = slagLayer ? 0xFFD88E38 : 0xFFFFAC35;
        int hot = slagLayer ? 0xFFFFD58A : 0xFFFFF0BD;
        int color = warmth < 0.5f ? Color.lerp(red, orange, warmth * 2) : Color.lerp(orange, hot, (warmth - 0.5f) * 2);
        return Color.withAlpha(color, warmth * 0.72f * pulse);
    }

    private IDrawable temperatureGauge() {
        return (context, x, y, width, height, theme) -> {
            GuiDraw.drawRect(x, y, width, height, 0xFF34373B);
            float range = width - 2;
            int maximum = maximumTemperature.getValue();
            float span = Math.max(1, maximum - 300);
            GuiDraw.drawRect(x + 1, y + 1, range * Math.min(500, span) / span, height - 2, temperatureColor(800));
            for (int sample = 0; sample < INCANDESCENCE_TEMPERATURES.length - 1; sample++) {
                int kelvin = INCANDESCENCE_TEMPERATURES[sample];
                if (kelvin >= maximum) break;
                int end = Math.min(maximum, INCANDESCENCE_TEMPERATURES[sample + 1]);
                GuiDraw.drawHorizontalGradientRect(
                    x + 1 + range * (kelvin - 300) / span,
                    y + 1,
                    range * (end - kelvin) / span,
                    height - 2,
                    temperatureColor(kelvin),
                    temperatureColor(end));
            }
            GuiDraw.drawRect(
                x + 1 + (range - 1) * Math.max(0, Math.min(1, (temperature.getValue() - 300) / span)),
                y,
                1,
                height,
                0xFFFFFFFF);
        };
    }

    private static int temperatureColor(int kelvin) {
        int lower = 0;
        while (lower < INCANDESCENCE_TEMPERATURES.length - 2 && kelvin > INCANDESCENCE_TEMPERATURES[lower + 1]) lower++;
        float fraction = Math.max(
            0,
            Math.min(
                1,
                (kelvin - INCANDESCENCE_TEMPERATURES[lower])
                    / (float) (INCANDESCENCE_TEMPERATURES[lower + 1] - INCANDESCENCE_TEMPERATURES[lower])));
        int emission = Color.lerp(INCANDESCENCE_COLORS[lower], INCANDESCENCE_COLORS[lower + 1], fraction);
        // An ambient-gray fade makes the onset of visible incandescence readable
        return Color.lerp(0xFF52565B, emission, (kelvin - 800) / 600f);
    }

    private IDrawable compositionGauge() {
        return (context, x, y, width, height, theme) -> {
            int barHeight = 7;
            GuiDraw.drawRect(x, y, width, barHeight, 0xFF34373B);
            float start = x + 3;
            float range = width - 4;
            float selectedLeft = x + 1, selectedWidth = 2;
            IronProduct selected = PRODUCTS[product.getValue()];
            for (IronProduct type : PRODUCTS) {
                if (type == IronProduct.SLAG) continue;
                float left = type == IronProduct.IRON ? x + 1
                    : start + range * (float) (type.lowerCarbonRatio() * 144 / 5);
                float right = type == IronProduct.IRON ? start
                    : start + range * (float) Math.min(1, type.upperCarbonRatio() * 144 / 5);
                GuiDraw.drawRect(left, y + 1, right - left, barHeight - 2, type.displayColor());
                if (type == selected) {
                    selectedLeft = left;
                    selectedWidth = right - left;
                }
            }
            GuiDraw.drawRect(selectedLeft - 1, y - 1, selectedWidth + 2, barHeight + 2, 0xFF34373B);
            GuiDraw.drawRect(selectedLeft, y, selectedWidth, barHeight, selected.displayColor());
            float marker = product.getValue() == IronProduct.IRON.ordinal() ? x + 1
                : start + (range - 1) * (float) Math.min(1, carbonContent() / 5);
            GuiDraw.drawRect(marker, y, 1, barHeight, 0xFFFFFFFF);
            String name = productName();
            int textWidth = TextRenderer.getFontRenderer()
                .getStringWidth(name);
            float scale = Math.min(1, width / (float) Math.max(1, textWidth));
            float labelX = Math.max(
                x,
                Math.min(x + width - textWidth * scale, selectedLeft + selectedWidth / 2 - textWidth * scale / 2));
            GuiDraw.drawText(name, labelX, y + 12, scale, 0xFF292C30, false);
        };
    }

    private static String layerDescription(String name, int total, int molten) {
        String amount = text("layer", name, total);
        if (total > 0 && molten == 0) return text("layer_solid", amount);
        if (molten > 0 && molten < total) return text("layer_partial", amount, molten);
        return amount;
    }

    private double carbonContent() {
        return metal.getValue() > 0 ? carbon.getValue() / 1000.0 * 144 / metal.getValue() : 0;
    }

    private String productName() {
        return IronBathFurnace.material(PRODUCTS[product.getValue()])
            .getLocalizedName();
    }

    private static TextWidget<?> label(Supplier<String> value, int x, int y, int width) {
        return IKey.dynamic(value)
            .asWidget()
            .pos(x, y)
            .size(width, 12)
            .textAlign(Alignment.CenterLeft)
            .color(0xFF292C30)
            .shadow(false);
    }

    private static String text(String key, Object... arguments) {
        return StatCollector.translateToLocalFormatted("gtnhextranerf.iron_bath.gui." + key, arguments);
    }

    @Override
    protected int getBasePanelWidth() {
        return 286;
    }

    @Override
    protected int getTextBoxToInventoryGap() {
        return 18;
    }

    @Override
    protected int getBasePanelHeight() {
        return 338;
    }
}
