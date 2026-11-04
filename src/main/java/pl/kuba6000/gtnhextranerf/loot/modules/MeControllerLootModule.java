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

/** Changes the real ELB source entries before publication, on both physical sides. */
public final class MeControllerLootModule implements LootModule {

    public static final ItemKey ITEM = ItemKey.of("appliedenergistics2:tile.BlockController", 0);
    public static final String LIMIT_ID = "gtnhextranerf:me_controller";
    public static final int GLOBAL_ALLOWANCE = 1;
    private static final String BASIC = "c86156c4-b72c-4ada-9ad2-3bbb229c5664";
    private static final String GOOD = "13a284df-df59-4389-8543-9c3b65fc28dc";

    public AuditScope scope() {
        return AuditScope.MODIFY_ME_CONTROLLER_LOOT;
    }

    public AuditCounts apply(LootGroups tables) {
        NativeLootTables editor = new NativeLootTables(tables);
        Drop basic = editor.require(37, BASIC, ITEM);
        Drop good = editor.require(38, GOOD, ITEM);
        requireAmount(basic, 1);
        requireAmount(good, 4);
        requireSources(tables);
        editor.amount(38, good, 1);
        AuditCounts changes = editor.commit();
        verify(tables);
        return changes;
    }

    public void verify(LootGroups tables) {
        NativeLootTables editor = new NativeLootTables(tables);
        requireAmount(editor.require(37, BASIC, ITEM), 1);
        requireAmount(editor.require(38, GOOD, ITEM), 1);
        requireSources(tables);
    }

    private static void requireAmount(Drop drop, int amount) {
        if (drop.getAmount() != amount || drop.getIsRandomAmount() || drop.getChance() <= 0)
            throw new LootValidationException("Unexpected ME Controller loot definition: " + drop.getIdentifier());
    }

    private static void requireSources(LootGroups tables) {
        int count = 0;
        for (LootGroup group : tables.getLootTable()) for (Drop drop : group.getDrops()) if (NativeLootTables.item(drop)
            .equals(ITEM)) count++;
        if (count != 2)
            throw new LootValidationException("Expected exactly two ME Controller loot sources; got " + count);
    }
}
