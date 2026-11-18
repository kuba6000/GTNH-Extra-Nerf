package pl.kuba6000.gtnhextranerf.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.AxisAlignedBB;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;

/** Chimney, feed conveyor and physical port positions relative to a north-facing controller. */
public final class IronBathStructure {

    public static final int WIDTH = 13;
    public static final int HEIGHT = 12;
    public static final int DEPTH = 7;
    public static final int OFFSET_X = 11;
    public static final int OFFSET_Y = 10;
    public static final int OFFSET_Z = 2;
    public static final int MIN_X = -1;
    public static final int MAX_X = 11;
    public static final int MIN_Y = -1;
    public static final int MAX_Y = 10;
    public static final int MIN_Z = -2;
    public static final int MAX_Z = 4;
    // Tank boundaries in StructureLib's controller-centred ABC coordinates
    public static final double TANK_MIN_A = -9.5;
    public static final double TANK_BOTTOM_B = -2.5;
    public static final double TANK_MIN_C = -0.5;
    public static final int TANK_WIDTH = 3;
    public static final int TANK_HEIGHT = 5;

    @Desugar
    public record Offset(int x, int y, int z) {}

    public static final Offset INPUT_BUS = new Offset(1, 1, 0);
    public static final Offset OXYGEN_INPUT = new Offset(-1, 1, 0);
    public static final Offset MAINTENANCE = new Offset(0, 1, 2);
    public static final Offset PREVIEW_MEASUREMENT = new Offset(8, 1, -2);
    public static final Offset ENERGY_A = new Offset(8, -1, -2);
    public static final Offset ENERGY_B = new Offset(8, -1, 4);
    public static final Offset EXTRA_ENERGY = new Offset(7, -1, -2);
    public static final Offset METAL_TAP = new Offset(9, 3, -1);
    public static final Offset SAMPLE_TAP = new Offset(9, 4, -1);
    public static final Offset SLAG_TAP = new Offset(7, 7, -1);
    public static final Offset CRITICAL_CASING = new Offset(11, 9, 1);

    private static final String[][] SHAPE = {
        { "  CCC        ", " C   C       ", "C     C      ", "C     C      ", "C     C      ", " C   C       ",
            "  CCC        " },
        { "  CCC        ", " CCCCC       ", "CC---CC      ", "CC---CC      ", "CC---CC      ", " CCCCC       ",
            "  CCC        " },
        { "   C         ", "  CCC        ", " C---C       ", "CC---        ", " C---C       ", "  CCC        ",
            "   C         " },
        { "             ", "  TKT        ", " T---T       ", " K---TF      ", " T---T       ", "  TKT        ",
            "             " },
        { "             ", "  TKT        ", " T---T       ", " K---K F     ", " T---T       ", "  TKT        ",
            "             " },
        { "             ", "  TKT        ", " T---T       ", " K---K  F    ", " T---T       ", "  TKT        ",
            "             " },
        { "             ", "  TKT        ", " T---T       ", " K---K   F   ", " T---T       ", "  TKT        ",
            "             " },
        { "             ", " CTKTC       ", " T---T       ", " K---K    F  ", " T---T       ", " CTKTC       ",
            "             " },
        { "   C         ", " CCCCC       ", " CCCCC       ", "CCCPCCC    F ", " CCCCC       ", " CCCCC       ",
            "   C         " },
        { "  CCC        ", " CCCCC       ", "CCCCCCC   MMM", "CCCPCCC   MMM", "CCCCCCC   MMM", " CCCCC       ",
            "  CCC        " },
        { "  KKK        ", " KKKKK       ", "KKKKKKC   M~M", "KKKPPPPPPPMMM", "KKKKKKC   MMM", " KKKKK       ",
            "  KKK        " },
        { "  CEE        ", " CCCCC       ", "CCCCCCC   MMM", "CCCCCCCFFFMMM", "CCCCCCC   MMM", " CCCCC       ",
            "  CEC        " } };
    public static final List<Offset> COIL_POSITIONS = findCoils();

    private IronBathStructure() {}

    public static String[][] shape() {
        String[][] result = new String[SHAPE.length][];
        for (int y = 0; y < SHAPE.length; y++) {
            result[y] = SHAPE[y].clone();
            if (y == HEIGHT - 1) for (int z = 0; z < DEPTH; z++) result[y][z] = result[y][z].replace('C', 'E');
        }
        return result;
    }

    public static int portBand(int relativeY) {
        return relativeY - METAL_TAP.y();
    }

    public static AxisAlignedBB tankBounds(ExtendedFacing facing, int x, int y, int z) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        double[] point = new double[3];
        double[] world = new double[3];
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) for (int c = 0; c < 2; c++) {
            point[0] = TANK_MIN_A + a * TANK_WIDTH;
            point[1] = TANK_BOTTOM_B - b * TANK_HEIGHT;
            point[2] = TANK_MIN_C + c * TANK_WIDTH;
            facing.getWorldOffset(point, world);
            minX = Math.min(minX, world[0]);
            minY = Math.min(minY, world[1]);
            minZ = Math.min(minZ, world[2]);
            maxX = Math.max(maxX, world[0]);
            maxY = Math.max(maxY, world[1]);
            maxZ = Math.max(maxZ, world[2]);
        }
        return AxisAlignedBB.getBoundingBox(
            x + 0.5 + minX,
            y + 0.5 + minY,
            z + 0.5 + minZ,
            x + 0.5 + maxX,
            y + 0.5 + maxY,
            z + 0.5 + maxZ);
    }

    private static List<Offset> findCoils() {
        List<Offset> result = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y++) for (int z = 0; z < DEPTH; z++) for (int x = 0; x < WIDTH; x++) {
            if (SHAPE[y][z].charAt(x) == 'K') result.add(new Offset(OFFSET_X - x, OFFSET_Y - y, z - OFFSET_Z));
        }
        return Collections.unmodifiableList(result);
    }
}
