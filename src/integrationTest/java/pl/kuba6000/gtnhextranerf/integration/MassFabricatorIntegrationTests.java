package pl.kuba6000.gtnhextranerf.integration;

import java.util.UUID;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.tileentities.machines.basic.MTEMassfabricator;

final class MassFabricatorIntegrationTests {

    private MassFabricatorIntegrationTests() {}

    static void verify(WorldServer world) {
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(UUID.randomUUID(), "AmpGateTest"));
        ItemStack controller = ItemList.MassFabricatorLuV.get(1);
        FixtureWorld.loadAround(world, 704, 160);
        world.setBlockToAir(704, 210, 160);
        check(
            ((ItemBlock) controller.getItem())
                .placeBlockAt(controller, player, world, 704, 210, 160, 1, 0.5f, 0.5f, 0.5f, 0),
            "Could not place native Mass Fabricator");
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(704, 210, 160);
        MTEMassfabricator machine = (MTEMassfabricator) tile.getMetaTileEntity();
        int required = MTEMassfabricator.sUUAperUUM;
        check(required > 0, "Native amplifier fee must be positive");
        machine.setFillableStack(null);
        // The public checkRecipe contract defines result 2 as a successfully used recipe.
        check(machine.checkRecipe() != 2, "Singleblock produces UU without amplifier despite the NEI change");
        machine.setFillableStack(Materials.UUAmplifier.getFluid(required - 1));
        check(machine.checkRecipe() != 2, "Singleblock accepts insufficient amplifier");
        machine.setFillableStack(Materials.UUAmplifier.getFluid(required * 2));
        check(machine.checkRecipe() == 2, "Native amplified singleblock batch was rejected");
        check(
            machine.getFillableStack().amount == required,
            "Singleblock did not consume the native amplifier fee once");
        FixtureWorld.loadAround(world, 704, 160);
        world.setBlockToAir(704, 210, 160);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
