package pl.kuba6000.gtnhextranerf.loader;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import eu.usrv.enhancedlootbags.EnhancedLootBags;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup;
import eu.usrv.enhancedlootbags.core.serializer.LootGroups.LootGroup.Drop;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.audit.AuditManager;
import pl.kuba6000.gtnhextranerf.audit.AuditReport;
import pl.kuba6000.gtnhextranerf.audit.AuditScope;
import pl.kuba6000.gtnhextranerf.audit.AuditSession;
import pl.kuba6000.gtnhextranerf.loot.LootModule;
import pl.kuba6000.gtnhextranerf.loot.LootPatchRunner;
import pl.kuba6000.gtnhextranerf.loot.LootValidationException;
import pl.kuba6000.gtnhextranerf.loot.NativeLootTables;
import pl.kuba6000.gtnhextranerf.loot.modules.IronBathLootModule;
import pl.kuba6000.gtnhextranerf.loot.modules.MeControllerLootModule;

/** A single post-deserialization pipeline, using only native ELB tables. */
public final class LootLoader {

    private static final List<LootModule> MODULES = Arrays
        .asList(new MeControllerLootModule(), new IronBathLootModule());
    private static boolean sealed;
    private static int initialLoads;
    private static boolean developmentUnavailable;

    private LootLoader() {}

    public static void patch(LootGroups tables) {
        List<AuditScope> scopes = MODULES.stream()
            .map(LootModule::scope)
            .collect(Collectors.toList());
        scopes.add(AuditScope.VERIFY_NATIVE_LOOT_TABLES);
        AuditSession session = sealed ? new AuditSession(scopes) : AuditManager.session();
        try {
            for (LootModule module : MODULES) {
                if (!LootPatchRunner
                    .apply(module, tables, session, !sealed && GTNHExtraNerf.isDevelopmentEnvironment())) {
                    developmentUnavailable = true;
                    initialLoads++;
                    GTNHExtraNerf.LOG.warn(
                        "Development loot validation failed for {}. Restored native tables; "
                            + "continuing startup with lootbag opening disabled. See the startup audit for details.",
                        module.scope());
                    return;
                }
            }
            NativeLootTables.refreshWeights(tables);
            if (sealed) {
                session.run(AuditScope.VERIFY_NATIVE_LOOT_TABLES, handle -> verify(tables));
                AuditReport report = session.finish();
                if (!report.accepted()) throw new IllegalStateException(report.failureReport());
                GTNHExtraNerf.LOG.info("Reloaded native loot tables: {}", report.summary());
            } else initialLoads++;
        } catch (RuntimeException failure) {
            if (sealed) failure.addSuppressed(
                new IllegalStateException(
                    session.finish()
                        .failureReport()));
            throw failure;
        }
    }

    public static void verifyPublished() {
        sealed = true;
        if (developmentUnavailable()) {
            AuditManager.run(
                AuditScope.VERIFY_NATIVE_LOOT_TABLES,
                handle -> handle
                    .failure(new LootValidationException("Development loot patch failed; lootbag opening disabled")));
            return;
        }
        try {
            AuditManager.run(AuditScope.VERIFY_NATIVE_LOOT_TABLES, handle -> {
                if (initialLoads != 1)
                    throw new IllegalStateException("Expected one native loot load hook; got " + initialLoads);
                String version = cpw.mods.fml.common.Loader.instance()
                    .getIndexedModList()
                    .get("enhancedlootbags")
                    .getVersion();
                if (!"1.3.5".equals(version)) throw new LootValidationException("Unsupported ELB " + version);
                verify(EnhancedLootBags.LootGroupHandler.getLootGroups());
            });
        } catch (LootValidationException failure) {
            if (!GTNHExtraNerf.isDevelopmentEnvironment()) throw failure;
            developmentUnavailable = true;
            GTNHExtraNerf.LOG.warn("Development loot verification failed; lootbag opening disabled", failure);
        }
    }

    public static boolean developmentUnavailable() {
        return developmentUnavailable && GTNHExtraNerf.isDevelopmentEnvironment();
    }

    public static void verify(LootGroups tables) {
        for (LootModule module : MODULES) module.verify(tables);
        NativeLootTables.verifyWeights(tables);
        for (LootGroup group : tables.getLootTable()) for (Drop drop : group.getDrops()) {
            if (!NativeLootTables.item(drop)
                .equals(MeControllerLootModule.ITEM)) continue;
            ItemStack stack = drop.getItemStack();
            if (stack == null || stack.stackSize != 1
                || stack.getItemDamage() != 0
                || !MeControllerLootModule.ITEM.registryName()
                    .equals(Item.itemRegistry.getNameForObject(stack.getItem())))
                throw new LootValidationException("Native controller reward cannot be materialized correctly");
        }
    }
}
