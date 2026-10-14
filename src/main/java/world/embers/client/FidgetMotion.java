package world.embers.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import world.embers.LighterKind;

/** Each mechanism gets a different palm roll / ballistic flourish. */
public final class FidgetMotion {
    public static void apply(PoseStack p,LighterKind kind,float t,int variant){
        if(t<10||t>31)return;float u=Math.max(0,Math.min(1,(t-12)/15f)),arc=4*u*(1-u),settle=1-Hands.ease((t-26)/5);
        if(variant==1){
            p.translate(.028*Math.sin(u*Math.PI),arc*(kind.lid?.065:.10)*settle,-arc*.018);
            p.translate(0,.037,0);p.mulPose(Axis.ZP.rotationDegrees(u*360));p.mulPose(Axis.YP.rotationDegrees((float)Math.sin(u*Math.PI)*35));p.translate(0,-.037,0);return;
        }
        if(variant==2){
            float inspect=(float)Math.sin(u*Math.PI);
            p.translate(-.016*inspect,.022*inspect,-.012*inspect);p.translate(0,.037,0);
            p.mulPose(Axis.YP.rotationDegrees(-inspect*(kind.lid?115:75)));p.mulPose(Axis.XP.rotationDegrees(inspect*24));p.translate(0,-.037,0);return;
        }
        p.translate(0,(kind.lid?.09:kind.jet?.16:.025)*arc*settle,-arc*.02);
        p.translate(0,.037,0);
        switch(kind){
            case PIEZO -> p.mulPose(Axis.YP.rotationDegrees((float)Math.sin(u*Math.PI*2)*32));
            case WHEEL -> {p.mulPose(Axis.XP.rotationDegrees((float)Math.sin(u*Math.PI)*23));p.mulPose(Axis.YP.rotationDegrees(u*360));}
            case JET_BLACK -> p.mulPose(Axis.ZP.rotationDegrees(u*360));
            case JET_COPPER -> p.mulPose(Axis.XP.rotationDegrees(u*720));
            case JET_SILVER -> {p.mulPose(Axis.ZP.rotationDegrees(u*360));p.mulPose(Axis.YP.rotationDegrees(u*360));}
            case OIL_BRASS -> {p.mulPose(Axis.YP.rotationDegrees(u*360));p.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(u*Math.PI)*32));}
            case OIL_CHROME -> {p.mulPose(Axis.XP.rotationDegrees(u*360));p.mulPose(Axis.YP.rotationDegrees((float)Math.sin(u*Math.PI)*90));}
            case OIL_BLACK -> p.mulPose(Axis.ZP.rotationDegrees(u*720));
        }
        p.translate(0,-.037,0);
    }
}
