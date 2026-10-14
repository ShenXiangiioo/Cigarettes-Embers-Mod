package world.embers.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import world.embers.AshtrayEntity;
import java.util.Random;
public class AshtrayRenderer implements BlockEntityRenderer<AshtrayEntity> {
    public AshtrayRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(AshtrayEntity be,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){int style=((world.embers.AshtrayBlock)be.getBlockState().getBlock()).style;if(style>0){p.pushPose();p.translate(.5,0,.5);AshtrayVisuals.render(p,b,style,light);p.popPose();}for(int i=0;i<Math.min(AshtrayEntity.CAPACITY,be.count);i++){var pose=be.buttPose(i);p.pushPose();p.translate(pose.x(),pose.y(),pose.z());p.mulPose(Axis.YP.rotationDegrees(pose.yaw()));p.mulPose(Axis.XP.rotationDegrees(pose.tilt()));p.scale(pose.scale(),pose.scale(),pose.scale());p.translate(0,-.015,0);Visuals.butt(p,b,be.brand(i),light);p.popPose();}}
}
