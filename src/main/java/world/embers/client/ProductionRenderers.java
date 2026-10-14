package world.embers.client;

import world.embers.production.*;
import world.embers.Embers;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.network.chat.Component;
import java.util.*;

@Mod.EventBusSubscriber(modid=Embers.ID,value=Dist.CLIENT)
public final class ProductionRenderers {
    private static final Map<BlockPos,MachineSound> SOUNDS=new HashMap<>();
    private static void face(PoseStack p,MultiBufferSource b,ResourceLocation texture,float x,float y,float w,float h,float depth,int light){
        VertexConsumer v=b.getBuffer(RenderType.entityCutoutNoCull(texture));
        float[][] pts={{x,y,depth,0,0},{x,y-h,depth-.025f,0,1},{x+w,y-h,depth-.025f,1,1},{x+w,y,depth,1,0}};
        for(float[] a:pts)v.vertex(p.last().pose(),a[0],a[1],a[2]).color(255,255,255,255).uv(a[3],a[4]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),0,0,-1).endVertex();
    }
    private static void orient(PoseStack p,Direction facing){p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));p.translate(-.5,0,-.5);}
    public static final class Rack implements BlockEntityRenderer<DryingRackEntity> {
        public Rack(BlockEntityRendererProvider.Context c){}
        @Override public void render(DryingRackEntity e,float dt,PoseStack p,MultiBufferSource b,int light,int overlay){
            p.pushPose();orient(p,e.getBlockState().getValue(DryingRackBlock.FACING));
            for(int i=0;i<3;i++)if(e.tobacco[i]>=0){float dry=(float)e.progress[i];Tobacco tobacco=Tobacco.values()[e.tobacco[i]];String id=(dry>.65?"dried_":"")+tobacco.id+"_tobacco";ResourceLocation texture=Visuals.texture("item/"+id);
                p.pushPose();p.translate(.19+i*.31,1.37,.49);float time=e.getLevel()==null?0:e.getLevel().getGameTime()+dt;
                float phase=e.getBlockPos().getX()*.73f+e.getBlockPos().getZ()*.41f+i*1.7f;
                float gust=2.6f+(e.wet?4:2)*(1+(float)Math.sin(time*.013+phase))*.5f;
                p.mulPose(Axis.ZP.rotationDegrees((float)Math.sin(time*.047+phase)*gust));
                p.mulPose(Axis.XP.rotationDegrees((float)Math.sin(time*.033+phase+1)*gust*.6f));
                for(int j=0;j<3;j++){p.pushPose();p.mulPose(Axis.YP.rotationDegrees((j-1)*24));p.mulPose(Axis.ZP.rotationDegrees((j-1)*(10+dry*4)));float w=.24f*(1-dry*.15f),h=.58f*(1-dry*.18f);face(p,b,texture,-w/2,0,w,h,.008f*j,light);Visuals.box(p,b,-.006f,-.03f,0,.012f,.09f,.012f,0xA09164,light);p.popPose();}p.popPose();
            }p.popPose();
        }
        @Override public boolean shouldRenderOffScreen(DryingRackEntity e){return true;}
    }
    public static final class Machine implements BlockEntityRenderer<ProcessingEntity> {
        public Machine(BlockEntityRendererProvider.Context c){}
        @Override public void render(ProcessingEntity e,float dt,PoseStack p,MultiBufferSource b,int light,int overlay){
            float t=e.running&&e.work<e.total&&e.getLevel()!=null?e.getLevel().getGameTime()+dt:0;
            p.pushPose();orient(p,e.getBlockState().getValue(ProcessingBlock.FACING));
            // Horizontal cutter shafts, a ribbed drum, and a short packaging stroke.
            for(int i=0;i<2;i++){p.pushPose();p.translate(1.5,1.48+i*.16,.107);p.mulPose(Axis.XP.rotationDegrees(t*(i==0?12:-12)));Visuals.box(p,b,-.35f,-.045f,-.045f,.70f,.09f,.09f,0xABB0A2,light);for(int j=0;j<9;j++)Visuals.box(p,b,-.32f+j*.075f,-.057f,-.057f,.015f,.114f,.114f,0x657369,light);p.popPose();}
            p.pushPose();p.translate(1.5,.6,.112);p.mulPose(Axis.ZP.rotationDegrees(t*1.8f));for(int j=0;j<8;j++){p.pushPose();p.mulPose(Axis.ZP.rotationDegrees(j*45));Visuals.box(p,b,-.02f,.08f,-.015f,.04f,.12f,.03f,0x868A75,light);p.popPose();}p.popPose();
            float stroke=t==0?0:Math.max(0,(float)Math.sin(t*.08))*.07f;Visuals.box(p,b,2.23f,.56f,.035f-stroke,.53f,.035f,.16f,0xA8AC9B,light);
            p.popPose();
            if(e.running&&e.work<e.total&&!SOUNDS.containsKey(e.getBlockPos())){MachineSound sound=new MachineSound(e);SOUNDS.put(e.getBlockPos(),sound);Minecraft.getInstance().getSoundManager().play(sound);}
        }
        @Override public boolean shouldRenderOffScreen(ProcessingEntity e){return true;}
    }
    private static final class MachineSound extends AbstractTickableSoundInstance {
        final ProcessingEntity machine;
        MachineSound(ProcessingEntity e){super(SoundEvents.MINECART_INSIDE,SoundSource.BLOCKS,RandomSource.create());machine=e;x=e.getBlockPos().getX()+.5;y=e.getBlockPos().getY()+1;z=e.getBlockPos().getZ()+.5;looping=true;delay=0;volume=.20f;pitch=.68f;}
        @Override public void tick(){var m=Minecraft.getInstance();if(m.level==null||m.level!=machine.getLevel()||machine.isRemoved()||!machine.running||machine.work>=machine.total||!m.level.hasChunkAt(machine.getBlockPos()))stop();}
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;Minecraft m=Minecraft.getInstance();SOUNDS.entrySet().removeIf(e->e.getValue().isStopped());if(m.level==null)return;
        for(MachineSound sound:SOUNDS.values()){
            ProcessingEntity e=sound.machine;if(!e.running||e.work>=e.total)continue;long tick=m.level.getGameTime();var random=m.level.random;
            Direction facing=e.getBlockState().getValue(ProcessingBlock.FACING);
            // These two points share the NORTH-model coordinates of the cap and hatch.
            if(tick%2==0)for(int outlet=0;outlet<2;outlet++){
                Vec3 at=machinePoint(e,outlet==0?2.78125:.5,outlet==0?2.035:2.025,outlet==0?.65625:.5625);
                int count=outlet==0?2:3;
                for(int i=0;i<count;i++){
                    var smoke=m.particleEngine.createParticle(Embers.SMOKE.get(),at.x+(random.nextDouble()-.5)*.075,at.y,at.z+(random.nextDouble()-.5)*.075,
                        .009+(random.nextDouble()-.5)*.012,.024+random.nextDouble()*.022,.005+(random.nextDouble()-.5)*.01);
                    if(smoke instanceof SmokeParticle puff)puff.machine(outlet==1);
                }
            }
            if(tick%3==0){Vec3 at=machinePoint(e,1.5,1.44,.055);for(int i=0,n=2+random.nextInt(4);i<n;i++)m.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(Production.DRY.get(Tobacco.values()[random.nextInt(4)]).get())),at.x+(random.nextDouble()-.5)*.6,at.y+random.nextDouble()*.2,at.z,facing.getStepX()*(.035+random.nextDouble()*.06)+(random.nextDouble()-.5)*.025,.015+random.nextDouble()*.055,facing.getStepZ()*(.035+random.nextDouble()*.06)+(random.nextDouble()-.5)*.025);}
        }
    }
    public static Vec3 machinePoint(ProcessingEntity e,double x,double y,double z){Direction f=e.getBlockState().getValue(ProcessingBlock.FACING),right=f.getClockWise(),back=f.getOpposite();return Vec3.atLowerCornerOf(e.getBlockPos()).add(.5+right.getStepX()*(x-.5)+back.getStepX()*(z-.5),y,.5+right.getStepZ()*(x-.5)+back.getStepZ()*(z-.5));}
    @SubscribeEvent public static void hover(RenderGuiEvent.Post event){Minecraft m=Minecraft.getInstance();if(m.level==null||m.player==null||m.options.hideGui||m.screen!=null||!(m.hitResult instanceof BlockHitResult hit))return;var state=m.level.getBlockState(hit.getBlockPos());if(!state.is(Production.RACK.get()))return;BlockPos root=state.getValue(DryingRackBlock.UPPER)?hit.getBlockPos().below():hit.getBlockPos();if(!(m.level.getBlockEntity(root) instanceof DryingRackEntity rack))return;int slot=DryingRackBlock.slot(state,hit);if(rack.tobacco[slot]<0)return;Tobacco t=Tobacco.values()[rack.tobacco[slot]];int percent=(int)Math.floor(rack.progress[slot]*100+1e-6);Component text=rack.wet?Component.translatable("hud.embers.rack_wet",percent):Component.translatable("hud.embers.rack",Production.FRESH.get(t).get().getDescription(),percent);event.getGuiGraphics().drawCenteredString(m.font,text,m.getWindow().getGuiScaledWidth()/2,m.getWindow().getGuiScaledHeight()/2+17,0xDDD9C9);}
}
