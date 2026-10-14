package world.embers.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;

/** No vanilla billboard shadow, levitation, bobbing or perpetual item rotation. */
public final class ButtRenderer extends EntityRenderer<ItemEntity> {
    public ButtRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.035f;shadowStrength=.4f;}
    // ItemPickupParticle copies an ItemEntity into the base class even for a custom entity type.
    @Override public ResourceLocation getTextureLocation(ItemEntity e){return Visuals.WHITE;}
    @Override public void render(ItemEntity e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
        p.pushPose();p.translate(0,.016,0);p.mulPose(Axis.YP.rotationDegrees((e.getUUID().hashCode()&65535)%360));
        float spin=e.onGround()?0:(e.tickCount+partial)*24;
        p.mulPose(Axis.XP.rotationDegrees(90+spin));p.scale(3.2f,3.2f,3.2f);p.translate(0,-.014,0);Visuals.butt(p,b,world.embers.ButtItem.brand(e.getItem()),light);p.popPose();
        super.render(e,yaw,partial,p,b,light);
    }
}
