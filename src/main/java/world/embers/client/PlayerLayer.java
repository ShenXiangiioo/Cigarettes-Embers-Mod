package world.embers.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;
import world.embers.*;

public class PlayerLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    public PlayerLayer(PlayerRenderer r){super(r);}
    private static void drawArm(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,ModelPart arm,ModelPart sleeve,float x,float y,float z,int light){
        var skin=b.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
        for(ModelPart part:new ModelPart[]{arm,sleeve}){
            float xr=part.xRot,yr=part.yRot,zr=part.zRot;boolean visible=part.visible;
            part.xRot=x;part.yRot=y;part.zRot=z;part.visible=true;part.render(p,skin,light,OverlayTexture.NO_OVERLAY);
            part.xRot=xr;part.yRot=yr;part.zRot=zr;part.visible=visible;
        }
    }
    private static void attach(PoseStack p,ModelPart arm,float x,float y,float z){
        float ox=arm.xRot,oy=arm.yRot,oz=arm.zRot;arm.xRot=x;arm.yRot=y;arm.zRot=z;
        p.pushPose();arm.translateAndRotate(p);p.translate(-.055,.58,-.02);arm.xRot=ox;arm.yRot=oy;arm.zRot=oz;
    }
    @Override public void render(PoseStack p,MultiBufferSource b,int light,AbstractClientPlayer player,float walk,float amount,float partial,float age,float yaw,float pitch){
        if(player.isInvisible())return;
        PlayerState s=ClientState.get(player);float t=(s.actionTicks+partial)*(s.action.equals("offer")&&s.actionDuration==8?3.6f:1);var model=getParentModel();
        // Head coordinates run upward into negative Y. The previous positive Y placed the cigarette below the chin.
        if(s.mouthVisible()){p.pushPose();model.head.translateAndRotate(p);p.translate(.064*MouthPose.sideFor(player),-.094,-.257);p.scale(1.45f,1.30f,1.45f);p.mulPose(Axis.YP.rotationDegrees(22*MouthPose.sideFor(player)));p.mulPose(Axis.XP.rotationDegrees(-82));p.mulPose(Axis.ZP.rotationDegrees(5));Visuals.cigarette(p,b,Brand.byId(s.mouth),s.remaining(),s.lit,ClientState.heat(s,partial),light);p.popPose();}
        if(s.supportsCigarette()){
            float touch=Hands.ease(t/7);
            drawArm(p,b,player,model.rightArm,model.rightSleeve,-1.9f*touch,-.50f*touch,-.025f,light);
            drawArm(p,b,player,model.leftArm,model.leftSleeve,model.leftArm.xRot,model.leftArm.yRot,model.leftArm.zRot,light);return;
        }
        if(s.action.equals("offer")||s.action.equals("give")){
            float reach=Hands.ease((t-12)/12),lx=-1.1f-reach*.4f,ly=.85f*(1-reach);
            drawArm(p,b,player,model.rightArm,model.rightSleeve,-1,-.12f,-.025f,light);
            drawArm(p,b,player,model.leftArm,model.leftSleeve,lx,ly,.06f,light);
            var item=s.actionItem.isEmpty()?player.getMainHandItem():s.actionItem;
            if(item.getItem() instanceof SmokingItem pack){
                attach(p,model.rightArm,-1,-.12f,-.025f);p.mulPose(Axis.XP.rotationDegrees(180));
                Visuals.pack(p,b,pack.brand,Math.max(0,SmokingItem.remaining(item)-(t>=12?1:0)),Hands.ease(t/8),light);p.popPose();
                if(t>=12&&s.action.equals("offer")){attach(p,model.leftArm,lx,ly,.06f);p.mulPose(Axis.XP.rotationDegrees(175));Visuals.cigarette(p,b,pack.brand,1,false,false,light);p.popPose();}
            }return;
        }
        if(s.disposal()){
            boolean tray=s.action.equals("tray_out");float f=Hands.ease((t-6)/10),recover=Hands.ease((t-22)/10);
            float rx=(-1.9f+f*(tray?1.15f:.65f))*(1-recover),ry=-.4f*(1-f),rz=-.025f;
            drawArm(p,b,player,model.rightArm,model.rightSleeve,rx,ry,rz,light);drawArm(p,b,player,model.leftArm,model.leftSleeve,.05f,0,0,light);
            if(t>=6&&t<(tray?18:15)&&s.actionItem.getItem() instanceof SmokingItem item){
                attach(p,model.rightArm,rx,ry,rz);p.mulPose(Axis.XP.rotationDegrees(tray?35:170));
                Visuals.cigarette(p,b,item.brand,SmokingItem.burnFraction(s.actionItem),s.actionItem.getTag().getBoolean("lit"),false,light);p.popPose();
            }return;
        }
        if(s.action.equals("stomp")){
            float step=(float)Math.sin(Math.PI*Math.min(1,t/Math.max(1,s.actionDuration)*1.35));
            drawArm(p,b,player,model.rightLeg,model.rightPants,-step*.65f,0,0,light);
        }
        if(!ClientEvents.customArms(s))return;
        boolean lighting=s.action.equals("lighting"),offer=s.action.equals("offer"),fidget=s.action.equals("fidget"),share=s.action.startsWith("share");
        boolean lightAction=lighting||fidget||share||s.action.equals("lighter_ready")||s.action.equals("lighter_burn");
        float rx=lighting?-1.85f:s.action.equals("inhale")?-1.9f:offer||share?-1.40f:-1.0f;
        float ry=lighting||s.action.equals("inhale")?-.50f:-.12f;
        float rz=-.025f;
        drawArm(p,b,player,model.rightArm,model.rightSleeve,rx,ry,rz,light);
        drawArm(p,b,player,model.leftArm,model.leftSleeve,s.action.equals("prepare")?-1.35f:.06f,s.action.equals("prepare")?.45f:0,.025f,light);
        ModelPart arm=model.rightArm;float oldX=arm.xRot,oldY=arm.yRot,oldZ=arm.zRot;
        arm.xRot=rx;arm.yRot=ry;arm.zRot=rz;
        Vector3f up=p.last().normal().transform(new Vector3f(0,-1,0)).normalize();
        p.pushPose();arm.translateAndRotate(p);p.translate(-.055,.58,-.02);
        if(offer){
            if(t>=10){var it=s.actionItem.isEmpty()?player.getMainHandItem():s.actionItem;if(it.getItem() instanceof SmokingItem item)Visuals.cigarette(p,b,item.brand,1,false,false,light);}
        }else{
            p.mulPose(Axis.XP.rotationDegrees(180));
            if(lightAction){
                if(fidget)FidgetMotion.apply(p,LighterKind.byId(s.lighter),t,s.variant);
                float open=s.action.equals("lighter_burn")?1:s.action.equals("lighter_ready")?Hands.ease(t/5):Hands.ease(t/5)*(1-Hands.ease((t-s.actionDuration+6)/6));
                Visuals.lighter(p,b,LighterKind.byId(s.lighter),open,DynamicLighting.flame(s)&&!player.isUnderWater(),age,light,up);
            }else if(s.action.equals("stomp")||s.action.equals("tray_out")){Visuals.butt(p,b,s.actionItem.getItem() instanceof SmokingItem old?old.brand:Brand.CHAPMAN,light);}
            else if(!s.action.equals("inhale")){
                var it=s.actionItem.isEmpty()?player.getMainHandItem():s.actionItem;
                if(it.getItem() instanceof SmokingItem item){if(item.pack)Visuals.pack(p,b,item.brand,SmokingItem.remaining(it),Hands.ease((t-8)/8),light);else if(s.mouth<0)Visuals.cigarette(p,b,item.brand,1,false,false,light);}
            }
        }
        p.popPose();arm.xRot=oldX;arm.yRot=oldY;arm.zRot=oldZ;
    }
}
