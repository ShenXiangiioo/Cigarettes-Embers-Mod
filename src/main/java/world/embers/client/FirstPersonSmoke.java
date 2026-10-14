package world.embers.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import world.embers.PlayerState;
import world.embers.Brand;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Smoke inherits momentum on emission, then remains in the world as the smoker moves. */
public final class FirstPersonSmoke {
    private static final List<Puff> smoke=new ArrayList<>();
    private static final Random random=new Random(7183);
    private static double lastTime=-1,debt;
    private static Object level;
    private static Vec3 lastCamera;
    private static float worldProjection=1,projectionScale=1;
    private static int sceneLight;
    private static Matrix4f heldOrigin;
    public static void heldOrigin(Matrix4f pose){heldOrigin=new Matrix4f(pose);}
    private static class Puff {
        Vec3 pos,velocity;float age,life,size,opacity,phase,stretch,expansion=1;boolean breath;int tile;
        Puff(Vec3 p,Vec3 v,boolean exhale,float strength,Brand brand){
            pos=p;velocity=v;breath=exhale;tile=random.nextInt(8);
            life=exhale?36+strength*22+random.nextFloat()*10:40+random.nextFloat()*14;
            size=exhale?.010f+strength*.017f:.005f+random.nextFloat()*.003f;
            opacity=exhale?.23f+strength*.24f:.28f+random.nextFloat()*.15f;
            expansion=brand.classic()?1.22f:brand.horizontalCase()?1.08f:1;size*=expansion;opacity*=brand.classic()?1.40f:1.10f;
            phase=random.nextFloat()*6.283f;stretch=.72f+random.nextFloat()*.65f;
        }
    }
    public static void clear(){smoke.clear();lastTime=-1;debt=0;lastCamera=null;heldOrigin=null;}
    public static void cancel(){} // Canceling an action cannot remove air that was already exhaled.
    public static int count(){return smoke.size();}
    public static int light(){return sceneLight;}
    private static Vec3 world(Vector3f v,Quaternionf q,Vec3 origin){Vector3f a=new Vector3f(-v.x*projectionScale,v.y*projectionScale,-v.z).rotate(q);return origin.add(a.x,a.y,a.z);}
    public static void render(PoseStack mouth,MultiBufferSource buffers,PlayerState state,float length,float partial,int light){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||!mc.options.getCameraType().isFirstPerson()){clear();return;}
        var camera=mc.gameRenderer.getMainCamera();Vec3 eye=camera.getPosition();
        if(level!=mc.level||lastCamera!=null&&eye.distanceToSqr(lastCamera)>4){clear();level=mc.level;}lastCamera=eye;
        double time=mc.player.tickCount+partial,dt=lastTime<0?0:Math.max(0,Math.min(1,time-lastTime));lastTime=time;
        projectionScale=RenderSystem.getProjectionMatrix().m11()/worldProjection;
        Quaternionf rotation=camera.rotation();
        boolean emitting=state.lit&&(state.mouthVisible()||heldOrigin!=null);
        Vector3f coal=(heldOrigin==null?mouth.last().pose():heldOrigin).transformPosition(new Vector3f(0,length,0));heldOrigin=null;
        Vec3 coalWorld=world(coal,rotation,eye);
        // The animated mouth transform drives the mesh and both emitters, including left-side smoking.
        Vec3 mouthWorld=world(mouth.last().pose().transformPosition(new Vector3f(0,.014f,0)),rotation,eye);
        Vec3 inherited=new Vec3(mc.player.getX()-mc.player.xo,mc.player.getY()-mc.player.yo,mc.player.getZ()-mc.player.zo).scale(.28);
        debt+=dt;
        while(debt>=.40){
            debt-=.40;
            if(emitting)smoke.add(new Puff(coalWorld.add((random.nextFloat()-.5)*.003,0,(random.nextFloat()-.5)*.003),new Vec3(0,.0045,0).add(inherited),false,0,Brand.byId(state.mouth)));
            if(state.exhaleTicks>0){
                float strength=state.exhaleStrength,tail=Math.min(1,state.exhaleTicks/8f);
                for(int i=0;i<1+(int)(strength*2.5f);i++){
                    Vector3f speed=new Vector3f((random.nextFloat()-.5f)*(.008f+strength*.010f),(random.nextFloat()-.5f)*.009f,.027f+strength*.038f).rotate(rotation);
                    Puff puff=new Puff(mouthWorld,new Vec3(speed.x,speed.y,speed.z).add(inherited),true,strength,Brand.byId(state.mouth));puff.opacity*=tail;smoke.add(puff);
                }
            }
        }
        for(var it=smoke.iterator();it.hasNext();){Puff f=it.next();f.age+=dt;if(f.age>=f.life||f.pos.distanceToSqr(eye)>20){it.remove();continue;}
            // Evolving eddies vary with position and age, with independent small variation per puff.
            double curl=(f.breath?.00025:.00031)*dt,eddy=time*.08+f.pos.y*9;
            f.velocity=f.velocity.scale(Math.pow(f.breath?.958:.966,dt)).add((Math.sin(eddy+f.pos.z*6)+.32*Math.sin(f.phase+f.age*.19))*curl,.00022*dt,(Math.cos(eddy*.83+f.pos.x*7)+.25*Math.cos(f.phase+f.age*.16))*curl);
            Vec3 next=f.pos.add(f.velocity.scale(dt));BlockPos at=BlockPos.containing(next);
            if(!mc.level.getBlockState(at).getCollisionShape(mc.level,at).isEmpty()){it.remove();continue;}f.pos=next;
        }
        while(smoke.size()>480)smoke.remove(0);
        sceneLight=LevelRenderer.getLightColor(mc.level,BlockPos.containing(eye));
    }
    public static void draw(net.minecraftforge.client.event.RenderLevelStageEvent event){
        if(event.getStage()!=net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        worldProjection=event.getProjectionMatrix().m11();
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||!mc.options.getCameraType().isFirstPerson()||smoke.isEmpty())return;
        Vec3 eye=event.getCamera().getPosition();Quaternionf inverse=new Quaternionf(event.getCamera().rotation()).conjugate();
        PoseStack p=new PoseStack();var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(SmokeRenderType.CLOUD);
        smoke.sort(Comparator.comparingDouble((Puff f)->f.pos.distanceToSqr(eye)).reversed());
        for(Puff f:smoke){
            Vector3f v=new Vector3f((float)(f.pos.x-eye.x),(float)(f.pos.y-eye.y),(float)(f.pos.z-eye.z)).rotate(inverse).mul(-1,1,-1);
            if(v.z>-.10f)continue;
            float age=f.age/f.life,fade=1-age,alpha=f.opacity*fade*fade*Math.min(1,(-v.z-.10f)/.055f);
            float radius=1.16f*(f.size+(f.breath?.0018f:.00065f)*f.age*f.expansion);
            float cs=(float)Math.cos(f.phase+f.age*.008f),sn=(float)Math.sin(f.phase+f.age*.008f);
            float[][] corners={{-1,-1,0,1},{1,-1,1,1},{1,1,1,0},{-1,1,0,0}};
            for(float[] c:corners){float x=c[0]*radius,y=c[1]*radius*f.stretch;float u=(f.tile%4+c[2])/4,vv=(f.tile/4+c[3])/2;
                out.vertex(p.last().pose(),v.x+x*cs-y*sn,v.y+x*sn+y*cs,v.z).uv(u,vv).color(.83f,.85f,.86f,alpha).uv2(sceneLight).endVertex();}
        }
        buffers.endBatch(SmokeRenderType.CLOUD);
    }
}

