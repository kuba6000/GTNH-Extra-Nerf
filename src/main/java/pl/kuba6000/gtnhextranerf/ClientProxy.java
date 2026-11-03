package pl.kuba6000.gtnhextranerf;

import java.awt.Desktop;
import java.net.URI;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.widget.Widget;

import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import pl.kuba6000.gtnhextranerf.client.BrainrotTooltips;
import pl.kuba6000.gtnhextranerf.client.ClientStartupErrors;
import pl.kuba6000.gtnhextranerf.client.ComponentItemRenderer;
import pl.kuba6000.gtnhextranerf.client.MainMenuBranding;
import pl.kuba6000.gtnhextranerf.client.StartupErrorRequest;
import pl.kuba6000.gtnhextranerf.client.gui.DrawingExamWidget;
import pl.kuba6000.gtnhextranerf.client.nei.CertificateRecipeHandler;
import pl.kuba6000.gtnhextranerf.client.nei.LootAllowancePreview;
import pl.kuba6000.gtnhextranerf.client.nei.NEIExtraNerfConfig;
import pl.kuba6000.gtnhextranerf.components.ComponentItem;
import pl.kuba6000.gtnhextranerf.loader.ComponentLoader;
import pl.kuba6000.gtnhextranerf.progression.EnergyHatchCertification;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;
import pl.kuba6000.gtnhextranerf.progression.exam.CertificationExams;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamSyncHandler;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        FMLCommonHandler.instance()
            .bus()
            .register(LootAllowancePreview.INSTANCE);
        StartupErrorRequest
            .developmentPreview(
                GTNHExtraNerf.isDevelopmentEnvironment(),
                Boolean.getBoolean(StartupErrorRequest.DEV_PREVIEW_PROPERTY))
            .ifPresent(ClientStartupErrors::request);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ClientCommandHandler.instance.registerCommand(new EnergyHatchCertification.OpenCertificateCommand());
        for (ComponentItem item : ComponentLoader.components()
            .items()) {
            MinecraftForgeClient.registerItemRenderer(item, new ComponentItemRenderer(item));
        }
        if (Loader.isModLoaded("NotEnoughItems")) MinecraftForge.EVENT_BUS.register(new NEIExtraNerfConfig());
        BrainrotTooltips.register();
        if (Loader.isModLoaded("custommainmenu")) MinecraftForge.EVENT_BUS.register(new MainMenuBranding());
    }

    @Override
    protected void handleIntegrityFailure(RuntimeException failure) {
        ClientStartupErrors.request(StartupErrorRequest.failure(failure));
    }

    @Override
    public void openUrl(String url) {
        try {
            Desktop.getDesktop()
                .browse(new URI(url));
        } catch (Throwable failure) {
            // No browser (or no desktop) to open: the address is on the clipboard instead.
            GuiScreen.setClipboardString(url);
            GTNHExtraNerf.LOG.info("Could not open {}; copied it to the clipboard", url);
        }
    }

    @Override
    public void showCertificate(ProgressionTier tier) {
        if (Loader.isModLoaded("NotEnoughItems"))
            GuiCraftingRecipe.openRecipeGui(CertificateRecipeHandler.ID, tier.id());
    }

    @Override
    public void receiveControllerAllowance(int remaining, MessageContext context) {
        LootAllowancePreview.receive(
            context.getClientHandler()
                .getNetworkManager(),
            remaining);
    }

    @Override
    public void openExam(ProgressionTier tier) {
        CertificationExams.factory(tier)
            .openClient();
    }

    @Override
    public Widget<?> createExamCanvas(ExamSyncHandler exam) {
        return new DrawingExamWidget(exam);
    }
}
