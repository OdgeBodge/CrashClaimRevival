package net.crashcraft.crashclaim.data;

public class MathUtils {
    public static boolean iskPointCollide(int minX, int minZ, int maxX, int maxZ, int x, int z) {
        return (z >= minZ && z <= maxZ) && (x >= minX && x <= maxX);
    }

    public static boolean isk3DPointCollide(int minX, int minZ, int maxX, int maxZ, int minY, int maxY, int x, int z, int y) {
        return (z >= minZ && z <= maxZ) && (x >= minX && x <= maxX) && (y >= minY && y <= maxY);
    }

    public static boolean doOverlap(int minX_1, int minZ_1, int maxX_1, int maxZ_1, int minX_2, int minZ_2, int maxX_2, int maxZ_2) {
        return !(minX_2 > maxX_1 ||
                minZ_2 > maxZ_1 ||
                minX_1 > maxX_2 ||
                minZ_1 > maxZ_2);
    }

    public static boolean containedInside(int minX_1, int minZ_1, int maxX_1, int maxZ_1, int minX_2, int minZ_2, int maxX_2, int maxZ_2) {
        return iskPointCollide(minX_1, minZ_1, maxX_1, maxZ_1, minX_2, minZ_2)
                && iskPointCollide(minX_1, minZ_1, maxX_1, maxZ_1, maxX_2, maxZ_2);
    }

    public static boolean doOverlapCuboid(int minX_1, int minY_1, int minZ_1, int maxX_1, int maxY_1, int maxZ_1,
                                          int minX_2, int minY_2, int minZ_2, int maxX_2, int maxY_2, int maxZ_2) {

            return !(
                minX_2 > maxX_1 ||
                minZ_2 > maxZ_1 ||
                minX_1 > maxX_2 ||
                minZ_1 > maxZ_2 ||
                minY_2 > maxY_1 ||
                minY_1 > maxY_2
            );

    }
}
