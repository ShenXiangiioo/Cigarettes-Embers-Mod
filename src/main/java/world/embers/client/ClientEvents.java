package world.embers.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import world.embers.*;

@Mod.EventBusSubscriber(modid=Embers.ID,value=Dist.CLIENT)
public final class ClientEvents {
    public static final KeyMapping ACTION=new KeyMapping("key.embers.action",InputConstants.Type.MOUSE,GLFW.GLFW_MOUSE_BUTTON_RIGHT,"key.categories.embers");
    public static final KeyMapping HUD=new KeyMapping("key.embers.hud",GLFW.GLFW_KEY_H,"key.categories.embers");
    private static boolean down,engaged,showHud=true;private static int lastSlot=-1;
    static PlayerState renderingState;
    static boolean customArms(PlayerState s){return s.supportsCigarette()||java.util.Set.of("prepare","lighting","offer","give","receive","received","fidget","share_fire","lighter_ready","lighter_burn","share_done","tray_out","toss","stomp").contains(s.action);}
    private static boolean eligible(){Minecraft m=Minecraft.getInstance();if(m.player==null||m.screen!=null||m.player.isSpectator()||InteractionPriority.blockOrEntity())return false;ItemStack item=m.player.getMainHandItem();PlayerState s=ClientState.get(m.player);return item.getItem() instanceof SmokingItem||item.getItem() instanceof LighterItem||item.isEmpty()&&(s.mouth>=0||m.hitResult instanceof EntityHitResult h&&h.getEntity() instanceof net.minecraft.world.entity.player.Player p&&ClientState.get(p).action.equals("offer"));}
    private static void send(int type){Minecraft m=Minecraft.getInstance();if(m.player!=null)Network.CHANNEL.sendToServer(new Network.Input(type,m.player.isShiftKeyDown(),m.hitResult instanceof EntityHitResult h?h.getEntity().getId():-1));}
    @SubscribeEvent public static void interaction(InputEvent.InteractionKeyMappingTriggered e){if(e.isUseItem()&&ACTION.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(GLFW.GLFW_MOUSE_BUTTON_RIGHT))&&(eligible()||engaged||Minecraft.getInstance().player!=null&&!ClientState.get(Minecraft.getInstance().player).action.equals("idle"))){e.setCanceled(true);e.setSwingHand(false);}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;Minecraft m=Minecraft.getInstance();if(m.player==null||m.level==null){down=engaged=false;lastSlot=-1;return;}
        ClientState.tick();DynamicLighting.tick();ClientFoley.tick();if(!m.options.getCameraType().isFirstPerson())FirstPersonSmoke.clear();while(HUD.consumeClick())showHud=!showHud;
        PlayerState active=ClientState.get(m.player);
        if(java.util.Set.of("prepare","offer","fidget","share_fire","lighter_ready","lighter_burn").contains(active.action)&&!ItemStack.isSameItem(m.player.getMainHandItem(),active.actionItem))cancel();
        boolean pressed=ACTION.isDown()&&m.screen==null;
        boolean clicked=false;while(ACTION.consumeClick())clicked=true;
        if(m.screen!=null||lastSlot!=-1&&lastSlot!=m.player.getInventory().selected){if(engaged||!ClientState.get(m.player).action.equals("idle"))cancel();engaged=false;}
        if((pressed&&!down||clicked&&!down)&&!engaged&&(eligible()||!ClientState.get(m.player).action.equals("idle"))){if(!ClientState.get(m.player).action.equals("idle")){cancel();}else {send(0);engaged=true;}}
        if(!pressed&&(down||clicked)&&engaged){send(1);if(ClientState.get(m.player).action.equals("lighter_burn"))ClientState.cancel(m.player);engaged=false;}down=pressed;lastSlot=m.player.getInventory().selected;
        for(var p:m.level.players()){
            if(p==m.player&&m.options.getCameraType().isFirstPerson())continue;
            PlayerState s=ClientState.get(p);Vec3 look=p.getLookAngle();Vec3 mouth=p.getEyePosition().add(look.scale(.27)).add(0,-.11,0);
            if(s.mouthVisible()&&s.lit){
                double length=Brand.byId(s.mouth).cigaretteLength(s.remaining());
                Vec3 tip=mouth.add(look.scale(length));Vec3 motion=p.getDeltaMovement().scale(.22);
                if(p.tickCount%2==0)styledSmoke(m,Embers.SMOKE.get(),s,tip.x,tip.y,tip.z,motion.x*.3,.009+motion.y*.1,motion.z*.3);
            }
            if(s.exhaleTicks>0){
                int n=2+(int)(s.exhaleStrength*7);
                for(int j=0;j<n;j++)styledSmoke(m,Embers.EXHALE.get(),s,mouth.x+(p.getRandom().nextDouble()-.5)*.035,mouth.y+(p.getRandom().nextDouble()-.5)*.02,mouth.z+(p.getRandom().nextDouble()-.5)*.035,look.x*(.035+s.exhaleStrength*.06),look.y*(.035+s.exhaleStrength*.06)+.006,look.z*(.035+s.exhaleStrength*.06));
            }
        }
    }
    private static void styledSmoke(Minecraft m,net.minecraft.core.particles.SimpleParticleType type,PlayerState state,double x,double y,double z,double vx,double vy,double vz){var particle=m.particleEngine.createParticle(type,x,y,z,vx,vy,vz);if(particle instanceof SmokeParticle smoke)smoke.style(Brand.byId(state.mouth));}
    private static void cancel(){Minecraft mc=Minecraft.getInstance();if(mc.player!=null){send(2);ClientState.cancel(mc.player);}engaged=false;}
    @SubscribeEvent public static void smokeWorld(RenderLevelStageEvent e){FirstPersonSmoke.draw(e);}
    @SubscribeEvent public static void escape(InputEvent.Key e){if(e.getKey()==GLFW.GLFW_KEY_ESCAPE&&e.getAction()==GLFW.GLFW_PRESS){Minecraft mc=Minecraft.getInstance();if(mc.player!=null&&(!ClientState.get(mc.player).action.equals("idle")||engaged))cancel();}}
    @SubscribeEvent public static void discard(InputEvent.Key e){Minecraft mc=Minecraft.getInstance();if(e.getAction()!=GLFW.GLFW_PRESS||mc.screen!=null||mc.player==null||!mc.options.keyDrop.matches(e.getKey(),e.getScanCode()))return;if(!ClientState.get(mc.player).action.equals("idle"))cancel();if(mc.player.getMainHandItem().isEmpty()&&ClientState.get(mc.player).mouth>=0){send(3);}}
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){
        Minecraft m=Minecraft.getInstance();if(m.player==null||!m.options.getCameraType().isFirstPerson()||!m.options.bobView().get())return;
        PlayerState s=ClientState.get(m.player);float t=s.actionTicks+(float)e.getPartialTick(),pitch=0,roll=0;
        if(s.action.equals("cough")){float envelope=(1-Hands.ease(t/25));pitch=(float)Math.sin(t*1.15)*.65f*envelope;roll=(float)Math.sin(t*.8)*.16f*envelope;}
        else if(s.action.equals("prepare")||s.action.equals("offer")){float at=s.action.equals("offer")?12:18;float d=t-at;if(d>=0&&d<6){pitch=(float)Math.sin(d*Math.PI/3)*.19f*(1-d/6);roll=pitch*.22f;}}
        else if(s.disposal()){float d=t-(s.action.equals("tray_out")?18:15);if(d>=0&&d<5)pitch=(float)Math.sin(d*Math.PI/2.5)*.15f*(1-d/5);}
        e.setPitch(e.getPitch()+pitch);e.setRoll(e.getRoll()+roll);
    }
    @SubscribeEvent public static void input(MovementInputUpdateEvent e){if(ClientState.get(e.getEntity()).reverseTicks>0){e.getInput().forwardImpulse=-e.getInput().forwardImpulse;e.getInput().leftImpulse=-e.getInput().leftImpulse;}}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST) public static void hand(RenderHandEvent e){Minecraft m=Minecraft.getInstance();if(m.player==null)return;PlayerState s=ClientState.get(m.player);ItemStack i=m.player.getMainHandItem();boolean custom=i.getItem() instanceof SmokingItem||i.getItem() instanceof LighterItem||i.getItem() instanceof ButtItem||i.isEmpty()&&(s.mouth>=0||!s.action.equals("idle"));if(!custom)return;if(e.getHand()==InteractionHand.OFF_HAND){if(customArms(s))e.setCanceled(true);return;}e.setCanceled(true);Hands.firstPerson(e.getPoseStack(),e.getMultiBufferSource(),m.player,s,e.getPartialTick(),e.getPackedLight());}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
    public static void mouthAfterHand(RenderHandEvent e){
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||e.getHand()!=InteractionHand.MAIN_HAND||!mc.options.getCameraType().isFirstPerson()||mc.player.isSpectator())return;
        var state=ClientState.get(mc.player);Hands.mouth(e.getPoseStack(),e.getMultiBufferSource(),state,state.actionTicks+e.getPartialTick(),e.getPackedLight());
    }
    @SubscribeEvent public static void playerPre(RenderLivingEvent.Pre<?,?> e){if(!(e.getEntity() instanceof net.minecraft.client.player.AbstractClientPlayer p)||!(e.getRenderer() instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer r))return;PlayerState s=ClientState.get(p);renderingState=s;if(s.action.equals("stomp")){r.getModel().rightLeg.visible=false;r.getModel().rightPants.visible=false;}if(customArms(s)){r.getModel().rightArm.visible=false;r.getModel().rightSleeve.visible=false;r.getModel().leftArm.visible=false;r.getModel().leftSleeve.visible=false;}}
    @SubscribeEvent public static void playerPost(RenderPlayerEvent.Post e){var m=e.getRenderer().getModel();m.rightArm.visible=m.leftArm.visible=m.rightSleeve.visible=m.leftSleeve.visible=m.rightLeg.visible=m.rightPants.visible=true;renderingState=null;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){ClientState.clear();MouthPose.clear();}
    private static void hud(GuiGraphics g,int w,int h){Minecraft m=Minecraft.getInstance();if(m.player==null||m.options.hideGui)return;PlayerState s=ClientState.get(m.player);if(!s.stressEnabled)return;
        if(s.stress==100){float pulse=.5f+.25f*(float)Math.pow(Math.max(0,Math.sin(m.player.tickCount*.157)),3);com.mojang.blaze3d.systems.RenderSystem.enableBlend();g.setColor(1,1,1,pulse);g.blit(new net.minecraft.resources.ResourceLocation(Embers.ID,"textures/gui/stress_vignette.png"),0,0,0,0,w,h,w,h);g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.disableBlend();}
        if(!showHud)return;
        int x=7,y=h-9,width=Math.max(42,Math.min(86,w/2-106));
        g.drawString(m.font,Component.translatable("hud.embers.stress",s.stress),x,y-12,0xE0E0DC,true);
        g.fill(x-1,y-1,x+width+1,y+4,0xB8181818);g.fill(x,y,x+width,y+3,0xFF474747);
        g.fill(x,y,x+Math.round(width*s.stress/100f),y+3,s.stress>=91?0xFFAE655C:0xFFBABCB8);
    }
    @Mod.EventBusSubscriber(modid=Embers.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(ACTION);e.register(HUD);}
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){e.registerReloadListener(AnimationLibrary.INSTANCE);}
        @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{MenuScreens.register(Embers.STATION_MENU.get(),StationScreen::new);MenuScreens.register(world.embers.production.Production.MENU.get(),ProcessingScreen::new);});}
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){for(String skin:e.getSkins()){var r=e.getSkin(skin);if(r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr)pr.addLayer(new PlayerLayer(pr));}for(var type:java.util.List.of(net.minecraft.world.entity.EntityType.ZOMBIE,net.minecraft.world.entity.EntityType.SKELETON,net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER,net.minecraft.world.entity.EntityType.CREEPER,net.minecraft.world.entity.EntityType.VILLAGER)){var renderer=e.getRenderer(type);if(renderer!=null)((net.minecraft.client.renderer.entity.LivingEntityRenderer)renderer).addLayer(new MobCigaretteLayer(renderer));}}
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Embers.ASHTRAY_ENTITY.get(),AshtrayRenderer::new);e.registerBlockEntityRenderer(world.embers.production.Production.RACK_ENTITY.get(),ProductionRenderers.Rack::new);e.registerBlockEntityRenderer(world.embers.production.Production.MACHINE_ENTITY.get(),ProductionRenderers.Machine::new);e.registerEntityRenderer(Embers.BUTT_ENTITY.get(),ButtRenderer::new);}
        @SubscribeEvent public static void particles(RegisterParticleProvidersEvent e){e.registerSpriteSet(Embers.SMOKE.get(),SmokeParticle.Provider::new);e.registerSpriteSet(Embers.EXHALE.get(),SmokeParticle.Provider::new);}
        @SubscribeEvent public static void overlay(RegisterGuiOverlaysEvent e){e.registerAboveAll("stress",(gui,g,partial,w,h)->hud(g,w,h));}
    }
}
