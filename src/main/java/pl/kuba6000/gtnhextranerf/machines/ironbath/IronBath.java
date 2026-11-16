package pl.kuba6000.gtnhextranerf.machines.ironbath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.github.bsideup.jabel.Desugar;

/** Owns the constituents, phase changes and heat balance of one furnace. */
public final class IronBath {

    public static final double AMBIENT_TEMPERATURE = 300;
    public static final double MELTING_TEMPERATURE = 1700;
    public static final double WORKING_TEMPERATURE = 1800;
    public static final double MAX_TEMPERATURE = 2200;
    public static final double COLD_MELTING_HEAT_PER_INGOT = 20.0 * (MELTING_TEMPERATURE - AMBIENT_TEMPERATURE) + 36000;
    public static final double COLD_MELTING_EU_PER_INGOT = 24000;
    public static final int BASE_CAPACITY_MB = 512 * 144;
    private static final int MAX_COIL_TIER = 13;
    private static final int TAP_BAND_COUNT = 5;
    private static final int CALCITE_UNITS_PER_DUST = 20;
    private static final double MAX_CARBON_PER_INGOT = 20;
    private static final double SENSIBLE_PER_MB = 20.0 / 144;
    private static final double LATENT_PER_MB = 36000.0 / 144;
    private static final IronProduct[] PRODUCTS = IronProduct.values();

    /** Stored fill fraction and the material and phase exposed at the surface. */
    @Desugar
    public record Surface(float fill, IronProduct product, boolean molten) {

        public static final Surface EMPTY = new Surface(0, IronProduct.IRON, false);
    }

    public enum Fuel {

        COAL(100, 5),
        COKE(100, 1),
        CHARCOAL(80, 10),
        CACTUS_COKE(25, 1),
        SUGAR_COKE(25, 1);

        private final double carbon;
        private final int impurities;

        Fuel(double carbon, int impurities) {
            this.carbon = carbon;
            this.impurities = impurities;
        }

        public double carbon() {
            return carbon;
        }

        public int impurities() {
            return impurities;
        }
    }

    public static final class Tap {

        public static final Tap EMPTY = new Tap(null, 0);
        private final IronProduct product;
        private final int amount;

        private Tap(IronProduct product, int amount) {
            this.product = product;
            this.amount = amount;
        }

        public IronProduct product() {
            return product;
        }

        public int amountMb() {
            return amount;
        }
    }

    private int capacity;
    private int coilTier;
    private int metal;
    private int slag;
    private double carbon;
    private int impurities;
    private int calcite;
    private double heat;
    private double insulation;
    private final List<Charge> charges = new ArrayList<>();

    private static final class Charge {

        private final IronProduct product;
        private final ItemStack stack;
        private int count;
        private double heat;

        private Charge(IronProduct product, ItemStack source, int count) {
            this.product = product;
            stack = source == null ? null : source.copy();
            if (stack != null) stack.stackSize = 1;
            this.count = count;
        }
    }

    @Desugar
    public record ChargeGroup(ItemStack stack, int count, double progress) {}

    public IronBath(int coilTier) {
        configure(coilTier);
    }

    public boolean configure(int tier) {
        requireTier(tier);
        int nextCapacity = BASE_CAPACITY_MB * (1 << tier);
        if (occupiedMb() > nextCapacity || carbon + queuedCarbon() > nextCapacity / 144.0 * MAX_CARBON_PER_INGOT)
            return false;
        capacity = nextCapacity;
        coilTier = tier;
        return true;
    }

    public void setInsulation(double upgradedFraction) {
        if (!Double.isFinite(upgradedFraction)) throw new IllegalArgumentException("Invalid insulation fraction");
        insulation = Math.max(0, Math.min(1, upgradedFraction));
    }

    public double heatingEfficiency() {
        return 0.9 + 0.05 * insulation;
    }

    public boolean addMetal(IronProduct product, int amountMb) {
        return addMetal(product, amountMb, null);
    }

    public boolean addMetal(IronProduct product, int amountMb, ItemStack source) {
        Objects.requireNonNull(product, "product");
        if (amountMb <= 0 || amountMb % 144 != 0 || amountMb / 144 > metalRoom(product)) return false;
        insertCharge(new Charge(product, source, amountMb / 144));
        return true;
    }

    /** Admits the part of an input stack that fits the bath's volume and composition limits. */
    public int acceptMetal(IronProduct product, int offered, ItemStack source) {
        int accepted = Math.min(Math.max(0, offered), metalRoom(product));
        if (accepted > 0) insertCharge(new Charge(product, source, accepted));
        return accepted;
    }

    private int metalRoom(IronProduct product) {
        Objects.requireNonNull(product, "product");
        if (product == IronProduct.SLAG) return 0;
        int room = Math.max(0, Math.min(metalCapacityMb() - metal - pendingMetalMb(), capacity - occupiedMb()) / 144);
        double carbonPerIngot = product.carbonPerMb() * 144;
        if (carbonPerIngot > 0) {
            double carbonRoom = capacity / 144.0 * MAX_CARBON_PER_INGOT - carbon - queuedCarbon();
            room = Math.min(room, (int) Math.max(0, Math.floor(carbonRoom / carbonPerIngot)));
        }
        return room;
    }

    public boolean addFuel(Fuel fuel, int count) {
        if (count <= 0 || count > fuelRoom(fuel)) return false;
        return acceptFuel(fuel, count) == count;
    }

    public int acceptFuel(Fuel fuel, int offered) {
        int accepted = Math.min(Math.max(0, offered), fuelRoom(fuel));
        carbon += fuel.carbon * accepted;
        impurities += fuel.impurities * accepted;
        return accepted;
    }

    private int fuelRoom(Fuel fuel) {
        Objects.requireNonNull(fuel, "fuel");
        if (metal + pendingMetalMb() > metalCapacityMb()) return 0;
        long impurityRoom = (Integer.MAX_VALUE - (long) impurities) / fuel.impurities;
        double carbonRoom = capacity / 144.0 * MAX_CARBON_PER_INGOT - carbon - queuedCarbon();
        return (int) Math.max(0, Math.min(impurityRoom, Math.floor(carbonRoom / fuel.carbon)));
    }

    private void insertCharge(Charge charge) {
        for (Charge group : charges) {
            if (group.product == charge.product && group.heat == charge.heat
                && ItemStack.areItemStacksEqual(group.stack, charge.stack)) {
                group.count += charge.count;
                return;
            }
        }
        charges.add(charge);
    }

    public int chargeGroupCount() {
        return charges.size();
    }

    /** Returns a detached page of melting groups for the controller display. */
    public List<ChargeGroup> chargeGroups(int offset, int limit) {
        if (offset < 0 || limit < 0) throw new IllegalArgumentException("Invalid charge group page");
        int end = (int) Math.min(charges.size(), (long) offset + limit);
        List<ChargeGroup> page = new ArrayList<>(Math.max(0, end - offset));
        for (int index = offset; index < end; index++) {
            Charge charge = charges.get(index);
            page.add(
                new ChargeGroup(
                    charge.stack == null ? null : charge.stack.copy(),
                    charge.count,
                    charge.heat / COLD_MELTING_HEAT_PER_INGOT));
        }
        return page;
    }

    public int meltingItems() {
        int occupied = 0;
        for (Charge charge : charges) occupied += charge.count;
        return occupied;
    }

    public int calciteMaximum() {
        return 180 + 160 * coilTier;
    }

    public int acceptCalcite(int offeredDust) {
        int accepted = Math.max(0, Math.min(offeredDust, (calciteMaximum() - calcite) / CALCITE_UNITS_PER_DUST));
        calcite += accepted * CALCITE_UNITS_PER_DUST;
        return accepted;
    }

    /** Consumes the supplied flow. Only oxygen contacting liquid carbon reacts. */
    public int addOxygen(int amountMb) {
        if (amountMb <= 0) return 0;
        if (liquidMetalMb() > 0) {
            double removed = Math.min(carbon * liquidFraction(), amountMb / 32.0);
            carbon = Math.max(0, carbon - removed);
        }
        return amountMb;
    }

    /** Advances one server tick. The caller supplies paid useful heat and coil transfer capacity. */
    public void tick(double usefulHeat, double transferLimit) {
        cool(0);
        addHeat(usefulHeat, transferLimit);
        finishTick(usefulHeat > 0, true);
    }

    /** Heats the current charges and bath. */
    public void addHeat(double usefulHeat, double transferLimit) {
        requireBudget(usefulHeat);
        requireBudget(transferLimit);
        double transferred = heatCharges(Math.min(usefulHeat, transferLimit));
        heat += Math.min(Math.max(0, usefulHeat - transferred), bathHeatDemand());
    }

    private double heatCharges(double usefulHeat) {
        int active = meltingItems();
        double share = active == 0 ? 0 : usefulHeat / active;
        double transferredHeat = 0;
        for (java.util.Iterator<Charge> iterator = charges.iterator(); iterator.hasNext();) {
            Charge charge = iterator.next();
            double before = charge.heat;
            charge.heat = Math.min(COLD_MELTING_HEAT_PER_INGOT, before + share);
            transferredHeat += (charge.heat - before) * charge.count;
            if (charge.heat >= COLD_MELTING_HEAT_PER_INGOT - 1e-9) {
                meltCharge(charge);
                iterator.remove();
            }
        }
        return transferredHeat;
    }

    public double maximumTemperature() {
        return MAX_TEMPERATURE + 900 * coilTier;
    }

    public double chargeHeatDemand() {
        double demand = 0;
        for (Charge charge : charges) demand += Math.max(0, COLD_MELTING_HEAT_PER_INGOT - charge.heat) * charge.count;
        return demand;
    }

    public double bathHeatDemand() {
        return Math.max(0, residentHeatAt(maximumTemperature()) - heat);
    }

    /** Returns the EU needed to finish the current charges and heat their bath to its coil ceiling. */
    public double powerDemand(double chargeHeatPerEu, double bathHeatPerEu) {
        requireConversion(chargeHeatPerEu);
        requireConversion(bathHeatPerEu);
        double chargeDemand = chargeHeatDemand();
        double bathDemand = Math.max(0, heatDemand(maximumTemperature()) - chargeDemand);
        return chargeDemand / chargeHeatPerEu + bathDemand / bathHeatPerEu;
    }

    /** Spends one paid EU allowance on charges, then directly heats the bath with the remainder. */
    public void addPower(double paidEu, double chargeHeatPerEu, double bathHeatPerEu) {
        requireBudget(paidEu);
        requireConversion(chargeHeatPerEu);
        requireConversion(bathHeatPerEu);
        double remaining = paidEu;
        while (!charges.isEmpty() && remaining > 0) {
            double requested = Math.min(chargeHeatDemand(), remaining * chargeHeatPerEu);
            if (requested <= 0) break;
            double transferred = heatCharges(requested);
            if (transferred <= 0) break;
            remaining = Math.max(0, remaining - transferred / chargeHeatPerEu);
        }
        addHeat(remaining * bathHeatPerEu, 0);
    }

    private static void requireConversion(double heatPerEu) {
        if (!Double.isFinite(heatPerEu) || heatPerEu <= 0)
            throw new IllegalArgumentException("Invalid heat conversion");
    }

    /** Advances charge cooling and chemistry after this tick's heating. */
    public void finishTick(boolean powered, boolean operating) {
        if (!powered) coolCharges();
        if (operating && temperature() >= MELTING_TEMPERATURE && calcite > 0 && impurities > 0) {
            int bound = Math.min(Math.min(calcite, impurities), (capacity - occupiedMb()) / 32);
            impurities -= bound;
            calcite -= bound;
            slag += bound * 32;
        }
        double temperature = temperature();
        double factor = temperature < MELTING_TEMPERATURE ? 0
            : temperature <= WORKING_TEMPERATURE ? 0.5 + (temperature - MELTING_TEMPERATURE) / 200
                : temperature <= MAX_TEMPERATURE ? 1 + (temperature - WORKING_TEMPERATURE) / 400
                    : 2 * Math.sqrt(temperature / MAX_TEMPERATURE);
        double burned = Math.min(carbon * liquidFraction(), liquidMetalMb() / 144.0 * 0.002 / 20 * factor);
        carbon = Math.max(0, carbon - burned);
    }

    /** Applies unpaid losses before heating, using the same temperature as the replacement payment. */
    public void cool(double replacedHeatLoss) {
        requireBudget(replacedHeatLoss);
        heat = Math.max(0, heat - 2 * Math.max(0, heatLoss() - replacedHeatLoss));
    }

    public double heatLoss() {
        return 30.0 * (1 << coilTier)
            * COLD_MELTING_HEAT_PER_INGOT
            / COLD_MELTING_EU_PER_INGOT
            * (1 - 0.5 * insulation)
            * (temperature() - AMBIENT_TEMPERATURE)
            / (maximumTemperature() - AMBIENT_TEMPERATURE);
    }

    private void coolCharges() {
        double chargeCapacity = COLD_MELTING_HEAT_PER_INGOT / (MELTING_TEMPERATURE - AMBIENT_TEMPERATURE);
        for (Charge charge : charges) {
            double chargeTemperature = AMBIENT_TEMPERATURE + charge.heat / chargeCapacity;
            double cooling = Math.max(
                0,
                0.01 * (chargeTemperature - temperature())
                    / (1 / sensibleCapacity() + 1 / (chargeCapacity * charge.count)));
            cooling = Math.min(charge.heat * charge.count, cooling);
            charge.heat -= cooling / charge.count;
            heat += cooling;
        }
    }

    private void meltCharge(Charge charge) {
        metal += 144 * charge.count;
        carbon += 144 * charge.product.carbonPerMb() * charge.count;
        // Excess charge heat dissipates at the coil ceiling
        heat = Math.min(residentHeatAt(maximumTemperature()), heat + charge.heat * charge.count);
    }

    public double heatDemand(double targetTemperature) {
        if (!Double.isFinite(targetTemperature)) throw new IllegalArgumentException("Invalid target temperature");
        double target = Math.max(AMBIENT_TEMPERATURE, Math.min(maximumTemperature(), targetTemperature));
        double pendingEnergy = 0;
        double chargeDemand = chargeHeatDemand();
        for (Charge charge : charges) {
            pendingEnergy += charge.heat * charge.count;
        }
        double perMb = SENSIBLE_PER_MB * (target - AMBIENT_TEMPERATURE)
            + (target >= MELTING_TEMPERATURE ? LATENT_PER_MB : 0);
        return Math.max(chargeDemand, residentHeatAt(target) + pendingMetalMb() * perMb - heat - pendingEnergy);
    }

    public Tap previewTap(int band, int maxMb) {
        if (band < 0 || band >= TAP_BAND_COUNT || maxMb <= 0) return Tap.EMPTY;
        int lower = (int) ((long) capacity * band / TAP_BAND_COUNT);
        int upper = (int) ((long) capacity * (band + 1) / TAP_BAND_COUNT);
        int liquidMetal = liquidMetalMb();
        int liquidTop = liquidMetal + liquidSlagMb();
        int slagBottom = Math.max(lower, liquidMetal);
        if (Math.min(upper, liquidTop) > slagBottom)
            return new Tap(IronProduct.SLAG, Math.min(maxMb, liquidTop - slagBottom));
        if (Math.min(upper, liquidMetal) <= lower || impurities != 0) return Tap.EMPTY;
        IronProduct product = currentProduct();
        int amount = Math.min(maxMb, liquidMetal - lower);
        double reference = product.carbonPerMb();
        if (reference > 0)
            amount = Math.min(amount, (int) Math.min(Integer.MAX_VALUE, Math.floor(carbon / reference + 1e-9)));
        return amount > 0 ? new Tap(product, amount) : Tap.EMPTY;
    }

    public Tap tap(int band, int maxMb) {
        Tap result = previewTap(band, maxMb);
        if (result.amount == 0) return result;
        double temperature = temperature();
        double removedHeat = result.amount * (SENSIBLE_PER_MB * (temperature - AMBIENT_TEMPERATURE) + LATENT_PER_MB);
        if (result.product == IronProduct.SLAG) slag -= result.amount;
        else {
            metal -= result.amount;
            carbon = Math.max(0, carbon - result.amount * result.product.carbonPerMb());
        }
        heat = Math.max(0, heat - removedHeat);
        return result;
    }

    public int capacityMb() {
        return capacity;
    }

    public Surface surface() {
        int stored = metal + slag;
        if (stored == 0) return Surface.EMPTY;
        return new Surface(
            stored / (float) capacity,
            slag > 0 ? IronProduct.SLAG : currentProduct(),
            slag > 0 ? liquidSlagMb() == slag : liquidMetalMb() == metal);
    }

    public int metalCapacityMb() {
        return capacity - capacity / 32;
    }

    public int metalMb() {
        return metal;
    }

    public int liquidMetalMb() {
        return (int) Math.floor(metal * liquidFraction() + 1e-9);
    }

    public int slagMb() {
        return slag;
    }

    public int liquidSlagMb() {
        return (int) Math.floor(slag * liquidFraction() + 1e-9);
    }

    public double carbon() {
        return carbon;
    }

    public int impurities() {
        return impurities;
    }

    public int calciteCapacity() {
        return calcite;
    }

    public int pendingMetalMb() {
        int amount = 0;
        for (Charge charge : charges) if (charge.product != null) amount += 144 * charge.count;
        return amount;
    }

    public int occupiedMb() {
        return metal + slag + pendingMetalMb();
    }

    public IronProduct currentProduct() {
        return IronProduct.classify(carbon, metal);
    }

    public double temperature() {
        double sensible = sensibleThreshold();
        if (heat < sensible) return AMBIENT_TEMPERATURE + heat / sensibleCapacity();
        double latent = (metal + slag) * LATENT_PER_MB;
        if (heat < sensible + latent) return MELTING_TEMPERATURE;
        return MELTING_TEMPERATURE + (heat - sensible - latent) / sensibleCapacity();
    }

    private double liquidFraction() {
        if (metal + slag == 0 || heat <= sensibleThreshold()) return 0;
        return Math.min(1, (heat - sensibleThreshold()) / ((metal + slag) * LATENT_PER_MB));
    }

    private double sensibleCapacity() {
        return shellCapacity() + (metal + slag) * SENSIBLE_PER_MB;
    }

    private double sensibleThreshold() {
        return sensibleCapacity() * 1400;
    }

    private double shellCapacity() {
        return 128000.0 / 1400;
    }

    private double residentHeatAt(double temperature) {
        return sensibleCapacity() * (temperature - AMBIENT_TEMPERATURE)
            + (temperature >= MELTING_TEMPERATURE ? (metal + slag) * LATENT_PER_MB : 0);
    }

    private double queuedCarbon() {
        double value = 0;
        for (Charge charge : charges) {
            value += (144 * charge.product.carbonPerMb()) * charge.count;
        }
        return value;
    }

    private static void requireTier(int tier) {
        if (tier < 0 || tier > MAX_COIL_TIER) throw new IllegalArgumentException("Invalid coil tier");
    }

    private static void requireBudget(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid thermal budget");
    }

    public void save(NBTTagCompound tag) {
        tag.setInteger("CoilTier", coilTier);
        tag.setInteger("Metal", metal);
        tag.setInteger("Slag", slag);
        tag.setDouble("Carbon", carbon);
        tag.setInteger("Impurities", impurities);
        tag.setInteger("Calcite", calcite);
        tag.setDouble("Heat", heat);
        tag.setDouble("Insulation", insulation);
        NBTTagCompound slots = new NBTTagCompound();
        int index = 0;
        for (Charge charge : charges) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("Product", charge.product.ordinal());
            entry.setDouble("Heat", charge.heat);
            entry.setInteger("Count", charge.count);
            if (charge.stack != null) entry.setTag("Stack", charge.stack.writeToNBT(new NBTTagCompound()));
            slots.setTag(Integer.toString(index), entry);
            index += charge.count;
        }
        tag.setTag("Charges", slots);
    }

    public void load(NBTTagCompound tag) {
        int savedTier = tag.getInteger("CoilTier");
        requireTier(savedTier);
        charges.clear();
        metal = tag.getInteger("Metal");
        slag = tag.getInteger("Slag");
        carbon = tag.getDouble("Carbon");
        impurities = tag.getInteger("Impurities");
        calcite = tag.getInteger("Calcite");
        heat = tag.getDouble("Heat");
        setInsulation(tag.getDouble("Insulation"));
        NBTTagCompound slots = tag.getCompoundTag("Charges");
        List<String> keys = new ArrayList<>(slots.func_150296_c());
        keys.sort(Comparator.comparingInt(Integer::parseInt));
        int loadedCount = 0;
        int limit = BASE_CAPACITY_MB / 144 * (1 << MAX_COIL_TIER);
        for (String key : keys) {
            int index = Integer.parseInt(key);
            NBTTagCompound entry = slots.getCompoundTag(key);
            int count = entry.hasKey("Count") ? entry.getInteger("Count") : 1;
            if (index < 0 || count <= 0 || count > limit - loadedCount)
                throw new IllegalArgumentException("Invalid saved charge group");
            loadedCount += count;
            int product = entry.getInteger("Product");
            if (product < 0) {
                Fuel fuel = Fuel.values()[entry.getInteger("Fuel")];
                carbon += fuel.carbon * count;
                impurities = Math.addExact(impurities, Math.multiplyExact(fuel.impurities, count));
                heat += entry.getDouble("Heat") * count;
                continue;
            }
            Charge charge = new Charge(
                PRODUCTS[product],
                entry.hasKey("Stack") ? ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Stack")) : null,
                count);
            charge.heat = entry.getDouble("Heat");
            insertCharge(charge);
        }
        // Retain loaded charges and reserves before selecting a configuration
        while (!configure(savedTier)) {
            if (savedTier == MAX_COIL_TIER)
                throw new IllegalArgumentException("Saved bath exceeds maximum coil capacity");
            savedTier++;
        }
        heat = Math.min(heat, residentHeatAt(maximumTemperature()));
    }
}
