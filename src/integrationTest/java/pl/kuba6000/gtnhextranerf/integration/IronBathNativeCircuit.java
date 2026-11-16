package pl.kuba6000.gtnhextranerf.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import pl.kuba6000.gtnhextranerf.integration.IronBathRedstoneLab.Hardware;
import pl.kuba6000.gtnhextranerf.integration.IronBathRedstoneLab.Position;

/** Places the native redstone controller and its material-sampling circuits. */
final class IronBathNativeCircuit {

    static final class Net {

        final String name;
        final int colour;

        Net(String name, int colour) {
            this.name = name;
            this.colour = colour;
        }
    }

    static final class Bundle {

        final String name;
        final Map<Integer, Net> channels = new LinkedHashMap<>();

        Bundle(String name) {
            this.name = name;
        }
    }

    static final class Gate {

        final String name;
        final Position position;
        final Object part;
        final int kind;

        Gate(String name, Position position, Object part, int kind) {
            this.name = name;
            this.position = position;
            this.part = part;
            this.kind = kind;
        }

        Position port(int pin) {
            return IronBathRedstoneLab.port(part, position, pin);
        }
    }

    private final WorldServer world;
    private final List<Gate> gates = new ArrayList<>();
    private final Map<String, Position> observations = new LinkedHashMap<>();
    private final Map<String, Net> wires = new LinkedHashMap<>();
    private final Map<String, Bundle> bundles = new LinkedHashMap<>();
    private final Map<String, Position> positions = new LinkedHashMap<>();
    private final Map<String, Net> bare = new LinkedHashMap<>();
    private final List<Gate> sampleLatches = new ArrayList<>();

    /** Places local material memories and a timed sample, produce or correct cycle. */
    static IronBathNativeCircuit build(Hardware h) {
        IronBathNativeCircuit c = new IronBathNativeCircuit(h.world);
        int x = h.x, y = h.y, z = h.z;
        Bundle samples = c.bundle("sample identity");
        Bundle shortages = c.bundle("stock shortages");
        Bundle wanted = c.bundle("wanted sampled product");
        Net reset = c.net("reset sample memory", 15);
        Net[] categories = new Net[6];
        for (int i = 0; i < 6; i++) {
            int tx = h.sampleTanks.get(i)
                .getXCoord();
            boolean north = h.sampleTanks.get(i)
                .getZCoord() == z + 1;
            int gz = north ? z - 1 : z + 6;
            int outward = north ? -1 : 1;
            Net sense = c.net("local sample detector " + i, 8 + i);
            Net category = c.net("sample identity " + i, i + 1);
            categories[i] = category;
            Gate latch = c.gate(
                "material memory " + i,
                12,
                2,
                north ? ForgeDirection.WEST : ForgeDirection.EAST,
                new Position(tx, y + 2, gz),
                40);
            c.sampleLatches.add(latch);
            c.contact(latch, 3, sense);
            c.contact(latch, 1, reset);
            c.contact(latch, 0, category);
            Position sensor = latch.port(3);
            int tankZ = h.sampleTanks.get(i)
                .getZCoord();
            c.wire(
                sense,
                sensor,
                new Position(tx + outward, y + 2, sensor.z),
                new Position(tx + outward, y + 1, sensor.z),
                new Position(tx + outward, y + 1, tankZ),
                new Position(tx, y + 1, tankZ));
            c.cable(samples, new Position(tx + outward, y + 3, gz), new Position(x - 14, y + 3, gz));
        }
        c.cable(
            samples,
            new Position(x - 14, y + 3, z - 1),
            new Position(x - 14, y + 3, z + 8),
            new Position(x + 5, y + 3, z + 8),
            new Position(x + 5, y + 3, z + 10),
            new Position(x + 5, y + 2, z + 10),
            new Position(x + 6, y + 2, z + 10));
        c.cable(
            samples,
            new Position(x + 5, y + 3, z + 7),
            new Position(x + 25, y + 3, z + 7),
            new Position(x + 25, y + 3, z + 24),
            new Position(x + 12, y + 3, z + 24));
        c.wire(reset, new Position(x - 11, y + 2, z - 2), new Position(x - 5, y + 2, z - 2));
        c.wire(reset, new Position(x - 11, y + 2, z + 7), new Position(x - 5, y + 2, z + 7));
        c.wire(
            reset,
            new Position(x - 11, y + 2, z - 2),
            new Position(x - 16, y + 2, z - 2),
            new Position(x - 16, y + 2, z + 7),
            new Position(x - 16, y + 1, z + 7),
            new Position(x - 12, y + 1, z + 7),
            new Position(x - 12, y + 2, z + 7),
            new Position(x - 11, y + 2, z + 7));
        Gate actual = c.gate("sample rank", 29, 1, ForgeDirection.WEST, new Position(x + 7, y + 2, z + 10), 40);
        c.bundleContact(actual, 0, samples);
        Gate target = c
            .gate("highest missing stock", 29, 1, ForgeDirection.WEST, new Position(x + 7, y + 2, z + 14), 40);
        c.bundleContact(target, 0, shortages);
        Gate needed = c
            .gate("sampled product is wanted", 29, 1, ForgeDirection.WEST, new Position(x + 7, y + 2, z + 18), 40);
        c.bundleContact(needed, 0, wanted);
        for (int i = 0; i < 5; i++) {
            int sx = x + 12 + 3 * i;
            Net missing = c.net("stock shortage " + i, i + 1);
            Net match = c.net("wanted sample " + i, i + 1);
            Gate gate = c
                .gate("sample needed by stock " + i, 3, 4, ForgeDirection.SOUTH, new Position(sx, y + 2, z + 26), 40);
            c.contact(gate, 1, missing);
            c.contact(gate, 2, categories[i]);
            c.contact(gate, 0, match);
            c.wire(categories[i], gate.port(2), new Position(sx, y + 2, z + 24));
            c.wire(
                missing,
                new Position(sx - 1, y, z + 24),
                new Position(sx - 2, y, z + 24),
                new Position(sx - 2, y, z + 26),
                new Position(sx - 2, y + 2, z + 26),
                gate.port(1));
            c.wire(missing, new Position(sx - 2, y, z + 26), new Position(sx - 1, y, z + 26));
            c.cable(shortages, new Position(sx - 1, y - 1, z + 26), new Position(x + 5, y - 1, z + 26));
            c.cable(wanted, new Position(sx, y + 3, z + 27), new Position(x + 6, y + 3, z + 27));
        }
        c.cable(
            shortages,
            new Position(x + 5, y - 1, z + 26),
            new Position(x + 5, y - 1, z + 23),
            new Position(x + 5, y - 2, z + 23),
            new Position(x + 5, y - 2, z + 19),
            new Position(x + 5, y - 1, z + 19),
            new Position(x + 5, y - 1, z + 14),
            new Position(x + 5, y + 2, z + 14),
            target.port(0));
        c.cable(wanted, new Position(x + 6, y + 3, z + 27), new Position(x + 6, y + 3, z + 18), needed.port(0));
        Net actualRank = c.net("actual carbon rank", 6);
        Net targetRank = c.net("missing carbon rank", 7);
        Net highSignal = c.net("carbon too high", 11);
        Net lowSignal = c.net("carbon too low", 12);
        c.contact(actual, 2, actualRank);
        c.contact(target, 2, targetRank);
        Gate high = c.gate("carbon too high", 26, 1, ForgeDirection.EAST, new Position(x + 10, y + 2, z + 10), 40);
        Gate low = c.gate("carbon too low", 26, 1, ForgeDirection.EAST, new Position(x + 10, y + 2, z + 14), 40);
        c.contact(high, 2, actualRank);
        c.contact(high, 1, targetRank);
        c.contact(high, 0, highSignal);
        c.contact(low, 2, targetRank);
        c.contact(low, 1, actualRank);
        c.contact(low, 0, lowSignal);
        c.wire(actualRank, actual.port(2), high.port(2));
        c.wire(
            actualRank,
            actual.port(2),
            new Position(x + 8, y + 3, z + 10),
            new Position(x + 8, y + 3, z + 15),
            new Position(x + 10, y + 3, z + 15),
            low.port(1));
        c.wire(targetRank, target.port(2), low.port(2));
        c.wire(
            targetRank,
            target.port(2),
            new Position(x + 8, y + 2, z + 12),
            new Position(x + 10, y + 2, z + 12),
            high.port(1));

        Net stop = c.net("STOP", 0);
        Net run = c.net("RUN", 1);
        Net delayed = c.net("probe after reset", 2);
        Net probe = c.net("sample pulse", 3);
        Net anyMissing = c.net("any missing stock", 4);
        Net received = c.net("material identified", 8);
        Net stable = c.net("stable material", 9);
        Net pulse = c.net("decision pulse", 10);
        Net decision = c.net("enabled decision", 5);
        Net isWanted = c.net("current material wanted", 7);
        Net notWanted = c.net("current material not wanted", 13);
        Net production = c.net("bulk output", 14);
        Net fresh = c.net("single fresh batch", 9);
        Net fuel = c.net("one Coke", 2);
        Net oxygenTrigger = c.net("oxygen dose trigger", 3);
        Net oxygenActive = c.net("oxygen dose", 4);
        Net oxygenRun = c.net("oxygen enabled", 5);
        Gate enabled = c.gate("RUN", 2, 5, ForgeDirection.EAST, new Position(x - 10, y + 2, z + 20), 40);
        c.contact(enabled, 2, stop);
        c.contact(enabled, 0, run);
        c.wire(stop, new Position(x - 11, y + 1, z + 18), new Position(x - 11, y + 2, z + 18), enabled.port(2));
        Gate clock = c.gate("sample cadence", 17, 0, ForgeDirection.SOUTH, new Position(x - 6, y + 2, z + 20), 800);
        c.contact(clock, 0, reset);
        c.contact(clock, 2, stop);
        c.wire(stop, new Position(x - 11, y + 2, z + 18), new Position(x - 6, y + 2, z + 18), clock.port(2));
        c.wire(reset, clock.port(0), new Position(x - 16, y + 2, z + 21), new Position(x - 16, y + 2, z + 7));
        Gate delay = c.gate("reset before sample", 10, 5, ForgeDirection.SOUTH, new Position(x - 6, y + 2, z + 24), 40);
        c.contact(delay, 2, reset);
        c.contact(delay, 0, delayed);
        c.wire(reset, clock.port(0), delay.port(2));
        Gate sample = c.gate("sample when needed", 3, 0, ForgeDirection.EAST, new Position(x - 3, y + 2, z + 25), 40);
        c.contact(sample, 2, delayed);
        c.contact(sample, 1, run);
        c.contact(sample, 3, anyMissing);
        c.contact(sample, 0, probe);
        c.wire(delayed, delay.port(0), sample.port(2));
        c.contact(target, 1, anyMissing);
        c.wire(
            anyMissing,
            target.port(1),
            new Position(x + 7, y + 4, z + 13),
            new Position(x - 3, y + 4, z + 13),
            new Position(x - 3, y + 4, z + 24),
            sample.port(3));
        c.wire(
            run,
            enabled.port(0),
            new Position(x - 8, y + 2, z + 20),
            new Position(x - 8, y + 3, z + 20),
            new Position(x - 8, y + 3, z + 26),
            new Position(x - 3, y + 3, z + 26),
            sample.port(1));

        c.contact(actual, 1, received);
        Gate settle = c
            .gate("allow comparison to settle", 10, 4, ForgeDirection.SOUTH, new Position(x + 3, y + 2, z + 11), 40);
        c.contact(settle, 2, received);
        c.contact(settle, 0, stable);
        c.wire(
            received,
            actual.port(1),
            new Position(x + 8, y + 2, z + 9),
            new Position(x + 8, y + 1, z + 9),
            new Position(x + 8, y + 1, z + 8),
            new Position(x + 3, y + 1, z + 8),
            new Position(x + 3, y + 1, z + 9),
            new Position(x + 3, y + 2, z + 9),
            settle.port(2));
        Gate edge = c
            .gate("one decision per sample", 9, 0, ForgeDirection.SOUTH, new Position(x + 3, y + 2, z + 14), 40);
        c.contact(edge, 2, stable);
        c.contact(edge, 0, pulse);
        c.wire(stable, settle.port(0), edge.port(2));
        Gate decide = c.gate("decision while running", 3, 1, ForgeDirection.WEST, new Position(x, y + 2, z + 15), 40);
        c.contact(decide, 2, pulse);
        c.contact(decide, 3, run);
        c.contact(decide, 0, decision);
        c.wire(pulse, edge.port(0), decide.port(2));
        c.wire(
            run,
            new Position(x - 8, y + 3, z + 20),
            new Position(x, y + 3, z + 20),
            new Position(x, y + 3, z + 16),
            decide.port(3));
        c.contact(needed, 3, isWanted);
        Gate unwanted = c
            .gate("correct only unwanted material", 2, 5, ForgeDirection.WEST, new Position(x + 3, y + 2, z + 18), 40);
        c.contact(unwanted, 2, isWanted);
        c.contact(unwanted, 0, notWanted);
        c.wire(
            isWanted,
            needed.port(3),
            new Position(x + 8, y + 2, z + 19),
            new Position(x + 8, y + 5, z + 19),
            new Position(x + 4, y + 5, z + 19),
            new Position(x + 4, y + 2, z + 19),
            unwanted.port(2));
        Gate output = c
            .gate("produce wanted material", 3, 2, ForgeDirection.NORTH, new Position(x - 1, y + 2, z + 19), 40);
        c.contact(output, 1, isWanted);
        c.contact(output, 3, run);
        c.contact(output, 0, production);
        c.wire(isWanted, unwanted.port(2), new Position(x + 4, y + 2, z + 19), output.port(1));
        c.wire(
            run,
            new Position(x - 8, y + 3, z + 20),
            new Position(x - 2, y + 3, z + 20),
            new Position(x - 2, y + 3, z + 19),
            output.port(3));
        Gate refill = c.gate("one tick fresh Iron", 9, 1, ForgeDirection.NORTH, new Position(x - 1, y + 2, z + 23), 40);
        c.contact(refill, 2, production);
        c.contact(refill, 0, fresh);
        c.wire(
            production,
            output.port(0),
            new Position(x - 1, y + 4, z + 18),
            new Position(x + 1, y + 4, z + 18),
            new Position(x + 1, y + 4, z + 24),
            new Position(x - 1, y + 4, z + 24),
            refill.port(2));
        c.wire(
            fresh,
            refill.port(0),
            new Position(x - 2, y + 2, z + 22),
            new Position(x - 2, y + 1, z + 22),
            new Position(x - 12, y + 1, z + 22),
            new Position(x - 12, y + 1, z + 10),
            new Position(x - 12, y, z + 10),
            new Position(x - 11, y, z + 10));

        Gate coke = c.gate("add carbon", 3, 0, ForgeDirection.SOUTH, new Position(x + 13, y + 2, z + 10), 40);
        Gate oxygen = c.gate("remove carbon", 3, 0, ForgeDirection.SOUTH, new Position(x + 13, y + 2, z + 14), 40);
        c.contact(coke, 2, lowSignal);
        c.contact(coke, 1, notWanted);
        c.contact(coke, 3, decision);
        c.contact(coke, 0, fuel);
        c.contact(oxygen, 2, highSignal);
        c.contact(oxygen, 1, notWanted);
        c.contact(oxygen, 3, decision);
        c.contact(oxygen, 0, oxygenTrigger);
        c.wire(
            lowSignal,
            low.port(0),
            new Position(x + 11, y + 4, z + 14),
            new Position(x + 11, y + 4, z + 9),
            new Position(x + 13, y + 4, z + 9),
            coke.port(2));
        c.wire(
            highSignal,
            high.port(0),
            new Position(x + 11, y + 2, z + 12),
            new Position(x + 13, y + 2, z + 12),
            oxygen.port(2));
        c.wire(
            notWanted,
            unwanted.port(0),
            new Position(x + 2, y + 4, z + 18),
            new Position(x + 2, y + 4, z + 16),
            new Position(x + 12, y + 4, z + 16),
            new Position(x + 12, y + 4, z + 10),
            coke.port(1));
        c.wire(notWanted, new Position(x + 12, y + 4, z + 14), oxygen.port(1));
        c.wire(
            decision,
            decide.port(0),
            new Position(x - 1, y + 5, z + 15),
            new Position(x + 14, y + 5, z + 15),
            new Position(x + 14, y + 5, z + 10),
            coke.port(3));
        c.wire(decision, new Position(x + 14, y + 5, z + 14), oxygen.port(3));
        Gate dose = c.gate("short oxygen dose", 20, 0, ForgeDirection.SOUTH, new Position(x + 13, y + 2, z + 18), 32);
        c.contact(dose, 2, oxygenTrigger);
        c.contact(dose, 3, oxygenActive);
        c.wire(oxygenTrigger, oxygen.port(0), dose.port(2));
        Gate oxygenEnable = c
            .gate("oxygen RUN interlock", 3, 4, ForgeDirection.NORTH, new Position(x + 16, y + 2, z + 18), 40);
        c.contact(oxygenEnable, 2, oxygenActive);
        c.contact(oxygenEnable, 1, run);
        c.contact(oxygenEnable, 0, oxygenRun);
        c.wire(
            oxygenActive,
            dose.port(3),
            new Position(x + 14, y + 2, z + 19),
            new Position(x + 16, y + 2, z + 19),
            oxygenEnable.port(2));
        c.wire(
            run,
            new Position(x, y + 3, z + 20),
            new Position(x, y + 1, z + 20),
            new Position(x + 17, y + 1, z + 20),
            new Position(x + 17, y + 1, z + 19),
            new Position(x + 17, y + 2, z + 19),
            oxygenEnable.port(1));

        Position sampleRelay = new Position(h.lowTap.getXCoord() + 1, h.lowTap.getYCoord(), h.lowTap.getZCoord());
        c.relay("sample casing relay", sampleRelay, ForgeDirection.WEST, probe);
        c.wire(
            probe,
            sample.port(0),
            new Position(x - 1, y + 2, z + 25),
            new Position(x - 1, y, z + 25),
            new Position(x - 4, y, z + 25),
            new Position(x - 4, y, z + 10),
            new Position(x - 4, y + 5, z + 10),
            new Position(x - 4, y + 8, z + 10),
            new Position(x - 4, y + 8, z - 13),
            new Position(sampleRelay.x, y + 8, z - 13),
            new Position(sampleRelay.x, sampleRelay.y, z - 13),
            sampleRelay);
        Position bulkRelay = new Position(h.upperTap.getXCoord() - 1, h.upperTap.getYCoord(), h.upperTap.getZCoord());
        c.relay("bulk casing relay", bulkRelay, ForgeDirection.EAST, production);
        c.wire(
            production,
            output.port(0),
            new Position(x - 2, y + 2, z + 18),
            new Position(x - 2, y + 1, z + 18),
            new Position(x - 2, y + 1, z + 17),
            new Position(x + 9, y + 1, z + 17),
            new Position(x + 9, y, z + 17),
            new Position(x + 15, y, z + 17),
            new Position(x + 15, y + 8, z + 17),
            new Position(x + 15, y + 8, z - 14),
            new Position(bulkRelay.x, y + 8, z - 14),
            new Position(bulkRelay.x, y + 9, z - 14),
            new Position(bulkRelay.x, y + 9, bulkRelay.z - 1),
            new Position(bulkRelay.x, bulkRelay.y, bulkRelay.z - 1),
            bulkRelay);
        c.relay("Coke dropper relay", new Position(x - 11, y, z + 14), ForgeDirection.EAST, fuel);
        c.wire(
            fuel,
            coke.port(0),
            new Position(x + 13, y + 5, z + 11),
            new Position(x - 11, y + 5, z + 11),
            new Position(x - 11, y + 5, z + 14),
            new Position(x - 11, y, z + 14));
        c.wire(
            oxygenRun,
            oxygenEnable.port(0),
            new Position(x + 16, y + 2, z + 16),
            new Position(x + 16, y - 2, z + 16),
            new Position(x + 16, y - 2, z + 17),
            new Position(x + 16, y - 3, z + 17),
            new Position(x - 3, y - 3, z + 17),
            new Position(x - 3, y - 3, h.oxygenSupply.getZCoord()),
            new Position(h.oxygenSupply.getXCoord() - 1, y - 3, h.oxygenSupply.getZCoord()),
            new Position(h.oxygenSupply.getXCoord() - 1, h.oxygenSupply.getYCoord() - 1, h.oxygenSupply.getZCoord()),
            new Position(h.oxygenSupply.getXCoord(), h.oxygenSupply.getYCoord() - 1, h.oxygenSupply.getZCoord()));
        c.finish();
        return c;
    }

    int sampledRank() {
        int rank = 0;
        for (int i = 0; i < sampleLatches.size(); i++) {
            int state = ((Number) IronBathRedstoneLab.invoke(sampleLatches.get(i).part, "state", new Class<?>[0]))
                .intValue();
            if ((state & 0x10) != 0) rank = i + 1;
        }
        return rank;
    }

    IronBathNativeCircuit(WorldServer world) {
        this.world = world;
    }

    Net net(String name, int colour) {
        if (colour < 0 || colour > 15) throw new IllegalArgumentException("Invalid native wire colour");
        return new Net(name, colour);
    }

    Bundle bundle(String name) {
        return new Bundle(name);
    }

    Gate gate(String name, int kind, int shape, ForgeDirection output, Position position, int interval) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("subID", (byte) kind);
        tag.setBoolean("nolegacy", true);
        tag.setLong("schedTime", -1);
        tag.setInteger("pmax", Math.max(2, interval - 2));
        tag.setLong("pelapsed", -1);
        support(position);
        String type = kind == 22 || kind >= 28 && kind <= 31 ? "pr_bgate"
            : kind == 12 || kind == 13 || kind >= 17 && kind <= 21 || kind == 26 ? "pr_igate" : "pr_sgate";
        Object part = IronBathRedstoneLab.part(world, position, type, tag);
        for (int rotation = 0; rotation < 4; rotation++) {
            IronBathRedstoneLab.invoke(part, "setRotation", new Class<?>[] { int.class }, rotation);
            Position port = IronBathRedstoneLab.port(part, position, 0);
            if (same(port, position.offset(output))) break;
        }
        IronBathRedstoneLab.invoke(part, "setShape", new Class<?>[] { int.class }, shape);
        Gate gate = new Gate(name, position, part, kind);
        if (!same(gate.port(0), position.offset(output)))
            throw new IllegalStateException("Native gate orientation mismatch: " + name);
        gates.add(gate);
        observations.put(name, position);
        return gate;
    }

    void contact(Gate gate, int pin, Net net) {
        Position position = gate.port(pin);
        support(position);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("nolegacy", true);
        tag.setByte("side", (byte) ForgeDirection.DOWN.ordinal());
        tag.setByte("colour", (byte) net.colour);
        IronBathRedstoneLab.part(world, position, "pr_insulated", tag);
        wire(net, position);
    }

    void bundleContact(Gate gate, int pin, Bundle bundle) {
        Position position = gate.port(pin);
        support(position);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("nolegacy", true);
        tag.setByte("side", (byte) ForgeDirection.DOWN.ordinal());
        tag.setByte("colour", (byte) -1);
        tag.setByteArray("signal", new byte[16]);
        IronBathRedstoneLab.part(world, position, "pr_bundled", tag);
        cable(bundle, position);
    }

    void relay(String name, Position position, ForgeDirection attachment, Net net) {
        world.setBlockToAir(position.x, position.y, position.z);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("nolegacy", true);
        tag.setByte("side", (byte) attachment.ordinal());
        Object part = IronBathRedstoneLab.part(world, position, "pr_redwire", tag);
        if (((Number) IronBathRedstoneLab.invoke(part, "side", new Class<?>[0])).intValue() != attachment.ordinal())
            throw new IllegalStateException("Native relay attachment mismatch");
        observations.put(name, position);
        bare.put(position.toString(), net);
        wire(net, position);
    }

    void wire(Net net, Position... path) {
        for (Position position : cells(path)) {
            String key = position.toString();
            if (bundles.containsKey(key)) throw new IllegalStateException("Wire overlaps bundled cable at " + position);
            Net previous = wires.get(key);
            if (previous != null) {
                if (previous != net) throw new IllegalStateException("Different native signals overlap at " + position);
                continue;
            }
            wires.put(key, net);
            positions.put(key, position);
            if (world.getBlock(position.x, position.y, position.z) == Blocks.stonebrick
                || world.getBlock(position.x, position.y, position.z) == Blocks.stained_hardened_clay)
                world.setBlockToAir(position.x, position.y, position.z);
            IronBathRedstoneLab.wire(world, position, net.colour);
        }
    }

    void cable(Bundle bundle, Position... path) {
        for (Position position : cells(path)) {
            String key = position.toString();
            if (wires.containsKey(key))
                throw new IllegalStateException("Bundled cable overlaps a signal at " + position);
            Bundle previous = bundles.get(key);
            if (previous != null) {
                if (previous != bundle)
                    throw new IllegalStateException("Different native bundles overlap at " + position);
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("nolegacy", true);
            tag.setString("mat", "");
            tag.setByte("colour", (byte) -1);
            tag.setByteArray("signal", new byte[16]);
            if (world.getBlock(position.x, position.y, position.z) == Blocks.stonebrick
                || world.getBlock(position.x, position.y, position.z) == Blocks.stained_hardened_clay)
                world.setBlockToAir(position.x, position.y, position.z);
            IronBathRedstoneLab.part(world, position, "pr_fbundled", tag);
            bundles.put(key, bundle);
            positions.put(key, position);
        }
    }

    private static List<Position> cells(Position[] path) {
        if (path.length == 0) throw new IllegalArgumentException("An explicit path needs a position");
        List<Position> cells = new ArrayList<>();
        cells.add(path[0]);
        for (int index = 1; index < path.length; index++) {
            Position from = path[index - 1], to = path[index];
            int dx = Integer.signum(to.x - from.x), dy = Integer.signum(to.y - from.y),
                dz = Integer.signum(to.z - from.z);
            if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1)
                throw new IllegalArgumentException("A hand-wired path must specify each bend: " + from + " to " + to);
            Position current = from;
            while (!same(current, to)) {
                current = new Position(current.x + dx, current.y + dy, current.z + dz);
                cells.add(current);
            }
        }
        return cells;
    }

    private void support(Position position) {
        if (world.isAirBlock(position.x, position.y - 1, position.z))
            world.setBlock(position.x, position.y - 1, position.z, Blocks.stonebrick, 0, 3);
    }

    void finish() {
        for (Map.Entry<String, Net> entry : wires.entrySet()) {
            Position position = positions.get(entry.getKey());
            Net net = entry.getValue();
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                String neighbor = position.offset(side)
                    .toString();
                Net other = wires.get(neighbor);
                if (other != null && other != net
                    && (other.colour == net.colour || bare.containsKey(entry.getKey()) || bare.containsKey(neighbor)))
                    throw new IllegalStateException("Native signal short at " + position + " toward " + side);
                Bundle bundle = bundles.get(neighbor);
                if (bundle != null) {
                    Net previous = bundle.channels.putIfAbsent(net.colour, net);
                    if (previous != null && previous != net) throw new IllegalStateException(
                        "Two signals share bundled channel " + net.colour + " in " + bundle.name);
                }
            }
        }
        for (Map.Entry<String, Bundle> entry : bundles.entrySet()) {
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
                Bundle neighbor = bundles.get(
                    positions.get(entry.getKey())
                        .offset(side)
                        .toString());
                if (neighbor != null && neighbor != entry.getValue())
                    throw new IllegalStateException("Different bundled trunks touch at " + entry.getKey());
            }
        }
        for (Gate gate : gates) {
            if (gate.kind != 12) continue;
            Object logic = IronBathRedstoneLab.invoke(gate.part, "getLogicSequential", new Class<?>[0]);
            IronBathRedstoneLab.invoke(logic, "setState2", new Class<?>[] { int.class }, 2);
            IronBathRedstoneLab.invoke(gate.part, "setState", new Class<?>[] { int.class }, 0x40);
            IronBathRedstoneLab.invoke(gate.part, "onOutputChange", new Class<?>[] { int.class }, 5);
        }
    }

    Map<String, Position> observations() {
        return observations;
    }

    int gateCount() {
        return gates.size();
    }

    int conductorCount() {
        return wires.size() + bundles.size();
    }

    private static boolean same(Position a, Position b) {
        return a.x == b.x && a.y == b.y && a.z == b.z;
    }
}
