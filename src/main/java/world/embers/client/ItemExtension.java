package world.embers.client;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import world.embers.*;
public class ItemExtension implements IClientItemExtensions {
    private BlockEntityWithoutLevelRenderer renderer;
    @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new Renderer();return renderer;}
    private static class Renderer extends BlockEntityWithoutLevelRenderer {
        Renderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack s,ItemDisplayContext c,PoseStack p,MultiBufferSource b,int light,int overlay){if((c==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND||c==ItemDisplayContext.THIRD_PERSON_LEFT_HAND)&&ClientEvents.renderingState!=null&&ClientEvents.customArms(ClientEvents.renderingState))return;if(s.getItem() instanceof AshtrayItem ai){p.pushPose();p.translate(.5,.19,.5);AshtrayVisuals.render(p,b,((AshtrayBlock)ai.getBlock()).style,light);p.popPose();return;}p.pushPose();p.translate(.5,c==ItemDisplayContext.GUI?.10:.28,.5);float scale=c==ItemDisplayContext.GUI?(s.getItem() instanceof LighterItem?8:s.getItem() instanceof SmokingItem item&&item.pack?5:5.5f):c==ItemDisplayContext.GROUND?(s.getItem() instanceof ButtItem?3.2f:1.5f):1;p.scale(scale,scale,scale);
            if(s.getItem() instanceof SmokingItem i){if(i.pack)Visuals.pack(p,b,i.brand,SmokingItem.remaining(s),s.hasTag()&&s.getTag().getBoolean("opened")?.08f:0,light);else Visuals.cigarette(p,b,i.brand,SmokingItem.burnFraction(s),false,false,light);}
            else if(s.getItem() instanceof LighterItem i){Visuals.lighter(p,b,i.kind,0,false,0,light);}
            else if(s.getItem() instanceof ButtItem){if(c==ItemDisplayContext.GUI){p.translate(0,.03,0);p.scale(3.5f,3.5f,3.5f);p.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-35));}Visuals.butt(p,b,ButtItem.brand(s),light);}p.popPose();}
    }
}
