package pl.kuba6000.gtnhextranerf.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientTest;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import pl.kuba6000.gtnhextranerf.Config;
import pl.kuba6000.gtnhextranerf.telemetry.ConsentPrompt;

/** A dismissed consent window is offered again when the same operator logs in. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class ConsentScenarios {

    private ConsentScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 1800)
    public static void promptAfterAnotherLogin(GameTestHelper helper) {
        int[] settledAt = { 0 };
        ClientTest.scenario(helper)
            .defaultTimeoutTicks(400)
            .server("let login and other startup screens settle", () -> {
                ConsentPrompt.stop();
                player().closeScreen();
                helper.afterTest(ConsentPrompt::stop);
                settledAt[0] = MinecraftServer.getServer()
                    .getTickCounter() + 200;
            })
            .client(
                "close startup screens",
                client -> Minecraft.getMinecraft()
                    .displayGuiScreen(null))
            .awaitServer("the player is ready", () -> {
                if (MinecraftServer.getServer()
                    .getTickCounter() < settledAt[0] || player().openContainer != player().inventoryContainer)
                    throw new AssertionError("Login has not settled");
            })
            .awaitClient(
                "no other screen is open",
                client -> {
                    if (Minecraft.getMinecraft().currentScreen != null)
                        throw new AssertionError("A screen is still open");
                })
            .server("schedule consent on login", () -> {
                if (Config.telemetryAsked) throw new AssertionError("Scratch world has already answered");
                ConsentPrompt.start(MinecraftServer.getServer(), false);
                loginEvent();
            })
            .awaitScreen(GuiContainerWrapper.class)
            .capture("consent-window")
            .escape()
            .awaitClient(
                "the unanswered window is closed",
                client -> {
                    if (Minecraft.getMinecraft().currentScreen != null)
                        throw new AssertionError("Consent window is still open");
                })
            .awaitServer("the server closed the window without recording a choice", () -> {
                if (Config.telemetryAsked || player().openContainer != player().inventoryContainer)
                    throw new AssertionError("Consent was recorded or its container remains open");
            })
            .server("simulate the next login event", ConsentScenarios::loginEvent)
            .awaitScreen(GuiContainerWrapper.class)
            .capture("consent-after-login")
            .escape()
            .awaitClient(
                "the second window is closed",
                client -> {
                    if (Minecraft.getMinecraft().currentScreen != null)
                        throw new AssertionError("Consent window is still open");
                })
            .succeed();
    }

    private static void loginEvent() {
        FMLCommonHandler.instance()
            .bus()
            .post(new PlayerEvent.PlayerLoggedInEvent(player()));
    }

    private static EntityPlayerMP player() {
        return (EntityPlayerMP) MinecraftServer.getServer()
            .getConfigurationManager().playerEntityList.get(0);
    }
}
