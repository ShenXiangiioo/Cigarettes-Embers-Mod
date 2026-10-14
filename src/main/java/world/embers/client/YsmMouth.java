package world.embers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import world.embers.*;
import java.lang.reflect.Method;
import java.util.List;

/** Uses YSM's evaluated head hierarchy, preserving its own model scale and head animation. */
public final class YsmMouth {
    private static Method entity,model,head,apply;
    private static boolean checked,available;
    private static boolean initialize(){
        if(checked)return available;checked=true;
        String version=ModList.get().getModContainerById("yes_steve_model").map(c->c.getModInfo().getVersion().toString()).orElse("");
        if(!version.equals("2.6.5-forge+mc1.20.1"))return false;
        try {
            Class<?> actor=Class.forName("com.elfmcys.yesstevemodel.oo0OooOO0oOoOoOoo00oO000");
            entity=actor.getMethod("OO00OOOOo0Ooo0oo0o0Oo0OO");model=actor.getMethod("OOOoOO000000o0o0oOooo0o0");
            head=Class.forName("com.elfmcys.yesstevemodel.OOOO0O0O000O000000oOOO0o").getMethod("oOOOooO00oo0oOooooOO0Oo0");
            apply=Class.forName("com.elfmcys.yesstevemodel.o000OO00oooOo00000ooo0Oo").getMethod("Oo0Oo0o00O00Oo0OOoOOoooo",PoseStack.class,List.class);
            available=true;
        }catch(ReflectiveOperationException ex){com.mojang.logging.LogUtils.getLogger().warn("Cigarettes: YSM mouth attachment unavailable",ex);}
        return available;
    }
    public static void render(PoseStack p,MultiBufferSource b,int light,Object actor){
        if(!initialize())return;
        try {
            if(!(entity.invoke(actor) instanceof Player player)||player.isInvisible())return;
            PlayerState s=ClientState.get(player);if(!s.mouthVisible())return;
            Object m=model.invoke(actor);if(m==null)return;
            var bones=(List<?>)head.invoke(m);if(bones.isEmpty())return;
            p.pushPose();
            try {
                apply.invoke(null,p,bones);
                p.translate(.047*MouthPose.sideFor(player),.016,-.216);p.scale(1.45f,1.30f,1.45f);p.mulPose(Axis.YP.rotationDegrees(22*MouthPose.sideFor(player)));p.mulPose(Axis.XP.rotationDegrees(-101));
                Visuals.cigarette(p,b,Brand.byId(s.mouth),s.remaining(),s.lit,ClientState.heat(s,net.minecraft.client.Minecraft.getInstance().getFrameTime()),light);
            }finally{p.popPose();}
        }catch(ReflectiveOperationException ex){available=false;com.mojang.logging.LogUtils.getLogger().warn("Cigarettes: could not attach to YSM head",ex);}
    }
}
