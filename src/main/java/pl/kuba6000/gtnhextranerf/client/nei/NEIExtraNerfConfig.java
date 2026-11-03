package pl.kuba6000.gtnhextranerf.client.nei;

import codechicken.nei.ItemStackMap;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.Tags;
import pl.kuba6000.gtnhextranerf.components.ComponentItem;
import pl.kuba6000.gtnhextranerf.fabrication.FabricationItem;
import pl.kuba6000.gtnhextranerf.items.BlightedSoybean;
import pl.kuba6000.gtnhextranerf.loader.ComponentLoader;
import pl.kuba6000.gtnhextranerf.progression.ProgressionCertificateItem;
import pl.kuba6000.gtnhextranerf.progression.ProgressionCertificates;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Discovered by NEI on the client; hidden item-panel entries remain visible as recipe ingredients. */
@SideOnly(Side.CLIENT)
public final class NEIExtraNerfConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        API.hideItem(ItemStackMap.wildcard(BlightedSoybean.item()));
        API.registerRecipeHandler(new CertificateRecipeHandler());
        API.registerUsageHandler(new CertificateRecipeHandler());
        for (ProgressionCertificateItem item : ProgressionCertificates.items()) {
            API.hideItem(ItemStackMap.wildcard(item));
        }
        API.registerRecipeHandler(new UfoAssemblerRecipeHandler());
        API.registerUsageHandler(new UfoAssemblerRecipeHandler());
        API.addRecipeCatalyst(
            ComponentLoader.fabrication()
                .assembly(),
            UfoAssemblerRecipeHandler.class.getName());
        for (ComponentItem item : ComponentLoader.components()
            .items()) {
            API.hideItem(ItemStackMap.wildcard(item));
        }
        for (FabricationItem item : ComponentLoader.fabrication()
            .items()) {
            API.hideItem(ItemStackMap.wildcard(item));
        }
    }

    @SubscribeEvent
    public void registerHandlerInfo(NEIRegisterHandlerInfosEvent event) {
        event.registerHandlerInfo(
            new HandlerInfo.Builder(CertificateRecipeHandler.class.getName(), "GTNH Extra Nerf", GTNHExtraNerf.MODID)
                .setDisplayStack(ProgressionCertificates.stack(ProgressionTier.LV))
                .setWidth(CertificateRecipeHandler.WIDTH)
                .setHeight(CertificateRecipeHandler.HEIGHT)
                .setMultipleWidgetsAllowed(false)
                .setShowOverlayButton(false)
                .setShowFavoritesButton(false)
                .build());
        event.registerHandlerInfo(
            new HandlerInfo.Builder(UfoAssemblerRecipeHandler.class.getName(), "GTNH Extra Nerf", GTNHExtraNerf.MODID)
                .setDisplayStack(
                    ComponentLoader.fabrication()
                        .assembly())
                .setWidth(UfoAssemblerRecipeHandler.WIDTH)
                .setHeight(UfoAssemblerRecipeHandler.HEIGHT)
                .setMultipleWidgetsAllowed(false)
                .setShowOverlayButton(false)
                .setShowFavoritesButton(false)
                .build());
    }

    @Override
    public String getName() {
        return GTNHExtraNerf.MODID;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
