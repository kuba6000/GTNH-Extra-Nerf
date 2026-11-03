package pl.kuba6000.gtnhextranerf.telemetry;

import net.minecraft.entity.player.EntityPlayerMP;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.GuiData;
import com.cleanroommc.modularui.factory.SimpleGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;

import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.teams.TeamTracking;

/** The question every world is asked once: three choices, and the policies they refer to. */
public final class ConsentGui implements IGuiHolder<GuiData> {

    public static final String FACTORY_ID = GTNHExtraNerf.MODID + ".telemetry_consent";
    public static final String PRIVACY_URL = ServerTelemetry.PRIVACY_URL;
    static final int WIDTH = 320;
    static final int HEIGHT = 264;
    private static final int TEXT_WIDTH = WIDTH - 20;
    private static final int LINK_COLOR = 0xFF1F5FBF;
    private static SimpleGuiFactory factory;

    private ConsentGui() {}

    /** Registers the window; called once while the mod starts. */
    public static void register() {
        if (factory != null) throw new IllegalStateException("The consent window is already registered");
        factory = new SimpleGuiFactory(FACTORY_ID, new ConsentGui());
    }

    public static SimpleGuiFactory factory() {
        if (factory == null) throw new IllegalStateException("The consent window has not been registered");
        return factory;
    }

    @Override
    public ModularPanel buildUI(GuiData data, PanelSyncManager syncManager, UISettings settings) {
        settings.getRecipeViewerSettings()
            .disable();
        ConsentSyncHandler consent = syncManager.isClient() ? new ConsentSyncHandler()
            : new ConsentSyncHandler((EntityPlayerMP) data.getPlayer());
        syncManager.syncValue(ConsentSyncHandler.SYNC_ID, consent);
        ModularPanel panel = ModularPanel.defaultPanel("telemetry_consent", WIDTH, HEIGHT);
        panel.child(
            IKey.lang("gtnhextranerf.consent.title")
                .asWidget()
                .pos(10, 8)
                .size(TEXT_WIDTH, 12));
        panel.child(paragraph("gtnhextranerf.consent.minimal", 24, 58));
        panel.child(paragraph("gtnhextranerf.consent.team", 86, 38));
        panel.child(paragraph("gtnhextranerf.consent.scope", 128, 38));
        panel.child(link("gtnhextranerf.consent.link.privacy", PRIVACY_URL, 10, 170));
        panel.child(link("gtnhextranerf.consent.link.team", TeamTracking.POLICY_URL, 165, 170));
        panel.child(choice("gtnhextranerf.consent.decline", ConsentPrompt.DECLINE, consent, syncManager, 190));
        panel.child(choice("gtnhextranerf.consent.send_minimal", ConsentPrompt.MINIMAL, consent, syncManager, 214));
        panel.child(choice("gtnhextranerf.consent.send_team", ConsentPrompt.TEAM, consent, syncManager, 238));
        return panel;
    }

    private static TextWidget<?> paragraph(String key, int top, int height) {
        return IKey.lang(key)
            .asWidget()
            .pos(10, top)
            .size(TEXT_WIDTH, height)
            .maxWidth(TEXT_WIDTH);
    }

    private static ButtonWidget<?> link(String key, String url, int left, int top) {
        return new ButtonWidget<>().pos(left, top)
            .size(145, 12)
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY)
            .overlay(
                IKey.lang(key)
                    .color(LINK_COLOR))
            .onMousePressed(button -> {
                if (button != 0) return false;
                GTNHExtraNerf.proxy.openUrl(url);
                return true;
            });
    }

    private static ButtonWidget<?> choice(String key, int choice, ConsentSyncHandler consent,
        PanelSyncManager syncManager, int top) {
        return new ButtonWidget<>().pos(10, top)
            .size(TEXT_WIDTH, 20)
            .overlay(IKey.lang(key))
            .onMousePressed(button -> {
                if (button != 0 || !syncManager.isClient()) return false;
                consent.choose(choice);
                return true;
            });
    }
}
