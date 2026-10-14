package world.embers.client;
import world.embers.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Embers.ID,value=Dist.CLIENT)
public final class MobCigaretteLayer<T extends LivingEntity,M extends EntityModel<T>> extends RenderLayer<T,M> {
    private record Anchor(net.minecraft.world.phys.Vec3 coal,net.minecraft.world.phys.Vec3 mouth,long tick){}
    private record Origin(org.joml.Matrix4f inverse,net.minecraft.world.phys.Vec3 position){}
    private static final java.util.Map<java.util.UUID,Anchor> ANCHORS=new java.util.HashMap<>();
    private static final java.util.Map<Entity,Origin> ORIGINS=new java.util.IdentityHashMap<>();
    private static Object lastLevel;
    public static String anchorDebug(Entity e){Anchor a=ANCHORS.get(e.getUUID());return a==null?"none":a.coal.toString();}
    public MobCigaretteLayer(LivingEntityRenderer<T,M> renderer){super(renderer);}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void before(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> event){
        var e=event.getEntity();if(MobSmoking.visible(e))ORIGINS.put(e,new Origin(new org.joml.Matrix4f(event.getPoseStack().last().pose()).invert(),e.getPosition(event.getPartialTick())));
    }
    @SubscribeEvent public static void after(net.minecraftforge.client.event.RenderLivingEvent.Post<?,?> event){ORIGINS.remove(event.getEntity());}
    public static void accept(Network.MobState packet){var level=Minecraft.getInstance().level;if(level==null||packet.data()==null)return;Entity e=level.getEntity(packet.entity());if(e!=null&&e.getUUID().equals(packet.uuid()))e.getPersistentData().put("embers_smoking",packet.data());}
    @Override public void render(PoseStack p,MultiBufferSource b,int light,T e,float walk,float amount,float partial,float age,float yaw,float pitch){if(!MobSmoking.visible(e)||e.isInvisible())return;M model=getParentModel();ModelPart head=model instanceof HeadedModel h?h.getHead():model instanceof CreeperModel<?> c?c.root().getChild("head"):null;if(head==null)return;
        p.pushPose();if(model.young&&model instanceof HumanoidModel<?>){p.scale(.75f,.75f,.75f);p.translate(0,1,0);}head.translateAndRotate(p);
        p.translate(e instanceof Villager?.068:.061,e instanceof Villager?.012:-.10,-.263);
        p.scale(1.75f,1.65f,1.75f);p.mulPose(Axis.YP.rotationDegrees(12));p.mulPose(Axis.XP.rotationDegrees(-86));
        Brand brand=Brand.byId(MobSmoking.data(e).getInt("brand"));float fraction=MobSmoking.lit(e)?MobSmoking.remaining(e)/(float)Math.max(1,MobSmoking.data(e).getInt("max")):1;
        float cycle=(e.tickCount+partial)%120,heat=cycle>20&&cycle<50?(float)Math.sin((cycle-20)/30*Math.PI):0;
        Visuals.cigarette(p,b,brand,fraction,MobSmoking.lit(e),heat,light);
        Origin origin=ORIGINS.get(e);
        if(origin!=null)ANCHORS.put(e.getUUID(),new Anchor(worldPoint(origin,p,brand.cigaretteLength(fraction)),worldPoint(origin,p,.004f),e.level().getGameTime()));p.popPose();
    }
    // Remove the actual renderer entry matrix, including shader view transforms, before adding world position.
    private static net.minecraft.world.phys.Vec3 worldPoint(Origin origin,PoseStack p,float length){var point=p.last().pose().transformPosition(new org.joml.Vector3f(0,length,0));origin.inverse.transformPosition(point);return origin.position.add(point.x,point.y,point.z);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var m=Minecraft.getInstance();
        if(lastLevel!=m.level){ANCHORS.clear();ORIGINS.clear();lastLevel=m.level;}
        if(m.level==null||m.player==null)return;long now=m.level.getGameTime();ANCHORS.values().removeIf(a->now-a.tick>3);
        for(Entity e:m.level.entitiesForRendering())if(e instanceof Mob mob&&MobSmoking.lit(mob)&&mob.isAlive()&&mob.distanceToSqr(m.player)<1024){
            Anchor anchor=ANCHORS.get(mob.getUUID());if(anchor==null)continue;var look=mob.getLookAngle();Brand brand=Brand.byId(MobSmoking.data(mob).getInt("brand"));
            if(mob.tickCount%2==0){var at=anchor.coal;var smoke=m.particleEngine.createParticle(Embers.SMOKE.get(),at.x,at.y,at.z,look.x*.003,.009,look.z*.003);if(smoke instanceof SmokeParticle puff)puff.mob(brand);}
            int cycle=mob.tickCount%120;
            if(cycle>=52&&cycle<68){var at=anchor.mouth;var smoke=m.particleEngine.createParticle(Embers.EXHALE.get(),at.x,at.y,at.z,look.x*.035,.009+look.y*.02,look.z*.035);if(smoke instanceof SmokeParticle puff)puff.style(brand);}
        }
    }
}
