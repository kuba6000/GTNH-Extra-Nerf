package pl.kuba6000.gtnhextranerf.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.item.EnumRarity;

import org.junit.jupiter.api.Test;

import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import eu.usrv.enhancedlootbags.core.serializer.LootGroupsFactory;
import pl.kuba6000.gtnhextranerf.loot.modules.IronBathLootModule;

class IronBathLootModuleTest {

    @Test
    void removesTheNativeSteelRewardAndPreservesOtherRewardsAndWeights() {
        LootGroupsFactory factory = new LootGroupsFactory();
        LootGroups tables = new LootGroups();
        LootGroup group = factory.createLootGroup(6, "HV", EnumRarity.common, 1, 2, false);
        group.getDrops()
            .add(
                factory.createDrop(
                    "gregtech:gt.metaitem.01:11306",
                    "507bbd68-8c8c-4c27-9c6c-665d9d922cb5",
                    "",
                    16,
                    true,
                    75,
                    0,
                    ""));
        Drop retained = factory.createDrop("minecraft:diamond", "diamond", 1, false, 10, 0);
        group.getDrops()
            .add(retained);
        group.updateMaxWeight();
        tables.getLootTable()
            .add(group);
        LootGroup iv = factory.createLootGroup(8, "IV", EnumRarity.common, 1, 2, false);
        iv.getDrops()
            .add(
                factory.createDrop(
                    "EnderIO:item.darkSteel_sword",
                    "2036b97f-8445-4c87-850f-a2bb1c0d3c51",
                    "",
                    1,
                    false,
                    10,
                    1,
                    ""));
        iv.updateMaxWeight();
        tables.getLootTable()
            .add(iv);
        IronBathLootModule module = new IronBathLootModule();
        assertEquals(
            2,
            module.apply(tables)
                .removed());
        assertEquals(
            0,
            iv.getDrops()
                .size());
        assertEquals(
            1,
            group.getDrops()
                .size());
        assertSame(
            retained,
            group.getDrops()
                .get(0));
        assertEquals(10, group.getMaxWeight());
        module.verify(tables);
        group.getDrops()
            .add(factory.createDrop("gregtech:gt.metaitem.01:11306", "other", 1, false, 1, 0));
        assertThrows(LootValidationException.class, () -> module.verify(tables));
    }
}
