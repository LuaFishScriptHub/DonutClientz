package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Detached camera. Your character stays where it is (movement input is zeroed by KeyboardInputMixin). */
public final class Freecam {
    public static boolean active;
    public static boolean bypass;          // lets our own chest click through the "no interaction" callbacks
    private static ArmorStand cam;
    private static double x, y, z;
    private static float yaw, pitch, lockYaw, lockPitch;
    private static boolean useWas;

    public static void toggle(Minecraft mc) { if (active) off(mc); else on(mc); }

    private static void on(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        cam.setNoGravity(true);
        cam.setInvisible(true);
        x = p.getX(); y = p.getEyeY(); z = p.getZ();
        yaw = lockYaw = p.getYRot(); pitch = lockPitch = p.getXRot();
        apply();
        cam.setOldPosAndRot();
        mc.setCameraEntity(cam);
        active = true;
        p.displayClientMessage(Component.literal("Freecam ON - right click chests (server reach rules still apply)"), true);
    }

    public static void off(Minecraft mc) {
        active = false;
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.setYRot(lockYaw); mc.player.setXRot(lockPitch);
            mc.player.displayClientMessage(Component.literal("Freecam OFF"), true);
        }
        cam = null;
    }

    private static void apply() {
        cam.setPos(x, y - cam.getEyeHeight(), z);
        cam.setYRot(yaw); cam.setXRot(pitch);
    }

    /** Called every rendered frame: moves mouse-look from the (frozen) player to the camera. */
    public static void frame(Minecraft mc) {
        if (!active || cam == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        float dy = p.getYRot() - lockYaw, dp = p.getXRot() - lockPitch;
        if (dy != 0 || dp != 0) {
            yaw += dy; pitch = Mth.clamp(pitch + dp, -90f, 90f);
            p.setYRot(lockYaw); p.setXRot(lockPitch); p.yRotO = lockYaw; p.xRotO = lockPitch;
        }
        cam.setYRot(yaw); cam.setXRot(pitch); cam.yRotO = yaw; cam.xRotO = pitch;
    }

    public static void tick(Minecraft mc) {
        if (!active) return;
        if (mc.player == null || mc.level == null || cam == null || cam.level() != mc.level) { off(mc); return; }
        cam.setOldPosAndRot();
        var o = mc.options;
        double sp = o.keySprint.isDown() ? 1.6 : 0.7;
        double fwd = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double str = (o.keyLeft.isDown() ? 1 : 0) - (o.keyRight.isDown() ? 1 : 0);
        double vert = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0);
        double yr = Math.toRadians(yaw);
        if (mc.screen == null) {
            x += (-Math.sin(yr) * fwd + Math.cos(yr) * str) * sp;
            z += (Math.cos(yr) * fwd + Math.sin(yr) * str) * sp;
            y += vert * sp;
        }
        apply();

        boolean use = o.keyUse.isDown();
        if (use && !useWas && mc.screen == null) interact(mc);
        useWas = use;
    }

    private static void interact(Minecraft mc) {
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        Vec3 dir = new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3 from = new Vec3(x, y, z), to = from.add(dir.scale(6.0));
        BlockHitResult hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        bypass = true;
        try {
            LootLog.last = hit.getBlockPos();
            LootLog.lastTick = mc.level.getGameTime();
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        } finally { bypass = false; }
    }
}
