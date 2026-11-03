package pl.kuba6000.gtnhextranerf.audit;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import gregtech.api.enums.Mods;
import gregtech.common.tileentities.machines.basic.MTEMassfabricator;

/** Catalog of audit definitions; contains no session state or global result lookups. */
public enum AuditScope implements AuditDefinition {

    ADD_MAGICNETIZED_MANASTEEL_ROD(AuditExpectation.exact()
        .added(1)
        .build()),
    MODIFY_MV_MOTOR_HAND_CRAFTING(modified(1)),
    MODIFY_MV_MOTOR_ASSEMBLER(modified(1)),

    VERIFY_ASSEMBLY_LINE_SPATIAL_DIMENSION(Mods.AppliedEnergistics2::isModLoaded, AuditExpectation.exact()
        .build()),

    VERIFY_LIGHTNING_RODS(AuditExpectation.exact()
        .build()),

    VERIFY_VOID_MINER_GAS_USAGE(AuditExpectation.exact()
        .build()),

    ADD_NAQUADAH_PURIFICATION_WATER(modified(9)),
    VERIFY_LEGACY_FUSION_COILS(AuditExpectation.exact()
        .build()),
    MODIFY_HIGHEST_OZONATION_RECIPE(modified(1)),
    REMOVE_FLOCCULATION_WASTE_RECYCLING(removed(1)),
    VERIFY_OZONE_SAFETY(AuditExpectation.exact()
        .build()),
    MODIFY_HIGH_TIER_HULL_INSULATION(modified(12)),
    MODIFY_AMERICIUM_BOULE(AuditExpectation.exact()
        .modified(1)
        .removed(7)
        .build()),
    MODIFY_PCB_FACTORY_WET_BIO_PLASMA(modified(21)),
    ADD_MINERAL_MIX_TO_RAW_BIO_MEDIUM(modified(3)),
    MODIFY_MUTATED_SOLDER_STAR(modified(1)),
    VERIFY_PLASMA_FORGE_COILS(AuditExpectation.exact()
        .build()),
    ADD_NEPTUNIUM_FLUORIDE_TO_EUROPIUM_BOULE(modified(8)),
    REMOVE_EV_CABLE_ASSEMBLER_RECIPES(AuditExpectation.atLeastRemoved(1)),
    MOVE_RUTHENIUM_TO_COOLANT_TOWER(AuditExpectation.exact()
        .removed(1)
        .added(1)
        .build()),
    VERIFY_COOLANT_TOWER(AuditExpectation.exact()
        .build()),
    REMOVE_UNAMPLIFIED_UU_RECIPES(() -> AuditExpectation.exact()
        .removed(MTEMassfabricator.sRequiresUUA ? 4 : 5)
        .build()),
    VERIFY_MASS_FABRICATOR_AMPLIFIER(AuditExpectation.exact()
        .build()),
    REDIRECT_HIGH_TIER_RAW_CASINGS(Mods.Chisel::isModLoaded, modified(8)),
    VERIFY_RAW_CASING_FINISHING(AuditExpectation.exact()
        .build()),
    VERIFY_EVERGLADES_MACHINE_BAN(AuditExpectation.exact()
        .build()),
    VERIFY_SINGLEBLOCK_TRANSFORMER_PORTS(AuditExpectation.exact()
        .build()),

    REROUTE_TUNGSTEN_STEEL_PRODUCTION(AuditExpectation.exact()
        .modified(4)
        .added(9)
        .build()),

    ADD_BRAINROTTER_RECIPES(AuditExpectation.exact()
        .added(5)
        .build()),
    VERIFY_BRAINROTTER(AuditExpectation.exact()
        .build()),

    VERIFY_BRAINROT_MATERIALS(AuditExpectation.exact()
        .build()),

    VERIFY_PROGRESSION_CERTIFICATES(AuditExpectation.exact()
        .build()),
    VERIFY_ENERGY_HATCH_CERTIFICATION(AuditExpectation.exact()
        .build()),
    VERIFY_ELECTRICAL_SAFETY_HOOKS(AuditExpectation.exact()
        .build()),
    VERIFY_STEAM_SAFETY_HOOKS(AuditExpectation.exact()
        .build()),
    VERIFY_CERTIFICATION_EXAM(AuditExpectation.exact()
        .build()),
    VERIFY_CERTIFICATION_COSTS(AuditExpectation.exact()
        .build()),

    MODIFY_ME_CONTROLLER_LOOT(Mods.EnhancedLootBags::isModLoaded, modified(1)),
    VERIFY_NATIVE_LOOT_TABLES(Mods.EnhancedLootBags::isModLoaded, AuditExpectation.exact()
        .build()),

    ADD_PPS_FOIL_TO_CABLE_MACHINE_CRAFTING(AuditExpectation.atLeastModified(1)),
    RING_FROM_LONG_STICK_HAND_CRAFTING(AuditExpectation.atLeastModified(1)),
    SET_SMALL_NAQ_ORE_TO_1(AuditExpectation.atLeastModified(1)),
    SET_GLOWSTONE_BREEDER_REQUIRED_PULSE_TO_1000(modified(1)),
    REDUCE_THORIUM_URANIUM_MOX_CELL_LIFETIME(AuditExpectation.atLeastModified(1)),
    SCALE_NANO_QUANTUM_CIRCUIT_ASSEMBLER_INPUTS(AuditExpectation.exact()
        .modified(4)
        .added(4)
        .build()),
    RING_FROM_LONG_STICK_WERKSTOFF_HAND_CRAFTING(AuditExpectation.addedPerModification()),
    SET_DENSE_NETHERITE_ASSEMBLY_LINE_AMOUNT_TO_64(modified(1)),
    REDUCE_BENZENE_FUEL_VALUE_TO_QUARTER(modified(1)),
    REDUCE_PHENOL_FUEL_VALUE_TO_QUARTER(modified(1)),
    REMOVE_IRON_LV_MOTOR_HAND_CRAFTING(removed(1)),
    REMOVE_IRON_LV_MOTOR_ASSEMBLER_RECIPE(removed(1)),
    MARK_STEEL_LV_MOTOR_HAND_CRAFTING_DISMANTLEABLE(AuditExpectation.exact()
        .modified(1)
        .added(1)
        .build()),
    REDUCE_BRONZE_DUST_HAND_CRAFTING_OUTPUT(modified(1)),
    MODIFY_LARGE_CHEMICAL_REACTOR_HAND_CRAFTING(modified(1)),
    MODIFY_FLINT_AND_STEEL_HAND_CRAFTING(Mods.NewHorizonsCoreMod::isModLoaded, modified(1)),
    MODIFY_TOOL_STATION_SLAB_HAND_CRAFTING(Mods.NewHorizonsCoreMod::isModLoaded, modified(1)),
    MODIFY_LV_CIRCUIT_ASSEMBLER_HAND_CRAFTING(modified(1)),
    REMOVE_GOOD_CIRCUIT_HAND_CRAFTING(Mods.NewHorizonsCoreMod::isModLoaded, removed(1)),
    REMOVE_ILMENITE_CHEMICAL_BATH_RECIPES(removed(2)),
    REMOVE_RUBY_JUICE_RECIPES(removed(18)),
    MODIFY_ILMENITE_SLAG_CENTRIFUGE_RECIPE(modified(2)),
    RESTRICT_PRIMITIVE_BLAST_FURNACE_STEEL_FUELS(removed(39)),
    MODIFY_STEEL_BLAST_FURNACE_RECIPES(AuditExpectation.exact()
        .modified(2)
        .added(1)
        .removed(2)
        .build()),
    MODIFY_ALUMINIUM_BLAST_FURNACE_RECIPES(AuditExpectation.exact()
        .modified(5)
        .removed(15)
        .build()),
    MODIFY_TINKERS_ALUMINIUM_BLAST_FURNACE_RECIPES(Mods.TinkerConstruct::isModLoaded, AuditExpectation.exact()
        .modified(4)
        .removed(14)
        .build()),
    MODIFY_ILMENITE_BLAST_FURNACE_RECIPE(modified(1)),
    MODIFY_TITANIUM_BLAST_FURNACE_RECIPE(modified(1)),
    MODIFY_INDIUM_CONCENTRATE_RECIPES(modified(2)),
    REDUCE_TPV_MIXER_OUTPUT(modified(2)),
    MODIFY_PLUTONIUM_DECAY_RECIPES(AuditExpectation.exact()
        .modified(1)
        .added(1)
        .build()),
    MODIFY_SILICON_LINE_RECIPES(modified(7)),
    REMOVE_SILICON_WAFER_WATER_CUTTER_RECIPES(removed(2)),
    MODIFY_LV_OIL_DISTILLERY_RECIPES(modified(3)),
    MODIFY_ENDSTONE_CENTRIFUGE_RECIPE(modified(2)),
    MODIFY_STABALLOY_CENTRIFUGE_RECIPES(Mods.GTPlusPlus::isModLoaded, modified(2)),
    MODIFY_EUROPIUM_AMERICIUM_FUSION_RECIPES(modified(4)),
    MODIFY_AMERICIUM_PLASMA_FUSION_IGNITION(modified(1)),
    MODIFY_QUANTUM_STAR_RECIPE(modified(1)),
    MODIFY_TELLURIUM_BLAST_FURNACE_CHANCE(modified(1)),
    MODIFY_PLATINUM_SLUDGE_RECIPES(modified(3)),
    MODIFY_SAMARIUM_RECIPES(modified(6)),
    MODIFY_EXTREME_HEAT_EXCHANGER_HAND_CRAFTING(AuditExpectation.exact()
        .added(1)
        .removed(1)
        .build()),
    MODIFY_SUPERCONDUCTOR_COIL_RECIPE(modified(2)),
    MODIFY_LARGE_CHEMICAL_REACTOR_ASSEMBLER_RECIPE(modified(1)),
    MODIFY_ELITE_CIRCUIT_RECIPES(AuditExpectation.exact()
        .added(2)
        .removed(2)
        .build()),
    MODIFY_PPIC_WAFER_RECIPES(modified(2)),
    REMOVE_DIODE_ASSEMBLER_RECIPES(removed(4)),
    MODIFY_DIAMOND_SAWBLADE_RECIPE(modified(1)),
    MODIFY_DIAMOND_SAWBLADE_HAND_CRAFTING(modified(1)),
    MODIFY_MACHINE_CASING_RECIPES(modified(10)),
    REPLACE_MACHINE_CASING_HAND_CRAFTING(modified(10)),
    AE2_TPV_CRAFTING(Mods.AppliedEnergistics2::isModLoaded, AuditExpectation.atLeastModified(1)),
    AE2_TPV_MACHINES(Mods.AppliedEnergistics2::isModLoaded, AuditExpectation.atLeastModified(1)),
    AE2_REMOVE_CONTROLLER(Mods.AppliedEnergistics2::isModLoaded, removed(2)),
    AE2_ADD_CONTROLLER_ASSEMBLY_LINE(Mods.AppliedEnergistics2::isModLoaded, AuditExpectation.exact()
        .added(1)
        .build()),
    AE2_SUNNARIUM_PATTERN(Mods.AppliedEnergistics2::isModLoaded, modified(1)),
    MODIFY_CRAFTING_INPUT_PROXY_RECIPE(
        () -> Mods.GTNHIntergalactic.isModLoaded() && Mods.AppliedEnergistics2.isModLoaded()
            && Mods.AE2FluidCraft.isModLoaded(),
        modified(1)),
    MODIFY_ADVANCED_CIRCUIT_INPUTS(modified(2)),
    MODIFY_INTEGRATED_LOGIC_CIRCUIT_INPUTS(modified(1)),
    MODIFY_GOOD_CIRCUIT_INPUTS(modified(1)),
    MODIFY_CUPRONICKEL_COIL_INPUTS(modified(2)),
    MODIFY_HEATING_COIL_INPUTS(modified(10)),
    MODIFY_MV_ENERGY_HATCH_INPUTS(modified(1)),
    REMOVE_MV_ENERGY_HATCH_HAND_CRAFTING(removed(1)),
    MODIFY_RAW_NEUTRONIUM_RECIPE(modified(2)),
    MODIFY_INDIUM_COMB_RECIPES(Mods.Forestry::isModLoaded, modified(5)),
    MODIFY_PRASEODYMIUM_CENTRIFUGE_RECIPE(Mods.Forestry::isModLoaded, modified(2)),
    MODIFY_VACUUM_FURNACE_INDIUM_RECIPES(Mods.Forestry::isModLoaded, modified(2)),
    REMOVE_SLIME_EXTRACTOR_RECIPES(() -> AuditExpectation.exact()
        .removed(Mods.TinkerConstruct.isModLoaded() ? 5 : 1)
        .build()),
    REMOVE_SLIME_CENTRIFUGE_RECIPES(Mods.TinkerConstruct::isModLoaded, removed(6)),
    REMOVE_SLIME_MULTIBLOCK_CENTRIFUGE_RECIPES(
        () -> Mods.TinkerConstruct.isModLoaded() && Mods.GTPlusPlus.isModLoaded(), removed(6)),
    REPLACE_SLIME_STICKY_PISTON_ASSEMBLER_RECIPE(Mods.ElectroMagicTools::isModLoaded, AuditExpectation.exact()
        .added(1)
        .removed(1)
        .build()),
    REPLACE_SLIME_STICKY_PISTON_HAND_CRAFTING(
        () -> Mods.NewHorizonsCoreMod.isModLoaded() && Mods.ElectroMagicTools.isModLoaded(), AuditExpectation.exact()
            .added(1)
            .removed(1)
            .build()),
    REPLACE_SLIME_COATED_BOARD_HAND_CRAFTING(
        () -> Mods.NewHorizonsCoreMod.isModLoaded() && Mods.ElectroMagicTools.isModLoaded(),
        () -> AuditExpectation.exact()
            .added(2)
            .removed(Mods.TinkerConstruct.isModLoaded() ? 8 : 2)
            .build()),
    REPLACE_SLIME_RESISTOR_HAND_CRAFTING(
        () -> Mods.NewHorizonsCoreMod.isModLoaded() && Mods.ElectroMagicTools.isModLoaded(),
        () -> AuditExpectation.exact()
            .added(4)
            .removed(Mods.TinkerConstruct.isModLoaded() ? 16 : 4)
            .build());

    private final BooleanSupplier active;
    private final Supplier<AuditExpectation> expectation;

    AuditScope(AuditExpectation expectation) {
        this(() -> true, () -> expectation);
    }

    AuditScope(Supplier<AuditExpectation> expectation) {
        this(() -> true, expectation);
    }

    AuditScope(BooleanSupplier active, AuditExpectation expectation) {
        this(active, () -> expectation);
    }

    AuditScope(BooleanSupplier active, Supplier<AuditExpectation> expectation) {
        this.active = active;
        this.expectation = expectation;
    }

    @Override
    public String id() {
        return name();
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    @Override
    public AuditExpectation expectation() {
        return expectation.get();
    }

    private static AuditExpectation modified(int count) {
        return AuditExpectation.exact()
            .modified(count)
            .build();
    }

    private static AuditExpectation removed(int count) {
        return AuditExpectation.exact()
            .removed(count)
            .build();
    }
}
