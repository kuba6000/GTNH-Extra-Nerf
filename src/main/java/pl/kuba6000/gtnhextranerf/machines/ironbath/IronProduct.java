package pl.kuba6000.gtnhextranerf.machines.ironbath;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.github.bsideup.jabel.Desugar;

/** Carbon units per ingot and composition bands for extracted products. */
public enum IronProduct {

    IRON(0, 0, 0),
    WROUGHT_IRON(0.2, 0, 0.2),
    STEEL(1, 0.2, 2),
    CAST_IRON(2.75, 2, 3.5),
    PIG_IRON(4, 3.5, 4.5),
    OVERCARBURIZED_IRON(4.5, 4.5, Double.POSITIVE_INFINITY),
    SLAG(0, 0, 0);

    private final double carbon;
    private final double lower;
    private final double upper;

    IronProduct(double carbon, double lower, double upper) {
        this.carbon = carbon;
        this.lower = lower;
        this.upper = upper;
    }

    public double carbonPerIngot() {
        return carbon;
    }

    public double carbonPerMb() {
        return carbon / 144;
    }

    public double lowerCarbonRatio() {
        return lower / 144;
    }

    public double upperCarbonRatio() {
        return upper / 144;
    }

    public int displayColor() {
        return switch (this) {
            case IRON -> 0xFFD1D7DA;
            case WROUGHT_IRON -> 0xFF789CB7;
            case STEEL -> 0xFF9C87B5;
            case CAST_IRON -> 0xFF729F94;
            case PIG_IRON -> 0xFFB1A069;
            case OVERCARBURIZED_IRON -> 0xFFAE809C;
            case SLAG -> 0xFF898063;
        };
    }

    /** Whole metal and fuel inputs, consumed Calcite dusts and output millibuckets. */
    @Desugar
    public record BatchBalance(int ironItems, int fuelItems, double calciteItems, int metalMb, int slagMb) {}

    /**
     * Finds the smallest whole-metal and whole-fuel balance, including fractional Calcite consumption.
     * This material reference excludes carbon lost through natural burning or Oxygen injection.
     */
    public BatchBalance balance(IronBath.Fuel fuel) {
        if (this == SLAG) throw new IllegalArgumentException("Slag is a coproduct of an alloy balance");
        if (this == IRON) return new BatchBalance(1, 0, 0, 144, 0);

        BigDecimal carbonPerIron = BigDecimal.valueOf(carbonPerIngot());
        BigDecimal carbonPerFuel = BigDecimal.valueOf(fuel.carbon());
        int scale = Math.max(carbonPerIron.scale(), carbonPerFuel.scale());
        BigInteger ironCarbon = carbonPerIron.movePointRight(scale)
            .toBigIntegerExact();
        BigInteger fuelCarbon = carbonPerFuel.movePointRight(scale)
            .toBigIntegerExact();
        BigInteger carbonDivisor = ironCarbon.gcd(fuelCarbon);
        BigInteger ironItems = fuelCarbon.divide(carbonDivisor);
        BigInteger fuelItems = ironCarbon.divide(carbonDivisor);

        BigInteger impurities = fuelItems.multiply(BigInteger.valueOf(fuel.impurities()));
        return new BatchBalance(
            ironItems.intValueExact(),
            fuelItems.intValueExact(),
            impurities.intValueExact() / 20.0,
            ironItems.multiply(BigInteger.valueOf(144))
                .intValueExact(),
            impurities.multiply(BigInteger.valueOf(32))
                .intValueExact());
    }

    public static IronProduct classify(double carbon, int metalMb) {
        if (metalMb <= 0 || carbon <= 0) return IRON;
        double content = carbon * 144 / metalMb;
        if (content <= 0.2) return WROUGHT_IRON;
        if (content < 2) return STEEL;
        if (content < 3.5) return CAST_IRON;
        if (content < 4.5) return PIG_IRON;
        return OVERCARBURIZED_IRON;
    }
}
