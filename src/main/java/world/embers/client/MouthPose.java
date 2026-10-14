package world.embers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import world.embers.*;

/** Mouth placement is independent of the renderer used by the held item. */
public final class MouthPose {
    private static float side=1,lastTime=-1,swayX,swayY,swayPitch,swayRoll;
    public static float[] sway(){return new float[]{swayX,swayY,swayPitch,swayRoll};}
    public static boolean obstructs(ItemStack stack){
        if(stack.isEmpty()||stack.getItem() instanceof SmokingItem||stack.getItem() instanceof LighterItem||stack.getItem() instanceof ButtItem)return false;
        String ns=BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        return ns.equals("tacz")||ns.equals("pointblank")||ns.equals("cgm")||ns.equals("timeless_and_classics")||stack.getItem() instanceof BowItem||stack.getItem() instanceof CrossbowItem||stack.getItem() instanceof ShieldItem||stack.getItem() instanceof MapItem||stack.getUseAnimation()==UseAnim.SPEAR;
    }
    public static float sideFor(Player p){return obstructs(p.getMainHandItem())?(p.getMainArm()==HumanoidArm.RIGHT?-1:1):obstructs(p.getOffhandItem())?(p.getMainArm()==HumanoidArm.RIGHT?1:-1):1;}
    public static float side(){return side;}
    public static void apply(PoseStack poses,float partial){
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null)return;
        float time=p.tickCount+partial,dt=lastTime<0?1:Math.max(0,Math.min(1,time-lastTime));lastTime=time;
        side+=(sideFor(p)-side)*(1-(float)Math.exp(-dt*.42f));
        // Match GameRenderer's footstep phase. The bite point stays near the lip; the shaft
        // only inherits a small, damped part of the head motion rather than swinging as a held tool.
        float step=-(p.walkDist+(p.walkDist-p.walkDistO)*partial)*(float)Math.PI;
        float speed=net.minecraft.util.Mth.lerp(partial,p.oBob,p.bob);
        boolean walking=mc.options.bobView().get()&&p.onGround()&&!p.isPassenger()&&p.getDeltaMovement().horizontalDistanceSqr()>.000025;
        float amount=walking?Math.min(1.25f,speed*10)*(p.isSprinting()?1.22f:1):0;
        float blend=1-(float)Math.exp(-dt*.85f);
        swayX+=((float)Math.sin(step)*.0015f*amount-swayX)*blend;
        swayY+=(-(float)Math.abs(Math.cos(step))*.0013f*amount-swayY)*blend;
        swayPitch+=((float)Math.abs(Math.cos(step-.2f))*.35f*amount-swayPitch)*blend;
        swayRoll+=((float)Math.sin(step)*.28f*amount-swayRoll)*blend;
        poses.translate(.065*side+swayX,-.105+swayY,-.15);
        poses.mulPose(Axis.XP.rotationDegrees(-92+swayPitch));
        poses.mulPose(Axis.ZP.rotationDegrees(-6*side+swayRoll));
    }
    public static void clear(){side=1;lastTime=-1;swayX=swayY=swayPitch=swayRoll=0;}
}
