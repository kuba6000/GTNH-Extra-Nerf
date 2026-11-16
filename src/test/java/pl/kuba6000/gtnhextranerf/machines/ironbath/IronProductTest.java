package pl.kuba6000.gtnhextranerf.machines.ironbath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class IronProductTest {

    @Test
    void oneCokeSuppliesOneHundredSteelIngots() {
        IronProduct.BatchBalance balance = IronProduct.STEEL.balance(IronBath.Fuel.COKE);
        assertEquals(100, balance.ironItems());
        assertEquals(1, balance.fuelItems());
        assertEquals(0.05, (double) balance.calciteItems(), 1e-12);
        assertEquals(14400, balance.metalMb());
        assertEquals(32, balance.slagMb());
    }

    @Test
    void plantCokesProvideSmallerCarbonDosesWithOneImpurity() {
        for (IronBath.Fuel fuel : new IronBath.Fuel[] { IronBath.Fuel.CACTUS_COKE, IronBath.Fuel.SUGAR_COKE }) {
            assertEquals(25, fuel.carbon());
            assertEquals(1, fuel.impurities());
            assertEquals(new IronProduct.BatchBalance(25, 1, 0.05, 3600, 32), IronProduct.STEEL.balance(fuel));
        }
    }

    @Test
    void wroughtIronUsesOneCokePerFiveHundredIngots() {
        assertEquals(
            new IronProduct.BatchBalance(500, 1, 0.05, 72000, 32),
            IronProduct.WROUGHT_IRON.balance(IronBath.Fuel.COKE));
    }

    @Test
    void everyAlloyFuelBalanceIsMinimalAndConservesAllConstituents() {
        for (IronProduct product : IronProduct.values()) {
            if (product == IronProduct.IRON || product == IronProduct.SLAG) continue;
            for (IronBath.Fuel fuel : IronBath.Fuel.values()) {
                String context = product + " / " + fuel;
                IronProduct.BatchBalance balance = product.balance(fuel);
                assertTrue(balance.ironItems() > 0, context);
                assertTrue(balance.fuelItems() > 0, context);
                assertTrue(balance.calciteItems() > 0, context);
                assertEquals(balance.ironItems() * 144L, balance.metalMb(), context);
                assertEquals(
                    balance.fuelItems() * (long) fuel.impurities(),
                    balance.calciteItems() * 20,
                    1e-9,
                    context);
                assertEquals(balance.fuelItems() * (long) fuel.impurities() * 32, balance.slagMb(), context);
                BigDecimal suppliedCarbon = BigDecimal.valueOf(fuel.carbon())
                    .multiply(BigDecimal.valueOf(balance.fuelItems()));
                BigDecimal extractedCarbon = BigDecimal.valueOf(product.carbonPerIngot())
                    .multiply(BigDecimal.valueOf(balance.ironItems()));
                assertEquals(0, suppliedCarbon.compareTo(extractedCarbon), context);
                // The carbon balance fixes the metal-to-fuel ratio, so coprime counts give its smallest batch.
                assertEquals(
                    BigInteger.ONE,
                    BigInteger.valueOf(balance.ironItems())
                        .gcd(BigInteger.valueOf(balance.fuelItems())),
                    context);
            }
        }
    }

    @Test
    void approvedProductsUseExactWholeItemFuelRatios() {
        IronProduct[] products = { IronProduct.WROUGHT_IRON, IronProduct.STEEL, IronProduct.CAST_IRON,
            IronProduct.PIG_IRON, IronProduct.OVERCARBURIZED_IRON };
        int[][] iron = { { 500, 500, 400, 125, 125 }, { 100, 100, 80, 25, 25 }, { 400, 400, 320, 100, 100 },
            { 25, 25, 20, 25, 25 }, { 200, 200, 160, 50, 50 } };
        int[][] fuel = { { 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 1 }, { 11, 11, 11, 11, 11 }, { 1, 1, 1, 4, 4 },
            { 9, 9, 9, 9, 9 } };
        IronBath.Fuel[] fuels = { IronBath.Fuel.COAL, IronBath.Fuel.COKE, IronBath.Fuel.CHARCOAL,
            IronBath.Fuel.CACTUS_COKE, IronBath.Fuel.SUGAR_COKE };
        for (int p = 0; p < products.length; p++) {
            for (int f = 0; f < fuels.length; f++) {
                IronProduct.BatchBalance balance = products[p].balance(fuels[f]);
                assertEquals(iron[p][f], balance.ironItems(), products[p] + "/" + fuels[f]);
                assertEquals(fuel[p][f], balance.fuelItems(), products[p] + "/" + fuels[f]);
            }
        }
    }

    @Test
    void remeltingCanonicalProductsPreservesTheirIdentityAtTheExtremeBandEdges() {
        for (IronProduct product : IronProduct.values()) {
            if (product == IronProduct.SLAG) continue;
            assertEquals(product, IronProduct.classify(product.carbonPerIngot(), 144));
        }
        assertEquals(IronProduct.WROUGHT_IRON, IronProduct.classify(20, 14400));
        assertEquals(IronProduct.STEEL, IronProduct.classify(20.001, 14400));
        assertEquals(IronProduct.PIG_IRON, IronProduct.classify(449.999, 14400));
        assertEquals(IronProduct.OVERCARBURIZED_IRON, IronProduct.classify(450, 14400));
    }

    @Test
    void directIronMeltingNeedsNoFuelOrCalcite() {
        for (IronBath.Fuel fuel : IronBath.Fuel.values())
            assertEquals(new IronProduct.BatchBalance(1, 0, 0, 144, 0), IronProduct.IRON.balance(fuel));
    }

    @Test
    void slagRequiresAnAlloyBalanceAsItsSource() {
        assertThrows(IllegalArgumentException.class, () -> IronProduct.SLAG.balance(IronBath.Fuel.COAL));
    }
}
