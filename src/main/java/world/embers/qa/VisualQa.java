package world.embers.qa;

import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import world.embers.*;
import world.embers.client.ClientState;
import java.nio.file.*;
import java.util.*;

/** Development-only repeatable visual checks; explicitly excluded from release jars. */
@Mod.EventBusSubscriber(modid=Embers.ID,value=Dist.CLIENT)
public final class VisualQa {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final ArrayDeque<JsonObject> steps=new ArrayDeque<>();
    private static final List<String> sounds=new ArrayList<>();
    private static int wait;
    private static JsonArray inspectBlocks=new JsonArray();
    private static String capture;
    private static int nativeArmCalls,replacedArmCalls;
    private static boolean lastBlockClickCanceled;
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
    public static void arm(net.minecraftforge.client.event.RenderArmEvent e){if(Boolean.getBoolean("embers.qa")){nativeArmCalls++;if(e.isCanceled())replacedArmCalls++;}}
    @SubscribeEvent public static void sound(PlaySoundEvent e){if(Boolean.getBoolean("embers.qa")&&e.getSound()!=null&&e.getSound().getLocation().getNamespace().equals(Embers.ID))sounds.add(e.getSound().getLocation().toString());}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("embers.qa")||e.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;
        // Isolated automated clients must not capture the owner's desktop mouse.
        mc.mouseHandler.releaseMouse();mc.options.keyAttack.setDown(false);
        try {
            Path input=mc.gameDirectory.toPath().resolve("qa-steps.json");
            if(steps.isEmpty()&&wait==0&&capture==null&&Files.exists(input)){
                for(var step:JsonParser.parseString(Files.readString(input)).getAsJsonArray())steps.add(step.getAsJsonObject());Files.delete(input);
            }
            if(wait>0){wait--;return;}if(capture!=null||steps.isEmpty())return;
            var step=steps.remove();
            if(step.has("hideGui"))mc.options.hideGui=step.get("hideGui").getAsBoolean();
            if(step.has("inspectBlocks"))inspectBlocks=step.getAsJsonArray("inspectBlocks");
            if(step.has("guiScale")){mc.options.guiScale().set(step.get("guiScale").getAsInt());mc.resizeDisplay();}
            if(step.has("respawn")&&mc.player.isDeadOrDying()){mc.player.respawn();mc.setScreen(null);}
            if(step.has("clearChat"))mc.gui.getChat().clearMessages(false);
            if(step.has("productionCheck"))mc.getSingleplayerServer().execute(()->ProductionQa.run(mc.getSingleplayerServer(),mc.player.getUUID(),mc.gameDirectory.toPath()));
            if(step.has("resetBenefits"))mc.getSingleplayerServer().execute(()->RevisionQa.resetBenefits(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())));
            if(step.has("flight"))mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.getAbilities().flying=true;p.onUpdateAbilities();});
            if(step.has("serverReport")){String report=step.get("serverReport").getAsString().replaceAll("[^a-zA-Z0-9_-]","");mc.getSingleplayerServer().execute(()->RevisionQa.report(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()),mc.gameDirectory.toPath().resolve("screenshots/"+report+"-server.json")));}
            if(step.has("interactMob")){var mob=mc.level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,mc.player.getBoundingBox().inflate(4)).stream().min(java.util.Comparator.comparingDouble(mc.player::distanceToSqr)).orElseThrow();mc.gameMode.interact(mc.player,mob,net.minecraft.world.InteractionHand.MAIN_HAND);}
            if(step.has("menuClick")){var c=step.getAsJsonArray("menuClick");mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,c.get(0).getAsInt(),c.get(1).getAsInt(),net.minecraft.world.inventory.ClickType.valueOf(c.get(2).getAsString()),mc.player);}
            if(step.has("jeiShow"))JeiQa.show(step.get("jeiShow").getAsString());
            if(step.has("garden"))mc.getSingleplayerServer().execute(()->ProductionQa.garden(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())));
            if(step.has("cultivationCheck"))mc.getSingleplayerServer().execute(()->CultivationQa.run(mc.getSingleplayerServer(),mc.player.getUUID(),mc.gameDirectory.toPath()));
            if(step.has("checkBox")){var box=(net.minecraft.client.gui.components.Checkbox)mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Checkbox).findFirst().orElseThrow();box.onPress();}
            if(step.has("commerceCheck"))mc.getSingleplayerServer().execute(()->CommerceQa.run(mc.getSingleplayerServer(),mc.player.getUUID(),mc.gameDirectory.toPath()));
            if(step.has("useItem"))mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);
            if(step.has("closeScreen")&&mc.screen!=null)mc.screen.onClose();
            if(step.has("click")){String label=step.get("click").getAsString();var wanted=net.minecraft.network.chat.Component.translatable(label).getString();var buttons=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button).map(c->(net.minecraft.client.gui.components.Button)c).filter(b->b.getMessage().getString().equals(wanted)).toList();int index=step.has("clickIndex")?step.get("clickIndex").getAsInt():0;buttons.get(index).onPress();}
            if(step.has("edit")){var values=step.getAsJsonArray("edit");var boxes=mc.screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).toList();for(int i=0;i<values.size();i++)boxes.get(i).setValue(values.get(i).getAsString());}
            if(step.has("slot"))mc.player.getInventory().selected=step.get("slot").getAsInt();
            if(step.has("shift"))mc.options.keyShift.setDown(step.get("shift").getAsBoolean());
            if(step.has("look")){var a=step.getAsJsonArray("look");mc.player.setYRot(a.get(0).getAsFloat());mc.player.setXRot(a.get(1).getAsFloat());}
            if(step.has("drop"))mc.player.drop(false);
            if(step.has("clickBlock")){
                var a=step.getAsJsonArray("clickBlock");BlockPos at=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
                var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),net.minecraft.core.Direction.UP,at,false);mc.hitResult=hit;
                var event=net.minecraftforge.client.ForgeHooksClient.onClickInput(1,mc.options.keyUse,net.minecraft.world.InteractionHand.MAIN_HAND);lastBlockClickCanceled=event.isCanceled();
                if(!event.isCanceled())mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            }
            if(step.has("burnFraction")){float fraction=step.get("burnFraction").getAsFloat();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);var state=ServerEvents.state(player);state.burn=Math.max(1,(int)(state.burnMax*fraction));Network.sync(player);});}
            if(step.has("supportVariant")){int variant=step.get("supportVariant").getAsInt();var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(id);ServerEvents.state(player).variant=variant;Network.sync(player);});}
            if(step.has("dismount")){var id=mc.player.getUUID();mc.getSingleplayerServer().execute(()->mc.getSingleplayerServer().getPlayerList().getPlayer(id).stopRiding());}
            if(step.has("useTray")){var a=step.getAsJsonArray("useTray");BlockPos at=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(at).add(.5,.15,.5),net.minecraft.core.Direction.UP,at,false));}
            if(step.has("fov"))mc.options.fov().set(step.get("fov").getAsInt());
            if(step.has("walk"))mc.options.keyUp.setDown(step.get("walk").getAsBoolean());
            if(step.has("sprint")){mc.options.keySprint.setDown(step.get("sprint").getAsBoolean());mc.player.setSprinting(step.get("sprint").getAsBoolean());}
            if(step.has("language")){String code=step.get("language").getAsString();mc.getLanguageManager().setSelected(code);mc.options.languageCode=code;mc.options.save();mc.reloadResourcePacks();}
            if(step.has("camera"))mc.options.setCameraType(CameraType.valueOf(step.get("camera").getAsString()));
            if(step.has("commands"))for(var cmd:step.getAsJsonArray("commands"))mc.player.connection.sendCommand(cmd.getAsString());
            if(step.has("input"))Network.CHANNEL.sendToServer(new Network.Input(step.get("input").getAsInt(),step.has("sneak")&&step.get("sneak").getAsBoolean(),-1));
            if(step.has("button"))world.embers.client.ClientEvents.ACTION.setDown(step.get("button").getAsBoolean());
            if(step.has("escape"))net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.InputEvent.Key(256,0,1,0));
            if(step.has("capture"))capture=step.get("capture").getAsString().replaceAll("[^a-zA-Z0-9_-]","");
            if(step.has("wait"))wait=step.get("wait").getAsInt();
            if(step.has("quit")){mc.getConnection().getConnection().disconnect(net.minecraft.network.chat.Component.literal("QA complete"));mc.stop();}
        }catch(Exception ex){throw new RuntimeException("Visual QA sequence failed",ex);}
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(!Boolean.getBoolean("embers.qa")||e.phase!=TickEvent.Phase.END||capture==null||wait>0)return;
        Minecraft mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        try {
            String name=capture;capture=null;
            Path directory=mc.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(directory);
            try(var pixels=Screenshot.takeScreenshot(mc.getMainRenderTarget())){pixels.writeToFile(directory.resolve(name+".png"));}
            var s=ClientState.get(mc.player);JsonObject result=new JsonObject();
            JsonArray observed=new JsonArray();for(var c:inspectBlocks){var a=c.getAsJsonArray();var at=new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());observed.add(mc.level.getBlockState(at).toString());}result.add("observedBlocks",observed);
            result.addProperty("emberHeat",s.emberHeat);
            result.addProperty("hit",mc.hitResult==null?"none":mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit?hit.getEntity().getName().getString():mc.hitResult.getType().name());
            JsonArray people=new JsonArray();for(var player:mc.level.players()){
                var ps=ClientState.get(player);JsonObject info=new JsonObject();var held=player.getMainHandItem();
                info.addProperty("name",player.getName().getString());info.addProperty("mouth",ps.mouth);info.addProperty("lit",ps.lit);info.addProperty("action",ps.action);info.addProperty("emberHeat",ps.emberHeat);
                info.addProperty("held",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(held.getItem()).toString());info.addProperty("count",held.getCount());
                if(held.getItem() instanceof SmokingItem item&&item.pack)info.addProperty("packRemaining",SmokingItem.remaining(held));
                if(held.getItem() instanceof LighterItem)info.addProperty("fuel",LighterItem.fuel(held));people.add(info);
            }result.add("players",people);
            JsonArray mobs=new JsonArray();for(var entity:mc.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.Mob mob&&MobSmoking.supported(mob)){JsonObject info=new JsonObject();info.addProperty("id",mob.getId());info.addProperty("type",mob.getType().toString());info.addProperty("mouth",MobSmoking.data(mob).getInt("brand"));info.addProperty("lit",MobSmoking.lit(mob));info.addProperty("remaining",MobSmoking.remaining(mob));info.addProperty("position",mob.position().toString());info.addProperty("smokeAnchor",world.embers.client.MobCigaretteLayer.anchorDebug(mob));mobs.add(info);}result.add("mobs",mobs);
            if(mc.player.containerMenu instanceof world.embers.production.ProcessingMenu processing){var machine=processing.machine;result.addProperty("machineRunning",machine.running);result.addProperty("machineWork",machine.work);result.addProperty("machineFuel",machine.fuel);result.addProperty("machineReason",machine.reason);JsonArray inventory=new JsonArray();for(int i=0;i<9;i++)inventory.add(machine.getItem(i).toString());result.add("machineItems",inventory);}
            if(mc.player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu merchant){JsonArray offers=new JsonArray();for(var offer:merchant.getOffers()){JsonObject entry=new JsonObject();entry.addProperty("base",offer.getBaseCostA().toString());entry.addProperty("cost",offer.getCostA().toString());entry.addProperty("result",offer.getResult().toString());entry.addProperty("special",offer.getSpecialPriceDiff());offers.add(entry);}result.add("merchantOffers",offers);JsonArray inventory=new JsonArray();for(var slot:merchant.slots)inventory.add(slot.getItem().toString());result.add("merchantInventory",inventory);}
            result.add("heldTooltip",JSON.toJsonTree(mc.player.getMainHandItem().getTooltipLines(mc.player,net.minecraft.world.item.TooltipFlag.Default.NORMAL).stream().map(net.minecraft.network.chat.Component::getString).toList()));
            result.addProperty("combustionLoops",world.embers.client.ClientFoley.loopCount());
            if(mc.player.getMainHandItem().getItem() instanceof LighterItem){result.addProperty("lighterFuel",LighterItem.fuel(mc.player.getMainHandItem()));result.addProperty("flameTicks",LighterItem.flameTicks(mc.player.getMainHandItem()));}
            result.addProperty("action",s.action);result.addProperty("actionTicks",s.actionTicks);result.addProperty("mouth",s.mouth);result.addProperty("lit",s.lit);result.addProperty("burn",s.burn);
            result.addProperty("variant",s.variant);result.addProperty("passenger",mc.player.isPassenger());result.addProperty("vehicle",mc.player.getVehicle()==null?"none":mc.player.getVehicle().getType().toString());result.addProperty("lastBlockClickCanceled",lastBlockClickCanceled);
            result.add("mouthSway",JSON.toJsonTree(world.embers.client.MouthPose.sway()));
            if(mc.player.containerMenu instanceof StationMenu station){result.addProperty("refillFee",station.fee());result.addProperty("canRefill",station.canRefill());result.addProperty("refillInput",station.station.getItem(0).toString());result.addProperty("refillMaterial",station.station.getItem(1).toString());result.addProperty("refillOutput",station.station.getItem(2).toString());}result.addProperty("exhaleTicks",s.exhaleTicks);
            result.addProperty("mouthSide",world.embers.client.MouthPose.side());result.addProperty("nativeArmCalls",nativeArmCalls);result.addProperty("replacedArmCalls",replacedArmCalls);nativeArmCalls=replacedArmCalls=0;
            result.addProperty("burnMax",s.burnMax);result.addProperty("remainingFraction",s.remaining());
            if(s.mouth>=0){result.addProperty("cigaretteRadius",Brand.byId(s.mouth).cigaretteRadius());result.addProperty("cigaretteLength",Brand.byId(s.mouth).cigaretteLength(s.remaining()));}
            result.addProperty("exhaleStrength",s.exhaleStrength);result.addProperty("nearSmokeElements",world.embers.client.FirstPersonSmoke.count());result.addProperty("mouthVisible",s.mouthVisible());
            result.addProperty("position",mc.player.position().toString());
            result.addProperty("health",mc.player.getHealth());
            result.addProperty("stressEnabled",s.stressEnabled);result.addProperty("stress",s.stress);result.addProperty("screen",mc.screen==null?"none":mc.screen.getClass().getName());
            if(mc.screen!=null){JsonArray widgets=new JsonArray();for(var child:mc.screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widgets.add(widget.getMessage().getString());result.add("widgets",widgets);}
            result.addProperty("smokePackedLight",world.embers.client.FirstPersonSmoke.light());
            JsonArray trays=new JsonArray();for(BlockPos pos:BlockPos.betweenClosed(mc.player.blockPosition().offset(-4,-2,-4),mc.player.blockPosition().offset(4,2,4)))if(mc.level.getBlockEntity(pos) instanceof AshtrayEntity tray){JsonObject obj=new JsonObject();obj.addProperty("position",pos.toShortString());obj.addProperty("count",tray.count);int warm=0;for(int i=0;i<tray.count;i++)if(tray.warmth(i,mc.level.getGameTime())>0)warm++;obj.addProperty("smolderingButts",warm);trays.add(obj);}result.add("ashtrays",trays);
            JsonArray butts=new JsonArray();for(var entity:mc.level.entitiesForRendering())if(entity instanceof ButtEntity butt){JsonObject obj=new JsonObject();obj.addProperty("id",butt.getId());obj.addProperty("position",butt.position().toString());obj.addProperty("onGround",butt.onGround());obj.addProperty("velocity",butt.getDeltaMovement().toString());butts.add(obj);}result.add("groundButts",butts);
            result.addProperty("heldItem",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString());
            result.addProperty("sprinting",mc.player.isSprinting());result.addProperty("walkDistance",mc.player.walkDist);result.addProperty("language",mc.options.languageCode);
            BlockPos eye=BlockPos.containing(mc.player.getEyePosition());
            result.addProperty("blockLightAtEye",mc.level.getBrightness(LightLayer.BLOCK,eye));
            result.addProperty("blockLightNextToEye",mc.level.getBrightness(LightLayer.BLOCK,eye.east()));
            result.addProperty("blockLightFourAway",mc.level.getBrightness(LightLayer.BLOCK,eye.east(4)));
            result.addProperty("camera",mc.options.getCameraType().name());result.add("soundsSinceLastFrame",JSON.toJsonTree(sounds));sounds.clear();
            Files.writeString(directory.resolve(name+".json"),JSON.toJson(result));
        }catch(Exception ex){throw new RuntimeException("Visual QA capture failed",ex);}
    }
}
