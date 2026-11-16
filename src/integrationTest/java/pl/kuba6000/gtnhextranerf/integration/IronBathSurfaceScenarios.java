package pl.kuba6000.gtnhextranerf.integration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gtnewhorizons.horizonqa.api.GameTestHelper;
import com.gtnewhorizons.horizonqa.api.annotation.GameTest;
import com.gtnewhorizons.horizonqa.api.annotation.GameTestHolder;
import com.gtnewhorizons.horizonqa.api.client.ClientScenario;
import com.sun.management.ThreadMXBean;

import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;
import pl.kuba6000.gtnhextranerf.machines.MTEIronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;

/** Captures the actual chimney surface and measures native TESR submission separately from world rendering. */
@GameTestHolder(value = "gtnhextranerftests", clientOnly = true)
public final class IronBathSurfaceScenarios {

    private IronBathSurfaceScenarios() {}

    @GameTest(template = "native_fixture", timeoutTicks = 6000)
    public static void surfaceRenderingAndProfile(GameTestHelper helper) {
        IronBathScenarios.Fixture[] fixture = new IronBathScenarios.Fixture[1];
        List<SurfaceProfile> profiles = new ArrayList<>();
        float[] expected = new float[1];
        ClientScenario scenario = IronBathScenarios.prepareClientView(helper)
            .server("prepare a hot bath below the open chimney", () -> {
                fixture[0] = new IronBathScenarios.Fixture(helper);
                seed(fixture[0], 384 * 144, 0, false);
                expected[0] = 0.75f;
                EntityPlayerMP viewer = (EntityPlayerMP) MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList.get(0);
                viewer.setPositionAndUpdate(1288.5, 113, 97.5);
                viewer.rotationYaw = 0;
                viewer.rotationPitch = 90;
                viewer.motionX = viewer.motionY = viewer.motionZ = 0;
            })
            .client("keep the surface view still", client -> {
                Minecraft mc = Minecraft.getMinecraft();
                float fov = mc.gameSettings.fovSetting;
                boolean bobbing = mc.gameSettings.viewBobbing;
                boolean clouds = mc.gameSettings.clouds;
                client.afterTest(() -> {
                    mc.gameSettings.fovSetting = fov;
                    mc.gameSettings.viewBobbing = bobbing;
                    mc.gameSettings.clouds = clouds;
                });
                mc.gameSettings.fovSetting = 50;
                mc.gameSettings.viewBobbing = false;
                mc.gameSettings.clouds = false;
            })
            .awaitClient("the hot surface synchronizes without a GUI", client -> {
                assertSurface(expected[0], IronProduct.IRON, true);
                Minecraft mc = Minecraft.getMinecraft();
                check(
                    Math.abs(mc.thePlayer.posX - 1288.5) < 1 && Math.abs(mc.thePlayer.posZ - 97.5) < 1,
                    "The chimney camera has not synchronized");
                mc.thePlayer.setPosition(1288.5, 113, 97.5);
                mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
                mc.thePlayer.capabilities.isFlying = true;
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 90;
            });
        scenario.serverSequence()
            .thenIdle(15);
        scenario.capture("surface-liquid-metal");
        for (int count : new int[] { 1, 16, 64 }) {
            SurfaceProfile[] profile = new SurfaceProfile[1];
            scenario.client("prepare " + count + " visible bath surfaces", client -> {
                profile[0] = new SurfaceProfile(count);
                profiles.add(profile[0]);
                client.afterTest(profile[0]::close);
            });
            for (int batch = 0; batch < 8; batch++)
                scenario.client("warm surface renderer " + count + " " + batch, client -> profile[0].draw(false));
            for (int batch = 0; batch < 8; batch++)
                scenario.client("measure surface renderer " + count + " " + batch, client -> profile[0].draw(true));
            scenario.awaitClient("collect surface GPU samples " + count, client -> profile[0].collect());
        }
        scenario.server("add an upper slag layer", () -> {
            seed(fixture[0], 384 * 144, 1024, false);
            expected[0] = (384 * 144 + 1024) / (float) IronBath.BASE_CAPACITY_MB;
        })
            .awaitClient(
                "slag replaces the visible metal surface",
                client -> assertSurface(expected[0], IronProduct.SLAG, true))
            .capture("surface-liquid-slag");
        scenario.server("lower both layers", () -> {
            seed(fixture[0], 128 * 144, 1024, false);
            expected[0] = (128 * 144 + 1024) / (float) IronBath.BASE_CAPACITY_MB;
        })
            .awaitClient(
                "the surface follows the lower fill level",
                client -> assertSurface(expected[0], IronProduct.SLAG, true))
            .capture("surface-low-slag");
        scenario.server("freeze the contents and queue more metal", () -> seed(fixture[0], 128 * 144, 1024, true))
            .awaitClient(
                "frozen contents retain their height and queued metal stays outside",
                client -> assertSurface(expected[0], IronProduct.SLAG, false))
            .capture("surface-frozen-slag");
        scenario.serverSequence()
            .thenIdle(40);
        scenario.capture("surface-frozen-slag-later")
            .client("retain raw CPU, GPU and allocation measurements", client -> writeProfiles(profiles))
            .client(
                "the controller owns a world renderer",
                client -> check(
                    (Object) clientMachine() instanceof IMTERenderer,
                    "The Iron Bath has no world surface renderer"))
            .server("invalidate the furnace shell", () -> {
                var offset = IronBathStructure.CRITICAL_CASING;
                fixture[0].controller.getWorld()
                    .setBlockToAir(1280 + offset.x(), 100 + offset.y(), 96 + offset.z());
                check(!fixture[0].machine.checkStructure(true, fixture[0].controller), "The broken shell still forms");
                fixture[0].machine.onPostTick(fixture[0].controller, 10);
            })
            .awaitClient(
                "invalidating the structure clears the surface",
                client -> assertSurface(0, IronProduct.IRON, false))
            .succeed();
    }

    private static void seed(IronBathScenarios.Fixture fixture, int metal, int slag, boolean cold) {
        NBTTagCompound tag = new NBTTagCompound();
        fixture.machine.saveNBTData(tag);
        IronBath model = new IronBath(0);
        NBTTagCompound state = new NBTTagCompound();
        model.save(state);
        state.setInteger("Metal", metal);
        state.setInteger("Slag", slag);
        model.load(state);
        if (cold) model.addMetal(IronProduct.IRON, 32 * 144);
        else model.addHeat(model.heatDemand(2200), 0);
        model.save(state);
        tag.setTag("IronBath", state);
        fixture.machine.loadNBTData(tag);
        fixture.controller.disableWorking();
        fixture.machine.onPostTick(fixture.controller, 10);
    }

    private static MTEIronBathFurnace clientMachine() {
        Minecraft mc = Minecraft.getMinecraft();
        check(mc.currentScreen == null, "Surface inspection unexpectedly opened a GUI");
        check(mc.theWorld.getTileEntity(1280, 100, 96) instanceof BaseMetaTileEntity, "The controller is not loaded");
        BaseMetaTileEntity tile = (BaseMetaTileEntity) mc.theWorld.getTileEntity(1280, 100, 96);
        check(tile.getMetaTileEntity() instanceof MTEIronBathFurnace, "The client controller has not synchronized");
        return (MTEIronBathFurnace) tile.getMetaTileEntity();
    }

    private static void assertSurface(float fill, IronProduct product, boolean molten) {
        MTEIronBathFurnace machine = clientMachine();
        if (!((Object) machine instanceof IMTERenderer)) return;
        ByteBuf packet = Unpooled.buffer();
        try {
            machine.writeToStream(packet);
            packet.readerIndex(packet.writerIndex() - 6);
            float actual = packet.readFloat();
            int material = packet.readUnsignedByte();
            boolean liquid = packet.readBoolean();
            check(
                Math.abs(actual - fill) < 1e-6 && material == product.ordinal() && liquid == molten,
                "Surface data has not synchronized: fill=" + actual + ", material=" + material + ", liquid=" + liquid);
            var bounds = ((IMTERenderer) (Object) machine).getRenderBoundingBox(1280, 100, 96);
            check(
                bounds.minX == 1287 && bounds.maxX == 1290
                    && bounds.minY == 103
                    && bounds.maxY == 108
                    && bounds.minZ == 96
                    && bounds.maxZ == 99,
                "The surface has the wrong culling bounds");
        } finally {
            packet.release();
        }
    }

    private static void writeProfiles(List<SurfaceProfile> profiles) {
        JsonObject output = new JsonObject();
        output.addProperty(
            "scope",
            "native TESR submission only, 128 draws per sample, excludes world traversal and swaps");
        output.addProperty("limits", "one synchronized material per case, repeated in a spatial grid; no mesh cache");
        JsonArray cases = new JsonArray();
        for (SurfaceProfile profile : profiles) cases.add(profile.result);
        output.add("cases", cases);
        File file = new File(System.getProperty("horizonqa.reportDir"), "artifacts/surface-rendering-profile.json");
        check(
            file.getParentFile()
                .isDirectory()
                || file.getParentFile()
                    .mkdirs(),
            "Cannot create profile directory");
        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(output, writer);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot save surface profiles", failure);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class SurfaceProfile {

        private final int count;
        private final IMTERenderer renderer;
        private final ThreadMXBean counters;
        private final JsonObject result = new JsonObject();
        private final JsonArray samples = new JsonArray();
        private final List<Integer> queries = new ArrayList<>();
        private final List<JsonObject> pending = new ArrayList<>();
        private final boolean cpuEnabled;
        private final boolean allocationEnabled;

        private SurfaceProfile(int count) {
            this.count = count;
            Object machine = clientMachine();
            renderer = machine instanceof IMTERenderer ? (IMTERenderer) machine : null;
            counters = (ThreadMXBean) ManagementFactory.getThreadMXBean();
            check(
                counters.isCurrentThreadCpuTimeSupported() && counters.isThreadAllocatedMemorySupported(),
                "CPU/allocation counters are unavailable");
            check(
                GLContext.getCapabilities().OpenGL33 || GLContext.getCapabilities().GL_ARB_timer_query,
                "GPU timer queries are unavailable");
            cpuEnabled = counters.isThreadCpuTimeEnabled();
            allocationEnabled = counters.isThreadAllocatedMemoryEnabled();
            counters.setThreadCpuTimeEnabled(true);
            counters.setThreadAllocatedMemoryEnabled(true);
            result.addProperty("surfaces", count);
            result.addProperty("rendererPresent", renderer != null);
            result.addProperty("gpuRenderer", GL11.glGetString(GL11.GL_RENDERER));
            result.addProperty("width", Minecraft.getMinecraft().displayWidth);
            result.addProperty("height", Minecraft.getMinecraft().displayHeight);
            result.add("samples", samples);
        }

        private void draw(boolean measured) {
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "GL error before surface rendering");
            check(
                GL15.glGetQueryi(ARBTimerQuery.GL_TIME_ELAPSED, GL15.GL_CURRENT_QUERY) == 0,
                "Another GPU timer query is active");
            int query = measured ? GL15.glGenQueries() : 0;
            int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(-2, 34, -34, 2, -100, 100);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glRotatef(90, 1, 0, 0);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            float lightX = OpenGlHelper.lastBrightnessX;
            float lightY = OpenGlHelper.lastBrightnessY;
            try {
                long thread = Thread.currentThread()
                    .getId();
                long allocated = counters.getThreadAllocatedBytes(thread);
                long cpu = counters.getCurrentThreadCpuTime();
                if (query != 0) GL15.glBeginQuery(ARBTimerQuery.GL_TIME_ELAPSED, query);
                try {
                    for (int frame = 0; frame < 128; frame++)
                        for (int surface = 0; surface < count; surface++) if (renderer != null)
                            renderer.renderTESR((surface % 8) * 4 - 8.5, -5.5, (surface / 8) * 4 - 1.5, 0);
                } finally {
                    if (query != 0) GL15.glEndQuery(ARBTimerQuery.GL_TIME_ELAPSED);
                }
                JsonObject sample = new JsonObject();
                sample.addProperty("cpuNs", counters.getCurrentThreadCpuTime() - cpu);
                sample.addProperty("allocatedBytes", counters.getThreadAllocatedBytes(thread) - allocated);
                check(
                    GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) == texture
                        && GL11.glIsEnabled(GL11.GL_LIGHTING) == lighting
                        && OpenGlHelper.lastBrightnessX == lightX
                        && OpenGlHelper.lastBrightnessY == lightY,
                    "The surface renderer changed the caller's texture, lighting or lightmap state");
                if (measured) {
                    samples.add(sample);
                    queries.add(query);
                    pending.add(sample);
                }
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(mode);
                GL11.glPopAttrib();
            }
            check(GL11.glGetError() == GL11.GL_NO_ERROR, "Surface rendering left a GL error");
            if (measured) GL11.glFlush();
        }

        private void collect() {
            for (int index = 0; index < queries.size(); index++) {
                int query = queries.get(index);
                check(GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0, "GPU result is pending");
                pending.get(index)
                    .addProperty("gpuNs", ARBTimerQuery.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT));
            }
        }

        private void close() {
            for (int query : queries) GL15.glDeleteQueries(query);
            counters.setThreadCpuTimeEnabled(cpuEnabled);
            counters.setThreadAllocatedMemoryEnabled(allocationEnabled);
        }
    }
}
