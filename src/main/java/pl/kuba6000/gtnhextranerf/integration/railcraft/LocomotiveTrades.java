package pl.kuba6000.gtnhextranerf.integration.railcraft;

import javax.annotation.Nullable;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;

/** Supplies the player's steel contribution to a locomotive assembly trade. */
public final class LocomotiveTrades {

    private LocomotiveTrades() {}

    @Nullable
    public static ItemStack steelPayment(ItemStack payment, ItemStack output) {
        if (payment == null || payment.getItem() != Items.emerald || output == null) return null;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(output.getItem());
        if (id == null || !"Railcraft".equals(id.modId) || !"cart.loco.steam.solid".equals(id.name)) return null;
        return GTOreDictUnificator.get(OrePrefixes.plate, Materials.Steel, 17);
    }
}
