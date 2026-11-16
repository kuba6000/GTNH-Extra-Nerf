package pl.kuba6000.gtnhextranerf.client.render;

import java.util.Arrays;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.kuba6000.gtnhextranerf.machines.IronBathFurnace;
import pl.kuba6000.gtnhextranerf.machines.IronBathStructure;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronBath.Surface;
import pl.kuba6000.gtnhextranerf.machines.ironbath.IronProduct;

/** Draws the exposed surface inside the chimney using native material textures. */
@SideOnly(Side.CLIENT)
public final class IronBathSurfaceRenderer {

    private static final Fluid[] FLUIDS = Arrays.stream(IronProduct.values())
        .map(
            product -> IronBathFurnace.material(product)
                .getMolten(1)
                .getFluid())
        .toArray(Fluid[]::new);

    private IronBathSurfaceRenderer() {}

    public static void render(ExtendedFacing facing, Surface surface, double x, double y, double z) {
        if (surface.fill() <= 0) return;
        Fluid fluid = FLUIDS[surface.product()
            .ordinal()];
        IIcon icon = fluid.getStillIcon();
        double minU = surface.molten() ? icon.getMinU() : 0;
        double maxU = surface.molten() ? icon.getMaxU() : 1;
        double minV = surface.molten() ? icon.getMinV() : 0;
        double maxV = surface.molten() ? icon.getMaxV() : 0.05;
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glPushMatrix();
        try {
            Minecraft.getMinecraft().renderEngine
                .bindTexture(surface.molten() ? TextureMap.locationBlocksTexture : IronBathFurnace.FROZEN_BATH_TEXTURE);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            if (surface.molten()) OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
            Tessellator vertices = Tessellator.instance;
            vertices.startDrawingQuads();
            if (surface.molten() || surface.product() != IronProduct.SLAG) vertices.setColorOpaque_I(fluid.getColor());
            else {
                short[] tint = IronBathFurnace.material(surface.product()).mRGBa;
                vertices.setColorOpaque(tint[0], tint[1], tint[2]);
            }
            double[] point = new double[3];
            double[] world = new double[3];
            point[1] = IronBathStructure.TANK_BOTTOM_B - surface.fill() * IronBathStructure.TANK_HEIGHT;
            for (int a = 0; a < IronBathStructure.TANK_WIDTH; a++)
                for (int c = 0; c < IronBathStructure.TANK_WIDTH; c++) {
                    point[0] = IronBathStructure.TANK_MIN_A + a;
                    point[2] = IronBathStructure.TANK_MIN_C + c;
                    facing.getWorldOffset(point, world);
                    vertices.addVertexWithUV(world[0], world[1], world[2], minU, minV);
                    point[0]++;
                    facing.getWorldOffset(point, world);
                    vertices.addVertexWithUV(world[0], world[1], world[2], maxU, minV);
                    point[2]++;
                    facing.getWorldOffset(point, world);
                    vertices.addVertexWithUV(world[0], world[1], world[2], maxU, maxV);
                    point[0]--;
                    facing.getWorldOffset(point, world);
                    vertices.addVertexWithUV(world[0], world[1], world[2], minU, maxV);
                }
            vertices.draw();
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
