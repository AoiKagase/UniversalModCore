package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.world.World;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return Minecraft.getMinecraft().player != null;
    }

    private static Player playerCache;
    private static Entity entityCache;

	/** Hey, it's you! */
    public static Player getPlayer() {
        EntityPlayerSP internal = Minecraft.getMinecraft().player;
        if (internal == null) {
            throw new RuntimeException("Called to get the player before minecraft has actually started!");
        }
        if (entityCache == null) {
            entityCache = World.get(internal.world).getEntity(internal);
        }
		// Cannot join immediately after game startup in multiplayer.
		// The following fixes avoided the error, but the GUI cannot be displayed in IR when the game is joined immediately after startup. 
		// It is possible to work around it by re-logging in.
        if (playerCache == null || internal != playerCache.internal) {
            if (entityCache == null) {
                playerCache = new Player(Minecraft.getMinecraft().player);
            } else {
                playerCache = World.get(internal.world).getEntity(internal).asPlayer();
            }
        }
		// -----
        return playerCache;
    }

    /** Hooks into the GUI profiler */
    public static void startProfiler(String section) {
        Minecraft.getMinecraft().profiler.startSection(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        Minecraft.getMinecraft().profiler.endSection();
    }

    /** Entity that you are currently looking at (distance limited) */
    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = Minecraft.getMinecraft().objectMouseOver.entityHit;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUniqueID(), Entity.class);
        }
        return null;
    }

    /** Block you are currently pointing at (distance limited) */
    public static Vec3i getBlockMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK ? new Vec3i(Minecraft.getMinecraft().objectMouseOver.getBlockPos()) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK ? new Vec3d(Minecraft.getMinecraft().objectMouseOver.hitVec) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }
}
