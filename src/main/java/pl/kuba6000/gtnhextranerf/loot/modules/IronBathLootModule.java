package pl.kuba6000.gtnhextranerf.loot.modules;

import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import pl.kuba6000.gtnhextranerf.audit.AuditCounts;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.loot.ItemKey;
import pl.kuba6000.gtnhextranerf.loot.LootModule;
import pl.kuba6000.gtnhextranerf.loot.LootValidationException;
import pl.kuba6000.gtnhextranerf.loot.NativeLootTables;

/** Keeps bath-dependent alloy rewards in their production chains. */
public final class IronBathLootModule implements LootModule {

    private static final ItemKey STAINLESS = ItemKey.of("gregtech:gt.metaitem.01", 11306);
    private static final String REWARD = "507bbd68-8c8c-4c27-9c6c-665d9d922cb5";
    private static final ItemKey DARK_SWORD = ItemKey.of("EnderIO:item.darkSteel_sword", 0);
    private static final String SWORD_REWARD = "2036b97f-8445-4c87-850f-a2bb1c0d3c51";

    @Override
    public AuditScope scope() {
        return AuditScope.REMOVE_IRON_BATH_ALLOY_LOOT;
    }

    @Override
    public AuditCounts apply(LootGroups tables) {
        NativeLootTables editor = new NativeLootTables(tables);
        Drop reward = editor.require(6, REWARD, STAINLESS);
        if (reward.getAmount() != 16 || reward.getChance() != 75 || !reward.getIsRandomAmount())
            throw new LootValidationException("Unexpected Stainless Steel reward");
        Drop sword = editor.require(8, SWORD_REWARD, DARK_SWORD);
        if (sword.getAmount() != 1 || sword.getChance() != 10 || sword.getIsRandomAmount())
            throw new LootValidationException("Unexpected recyclable Dark Steel sword reward");
        editor.remove(6, reward);
        editor.remove(8, sword);
        AuditCounts result = editor.commit();
        verify(tables);
        return result;
    }

    @Override
    public void verify(LootGroups tables) {
        for (LootGroup group : tables.getLootTable()) for (Drop drop : group.getDrops()) if (NativeLootTables.item(drop)
            .equals(STAINLESS)
            || NativeLootTables.item(drop)
                .equals(DARK_SWORD))
            throw new LootValidationException("Alloy reward bypasses bath production");
    }
}
