package pl.kuba6000.gtnhextranerf.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;

/** Uses Forge's non-dismissable error screen; no menu/world navigation is provided. */
@SideOnly(Side.CLIENT)
public final class StartupErrorDisplay extends CustomModLoadingErrorDisplayException {

    private final StartupErrorRequest request;

    public StartupErrorDisplay(StartupErrorRequest request) {
        super(request.supportMessage(), request.cause());
        this.request = request;
    }

    /**
     * This screen must work even when the language files are not available yet, so the English text is the fallback.
     */
    private static String text(String key, String english, Object... arguments) {
        return StatCollector.canTranslate(key) ? StatCollector.translateToLocalFormatted(key, arguments) : english;
    }

    @Override
    public void initGui(GuiErrorScreen screen, FontRenderer font) {
        GTNHExtraNerf.LOG.info("[Extra Nerf] Startup error screen initialized (preview={})", request.isPreview());
    }

    @Override
    public void drawScreen(GuiErrorScreen screen, FontRenderer font, int mouseX, int mouseY, float partialTicks) {
        int width = Math.max(1, Math.min(480, screen.width - 32));
        int left = (screen.width - width) / 2;
        int top = Math.max(12, (screen.height - 150) / 2);
        screen.drawCenteredString(
            font,
            EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD
                + text("gtnhextranerf.error.title", "Extra Nerf cannot start"),
            screen.width / 2,
            top,
            0xFFFFFF);
        int y = top + 28;
        String support = text(
            "gtnhextranerf.error.supported",
            request.supportMessage(),
            GTNHExtraNerf.SUPPORTED_GTNH_VERSION);
        font.drawSplitString(support, left, y, width, 0xFFFFFF);
        y += font.splitStringWidth(support, width) + 14;
        String explanation = text(
            "gtnhextranerf.error.explanation",
            "The required recipe changes could not be verified. Please use the supported pack version with its original mods and configuration.");
        font.drawSplitString(explanation, left, y, width, 0xCCCCCC);
        y += font.splitStringWidth(explanation, width) + 14;
        font.drawSplitString(
            text(
                "gtnhextranerf.error.logs",
                "See logs/latest.log for the full error report. Close the game before changing your installation."),
            left,
            y,
            width,
            0xAAAAAA);
        if (request.isPreview()) {
            screen.drawCenteredString(
                font,
                text("gtnhextranerf.error.preview", "Development preview - forced by JVM flag"),
                screen.width / 2,
                screen.height - 18,
                0xFFCC55);
        }
    }
}
