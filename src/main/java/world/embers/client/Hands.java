package world.embers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import world.embers.*;

/** Animates the player's original arm and sleeve; no replacement palms or fingers. */
public final class Hands {
    static float ease(float v){v=Math.max(0,Math.min(1,v));return v*v*(3-2*v);}
    private static float lerp(float a,float b,float f){return a+(b-a)*f;}
    public static void hand(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,boolean left,float grip,float spread,int light){
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if(!(renderer instanceof PlayerRenderer r))return;
        p.pushPose();p.scale(.62f,.78f,.62f);p.translate(left?-.375:.375,-.73,0);
        if(left)r.renderLeftHand(p,b,light,player);else r.renderRightHand(p,b,light,player);
        p.popPose();
    }
    private static void arm(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,boolean left,float x,float y,float z,float roll,int light){
        p.pushPose();p.translate(x,y,z);p.mulPose(Axis.ZP.rotationDegrees(roll));p.mulPose(Axis.XP.rotationDegrees(-9));hand(p,b,player,left,0,0,light);p.popPose();
    }
    public static void firstPerson(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,PlayerState s,float partial,int light){
        float t=(s.actionTicks+partial)*(s.action.equals("offer")&&s.actionDuration==8?3.6f:1),time=player.tickCount+partial;
        ItemStack item=player.getMainHandItem();
        boolean heldFlame=s.action.equals("lighter_burn")||s.action.equals("lighter_ready");
        boolean prepare=s.action.equals("prepare"),lighting=s.action.equals("lighting"),fidget=s.action.equals("fidget"),offer=s.action.equals("offer"),share=s.action.startsWith("share");
        float shake=s.stress>50?(s.stress-50)*.000023f:0;
        Vector3f up=new Vector3f(0,(float)Math.cos(Math.toRadians(player.getXRot())),(float)Math.sin(Math.toRadians(player.getXRot())));p.last().normal().transform(up).normalize();
        p.pushPose();
        if(lighting){p.last().pose().identity();p.last().normal().identity();up.set(0,(float)Math.cos(Math.toRadians(player.getXRot())),(float)Math.sin(Math.toRadians(player.getXRot())));}
        p.translate(Math.sin(time*2.3)*shake,Math.sin(time*1.7)*shake,0);
        if(s.disposal()){disposal(p,b,player,s,t,light);}
        else if(s.supportsCigarette()){
            // Occasionally support this entire puff, until release or the cough threshold.
            float touch=ease(t/7);
            if(touch>0){p.pushPose();p.last().pose().identity();p.last().normal().identity();
                arm(p,b,player,false,lerp(.23f,.090f,touch),lerp(-.48f,-.155f,touch),lerp(-.36f,-.21f,touch),lerp(-8,-24,touch),light);p.popPose();}
        }
        else if(lighting||share||item.getItem() instanceof LighterItem){
            LighterKind kind=item.getItem() instanceof LighterItem li&&!lighting?li.kind:LighterKind.byId(s.lighter);
            float reach=lighting?ease(t/7)*(1-ease((t-26)/4)):share?ease(t/10)*(1-ease((t-54)/6)):0;
            float x=lerp(.24f,.061f,reach),y=lerp(-.30f,-.235f,reach),z=lerp(-.49f,-.15f-Brand.byId(s.mouth).cigaretteLength(s.remaining()),reach);
            if(lighting){
                // Align the flame's hot core with the actual burning end, including brand length,
                // head pitch and each lighter's different nozzle. Both props share view coordinates.
                PoseStack bite=new PoseStack();MouthPose.apply(bite,partial);
                Vector3f target=bite.last().pose().transformPosition(new Vector3f(0,Brand.byId(s.mouth).cigaretteLength(s.remaining()),0));
                Quaternionf rotation=new Quaternionf().rotationY((float)Math.toRadians(-16)).rotateZ((float)Math.toRadians(-5));
                Vector3f nozzle=new Vector3f(kind.lid?-.008f:kind.jet?0:-.007f,kind.lid?.082f:kind.jet?.088f:.090f,0).mul(1.30f).rotate(rotation);
                Vector3f fireAxis=kind.jet?new Vector3f(0,1,0).rotate(rotation):new Vector3f(up);
                target.sub(nozzle).sub(fireAxis.mul(kind.jet?.021f:.025f));
                x=lerp(.24f,target.x,reach);y=lerp(-.30f,target.y,reach);z=lerp(-.49f,target.z,reach);
            }
            if(share){x=lerp(.24f,.14f,reach);y=lerp(-.30f,-.25f,reach);z=lerp(-.49f,-.82f,reach);}
            float catchDip=fidget&&s.variant>0&&t>=25&&t<32?-(float)Math.sin((t-25)/7*Math.PI)*.023f:0;
            arm(p,b,player,false,x+.012f,y+.009f+catchDip,z-.13f,-5,light);
            p.pushPose();p.translate(x,y,z);p.mulPose(Axis.YP.rotationDegrees(-16));p.mulPose(Axis.ZP.rotationDegrees(-5));
            if(fidget)FidgetMotion.apply(p,kind,t,s.variant);
            float opened=heldFlame?(s.action.equals("lighter_burn")?1:ease(t/5)):lighting?ease((t-1)/5)*(1-ease((t-26)/4)):fidget?ease(t/4)*(1-ease((t-s.actionDuration+6)/6)):share?ease(t/5):0;
            p.scale(1.30f,1.30f,1.30f);Visuals.lighter(p,b,kind,opened,DynamicLighting.flame(s)&&!player.isUnderWater(),time,light,up);p.popPose();
            if(lighting&&player.level().isRainingAt(player.blockPosition().above()))arm(p,b,player,true,-.10f,-.14f,-.40f,22,light);
        }else if(item.getItem() instanceof SmokingItem si&&si.pack&&si.brand.horizontalCase()&&(prepare||offer)){
            horizontalPack(p,b,player,s,si,item,t,light);
        }else if(item.getItem() instanceof SmokingItem si&&si.pack){
            uprightPack(p,b,player,s,si,item,t,light);
        }else if(item.getItem() instanceof SmokingItem si&&!s.action.equals("stomp")&&!s.action.equals("tray_out")){
            float lift=prepare?ease(t/8):0;
            float x=lerp(.23f,.065f,lift),y=lerp(-.26f,-.105f,lift),z=lerp(-.48f,-.18f,lift);
            arm(p,b,player,false,x+.018f,y-.025f,z-.14f,-10,light);
            p.pushPose();p.translate(x,y,z);p.mulPose(Axis.XP.rotationDegrees(-lift*92));p.mulPose(Axis.ZP.rotationDegrees(-28*(1-lift)));
            float remaining=SmokingItem.burnFraction(item);
            Visuals.cigarette(p,b,si.brand,remaining,false,false,light);p.popPose();
        }else if(item.getItem() instanceof ButtItem){
            arm(p,b,player,false,.235f,-.285f,-.58f,-10,light);
            p.pushPose();p.translate(.22,-.23,-.42);p.mulPose(Axis.ZP.rotationDegrees(-28));p.scale(1.8f,1.8f,1.8f);Visuals.butt(p,b,item.getItem() instanceof ButtItem?ButtItem.brand(item):s.actionItem.getItem() instanceof SmokingItem old?old.brand:Brand.CHAPMAN,light);p.popPose();
        }else if(s.action.equals("mouth_put")||s.action.equals("mouth_remove")){
            float lift=ease(t/5);arm(p,b,player,false,lerp(.23f,.075f,lift),lerp(-.30f,-.145f,lift),-.35f,-20,light);
        }else if(s.action.equals("stomp")||s.action.equals("tray_out")){
            float out=ease(t/Math.max(1,s.actionDuration));arm(p,b,player,false,.18f,-.26f-out*.30f,-.47f,-12,light);
            p.pushPose();p.translate(.15,-.24-out*.30,-.36);p.mulPose(Axis.ZP.rotationDegrees(-60));Visuals.butt(p,b,item.getItem() instanceof ButtItem?ButtItem.brand(item):s.actionItem.getItem() instanceof SmokingItem old?old.brand:Brand.CHAPMAN,light);p.popPose();
        }
        p.popPose();
    }
    private static void uprightPack(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,PlayerState s,SmokingItem item,ItemStack stack,float t,int light){
        boolean taking=s.action.equals("prepare")||s.action.equals("offer"),offer=s.action.equals("offer");
        float x=.235f,y=-.30f,z=-.48f,open=taking?ease((t-4)/10):0;
        float take=taking?ease((t-(offer?10:16))/(offer?17:14)):0;
        arm(p,b,player,false,x+.074f,y+.020f,z-.020f,12,light);
        p.pushPose();p.translate(x,y,z);p.mulPose(Axis.YP.rotationDegrees(8));p.mulPose(Axis.XP.rotationDegrees(-12));p.scale(1.30f,1.30f,1.30f);
        Visuals.pack(p,b,item.brand,Math.max(0,SmokingItem.remaining(stack)-(take>0?1:0)),open,light);p.popPose();
        if(!taking)return;
        int index=Math.max(0,SmokingItem.remaining(stack)-1);float w=item.brand.slim?.092f:.109f;
        Vector3f start=new Vector3f(-w/2+.014f+(4-index%5)*(w-.025f)/5,.146f,-.011f+(index/5)*.021f).mul(1.30f).rotateX((float)Math.toRadians(-12)).rotateY((float)Math.toRadians(8)).add(x,y,z);
        float clear=ease(take/.60f),travel=ease((take-.48f)/.52f);
        Vector3f point=new Vector3f(start).add(0,.155f*clear,.035f*clear).lerp(new Vector3f(offer?-.12f:.065f,offer?-.21f:-.105f,offer?-.77f:-.15f),travel);
        if(take>0){
            arm(p,b,player,true,point.x-.052f,point.y-.018f,point.z-.030f,lerp(-58,-18,travel),light);
            Quaternionf from=new Quaternionf().rotationY((float)Math.toRadians(8)).rotateX((float)Math.toRadians(168));
            Quaternionf to=new Quaternionf().rotationY((float)Math.toRadians(offer?-5:0)).rotateX((float)Math.toRadians(-92));
            p.pushPose();p.translate(point.x,point.y,point.z);p.mulPose(from.slerp(to,travel));Visuals.cigarette(p,b,item.brand,1,false,false,light);p.popPose();
        }else {
            float approach=ease(t/10);arm(p,b,player,true,lerp(-.24f,start.x-.055f,approach),lerp(-.38f,start.y-.018f,approach),lerp(-.35f,start.z-.030f,approach),-58,light);
        }
    }
    private static void horizontalPack(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,PlayerState s,SmokingItem item,ItemStack stack,float t,int light){
        boolean offer=s.action.equals("offer");float opening=ease((t-3)/10),take=ease((t-(offer?12:16))/(offer?14:14));
        float x=.245f,y=-.315f,z=-.49f;
        arm(p,b,player,false,x+.081f,y+.010f,z-.025f,12,light);
        int count=SmokingItem.remaining(stack),row=Math.max(0,count-1);
        p.pushPose();p.translate(x,y,z);p.mulPose(Axis.YP.rotationDegrees(8));p.mulPose(Axis.XP.rotationDegrees(-12));p.scale(1.30f,1.30f,1.30f);
        Visuals.pack(p,b,item.brand,Math.max(0,count-(take>0?1:0)),opening,light);p.popPose();
        Vector3f start=new Vector3f(-.077f,.0085f+row*.0092f,0).mul(1.30f).rotateX((float)Math.toRadians(-12)).rotateY((float)Math.toRadians(8)).add(x,y,z);
        if(take<=0){float approach=ease(t/11);arm(p,b,player,true,lerp(-.24f,start.x-.048f,approach),lerp(-.38f,start.y-.023f,approach),lerp(-.35f,start.z-.032f,approach),-58,light);return;}
        float clear=ease(take/.45f),travel=ease((take-.3f)/.7f);
        Vector3f point=new Vector3f(start).add(0,.016f*clear,.066f*clear).lerp(new Vector3f(offer?-.12f:.065f,offer?-.21f:-.105f,offer?-.77f:-.15f),travel);
        arm(p,b,player,true,point.x-.048f,point.y-.023f,point.z-.032f,lerp(-58,-18,travel),light);
        Quaternionf from=new Quaternionf().rotationY((float)Math.toRadians(8)).rotateX((float)Math.toRadians(-12)).rotateZ((float)Math.toRadians(-90));
        Quaternionf to=new Quaternionf().rotationY((float)Math.toRadians(offer?-5:0)).rotateX((float)Math.toRadians(-92));
        p.pushPose();p.translate(point.x,point.y,point.z);p.mulPose(from.slerp(to,travel));float scale=lerp(1.28f,1,travel);p.scale(scale,scale,scale);Visuals.cigarette(p,b,item.brand,1,false,false,light);p.popPose();
    }
    private static void disposal(PoseStack p,MultiBufferSource b,AbstractClientPlayer player,PlayerState s,float t,int light){
        p.pushPose();p.last().pose().identity();p.last().normal().identity();
        boolean tray=s.action.equals("tray_out");int release=tray?18:15;
        float take=ease(t/6),travel=ease((t-6)/9),recover=ease((t-(tray?23:16))/10);
        Vector3f at=new Vector3f(lerp(.23f,.065f,take),lerp(-.29f,-.105f,take),lerp(-.43f,-.15f,take));
        Vector3f target=new Vector3f(.18f,-.22f,-.52f);
        if(tray&&s.trayTarget!=null){
            var cam=Minecraft.getInstance().gameRenderer.getMainCamera();var delta=net.minecraft.world.phys.Vec3.atLowerCornerOf(s.trayTarget).add(.5,.09,.5).subtract(cam.getPosition());
            target.set((float)delta.x,(float)delta.y,(float)delta.z).rotate(new Quaternionf(cam.rotation()).conjugate()).mul(-1,1,-1);
            float length=Brand.byId(s.mouth).cigaretteLength(SmokingItem.burnFraction(s.actionItem));
            // Offset the filter from the contact point along the same rotated cigarette axis.
            Vector3f axis=new Vector3f(0,length,0).rotate(new Quaternionf().rotationX((float)Math.toRadians(178)).rotateZ((float)Math.toRadians(-12)));target.sub(axis);
        }
        if(t>=6)at.lerp(target,travel);
        if(!tray&&t>=8&&t<release)at.y+=.085f*(float)Math.sin((t-8)/7*Math.PI*.7);
        if(tray&&t>=15&&t<23)at.y-=.007f*(float)Math.sin((t-15)*Math.PI/2);
        at.lerp(new Vector3f(.24f,-.48f,-.4f),recover);
        // Place the square wrist behind the filter, with a small squeeze at contact.
        if(tray&&t>=6){
            Vector3f wrist=new Vector3f(at).add(.014f,-.026f,.015f),direction=new Vector3f(.34f,-.62f,-.14f).sub(wrist);float distance=direction.length();
            p.pushPose();p.translate(wrist.x,wrist.y,wrist.z);p.mulPose(new Quaternionf().rotationTo(new Vector3f(0,-1,0),direction.normalize()));p.scale(1,Math.max(1,distance/.58f),1);hand(p,b,player,false,0,0,light);p.popPose();
        }else arm(p,b,player,false,at.x+.017f,at.y-.050f,at.z-.12f,-12+travel*8,light);
        if(t>=6&&t<release&&s.actionItem.getItem() instanceof SmokingItem item){
            p.pushPose();p.translate(at.x,at.y,at.z);
            Quaternionf from=new Quaternionf().rotationX((float)Math.toRadians(-92)).rotateZ((float)Math.toRadians(-6));
            Quaternionf to=new Quaternionf().rotationX((float)Math.toRadians(tray?178:-40)).rotateZ((float)Math.toRadians(-12));p.mulPose(from.slerp(to,travel));
            float remaining=SmokingItem.burnFraction(s.actionItem);
            FirstPersonSmoke.heldOrigin(p.last().pose());
            Visuals.cigarette(p,b,item.brand,remaining,s.actionItem.getTag().getBoolean("lit"),false,light);p.popPose();
        }
        p.popPose();
    }
    public static void mouth(PoseStack p,MultiBufferSource b,PlayerState s,float t,int light){
        
        p.pushPose();p.last().pose().identity();p.last().normal().identity();
        MouthPose.apply(p,Minecraft.getInstance().getFrameTime());
        float remaining=s.remaining();
        if(s.mouthVisible())Visuals.cigarette(p,b,Brand.byId(s.mouth),remaining,s.lit,ClientState.heat(s,Minecraft.getInstance().getFrameTime()),light);
        FirstPersonSmoke.render(p,b,s,Brand.byId(s.mouth).cigaretteLength(remaining),Minecraft.getInstance().getFrameTime(),light);p.popPose();
    }
}
