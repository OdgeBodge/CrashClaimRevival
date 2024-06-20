package net.crashcraft.crashclaim.data;

import org.bukkit.Location;

public class StaticClaimLogic {
    public static long getChunkHash(long chunkX, long chunkZ) {
        return (chunkZ ^ (chunkX << 32));
    }

    public static long getChunkHashFromLocation(int x, int z) {
        return getChunkHash(x >> 4, z >> 4);
    }

    public static Location calculateMaxCorner(Location loc1, Location loc2){
        return new Location(loc1.getWorld(), Math.max(loc1.getBlockX(), loc2.getBlockX()), Math.max(loc1.getY(), loc2.getY()), Math.max(loc1.getBlockZ(), loc2.getBlockZ()));
    }

    public static Location calculateMinCorner(Location loc1, Location loc2){
        return new Location(loc1.getWorld(), Math.min(loc1.getBlockX(), loc2.getBlockX()), Math.min(loc1.getY(), loc2.getY()), Math.min(loc1.getBlockZ(), loc2.getBlockZ()));
    }

    public static boolean isClaimBorder(int min_x, int max_x, int min_z, int max_z, int point_x, int point_z){
        return ((point_x == min_x || point_x == max_x) && (point_z >= min_z && point_z <= max_z)) // Is on X border and falls in Z bounds
                || ((point_z == min_z || point_z == max_z) && (point_x >= min_x && point_x <= max_x)); // Is on Z border and falls in X bounds
    }

    public static boolean IsSubclaimBorder(int min_x, int max_x, int min_z, int max_z, int min_y, int max_y, int point_x, int point_z, int point_y){
        if (min_y == -500 && max_y == -500){
            return isClaimBorder(min_x, max_x, min_z, max_z, point_x, point_z);
        }
        boolean XZCheck = isClaimBorder(min_x, max_x, min_z, max_z, point_x, point_z);
        boolean YCheck = (point_y == min_y || point_y == max_y);

        return (XZCheck && YCheck);
    }
}
