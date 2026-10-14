package world.embers;

import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public final class ServerEvents {
    private static final Map<UUID,PlayerState> STATES=new HashMap<>();
    public static PlayerState state(Player p){return STATES.computeIfAbsent(p.getUUID(),id->PlayerState.load(p.getPersistentData().getCompound("embers")));}
    static void save(ServerPlayer p){p.getPersistentData().put("embers",state(p).save());}
    public static void sound(Player p,String id,float volume,float pitch){if(p instanceof ServerPlayer sp)Network.foley(sp,id,Math.min(1f,volume*2.6f),pitch);}
    static void lighterSound(Player p,PlayerState s,String phase,float volume){if(p instanceof ServerPlayer sp)Network.foley(sp,LighterKind.byId(s.lighter).id+"_"+phase,Math.min(1.5f,volume*4.4f),1);}
    static void tip(Player p,String key,Object...args){p.displayClientMessage(Component.translatable("message.embers."+key,args),true);}
    static boolean usable(ServerPlayer p){return !p.isSleeping()&&!p.isSpectator();}
    static ServerPlayer partner(ServerPlayer p,int id){Entity e=p.level().getEntity(id);return e instanceof ServerPlayer t&&t!=p&&t.isAlive()&&p.distanceToSqr(t)<=9&&p.hasLineOfSight(t)&&p.getLookAngle().dot(t.getEyePosition().subtract(p.getEyePosition()).normalize())>.55?t:null;}
    static ItemStack hand(Player p){return p.getMainHandItem();}
    static int chooseLighter(ServerPlayer p){List<Integer> slots=new ArrayList<>();for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack s=p.getInventory().getItem(i);if(s.getItem() instanceof LighterItem&&LighterItem.fuel(s)>0)slots.add(i);}return slots.isEmpty()?-1:slots.get(p.getRandom().nextInt(slots.size()));}
    static void selectLighter(ServerPlayer p,PlayerState s,int slot){s.lighterSlot=slot;s.lighter=((LighterItem)p.getInventory().getItem(slot).getItem()).kind.ordinal();}
    static boolean ignitionFuel(ServerPlayer p,PlayerState s){if(s.fuelSpent)return true;if(s.lighterSlot<0||s.lighterSlot>=p.getInventory().getContainerSize())return false;ItemStack stack=p.getInventory().getItem(s.lighterSlot);if(!(stack.getItem() instanceof LighterItem l)||l.kind.ordinal()!=s.lighter||!LighterItem.spend(stack))return false;s.fuelSpent=true;return true;}
    public static void input(ServerPlayer p,Network.Input input){
        PlayerState s=state(p);
        if(input.kind()==3){
            if(!usable(p)||!hand(p).isEmpty()||s.mouth<0)return;
            if(s.disposal()){cancel(p,s);return;}
            beginDisposal(p,null);return;
        }
        if(input.kind()==2){cancel(p,s);return;}
        if(input.kind()==1){release(p,s);return;}
        if(input.kind()!=0||!usable(p))return;
        if(!s.action.equals("idle")){cancel(p,s);return;}
        if(p.level().getGameTime()-s.lastRequest<3)return;
        s.idle();s.lastRequest=p.level().getGameTime();s.partner=input.target();s.sourceSlot=p.getInventory().selected;s.variant=p.getRandom().nextInt(3);
        if(!PackHandling.isolate(p))return;
        ItemStack stack=hand(p);
        if(stack.getItem() instanceof SmokingItem item){
            if(item.pack&&input.sneak()){
                if(SmokingItem.remaining(stack)==0){tip(p,"empty_pack");return;}s.start("offer",MobSmoking.target(p,input.target())!=null?8:60);sound(p,"pack_open",.35f,1);
            }else{
                if(s.mouth>=0){tip(p,"mouth_full");return;}
                if(item.pack&&SmokingItem.remaining(stack)==0){tip(p,"empty_pack");return;}
                s.sealed=item.pack&&!(stack.hasTag()&&stack.getTag().getBoolean("opened"));s.start("prepare",item.pack?30:8);if(item.pack)sound(p,s.sealed?"pack_wrap":"pack_open",.38f,1);
            }
        }else if(stack.getItem() instanceof LighterItem l){
            if(LighterItem.fuel(stack)==0){tip(p,"empty_lighter");return;}
            selectLighter(p,s,p.getInventory().selected);s.start(input.sneak()?"share_fire":"lighter_ready",input.sneak()?60:6);lighterSound(p,s,"open",.4f);
        }else if(stack.isEmpty()){
            ServerPlayer other=partner(p,input.target());
            if(other!=null&&state(other).action.equals("offer")){s.start("receive",20);}
            else if(s.mouth>=0&&s.lit){
                if(input.sneak())s.start("prepare_stomp",20);
                else if(p.level().getGameTime()-s.lastPuff>=10){s.inhaleTicks=0;s.variant=p.getRandom().nextInt(5)==0?1:0;s.start("inhale",81);sound(p,"inhale_short",.13f,1);}
            }else if(s.mouth>=0)s.start("mouth_hold",8);
        }
        if(!s.action.equals("idle"))s.actionItem=stack.copy();Network.sync(p);
    }
    static void cancel(ServerPlayer p,PlayerState s){s.cancelAction();Network.sync(p);Network.stopFoley(p);}
    static void release(ServerPlayer p,PlayerState s){
        switch(s.action){
            case "prepare" -> {
                if(s.sourceSlot==p.getInventory().selected&&hand(p).getItem() instanceof SmokingItem i&&!i.pack&&s.mouth<0){s.mouth=i.brand.ordinal();restoreCigarette(s,hand(p));hand(p).shrink(1);s.start("mouth_put",12);sound(p,"pack_take",.22f,1);}
                else s.idle();
            }
            case "mouth_hold" -> {if(s.mouth>=0){ItemStack c=new ItemStack(Embers.CIGARETTES.get(Brand.byId(s.mouth)).get());c.getOrCreateTag().putInt("burn",s.burn);c.getOrCreateTag().putInt("burnMax",s.burnMax);c.getOrCreateTag().putInt("inhale",s.inhaleTotal);s.writeCigarette(c.getOrCreateTag());give(p,c);s.clearMouth();s.start("mouth_remove",12);sound(p,"pack_return",.24f,1);}}
            case "inhale" -> {if(s.inhaleTicks<8)s.actionDuration=8;else finishInhale(p,s,false);}
            case "lighter_ready" -> s.start("fidget",42+s.variant*4);
            case "lighter_burn" -> {Network.stopFoley(p);lighterSound(p,s,"close",.30f);s.idle();}
            case "receive","prepare_stomp" -> s.idle();
            default -> { }
        }Network.sync(p);
    }
    static void restoreCigarette(PlayerState s,ItemStack stack){int max=SmokingItem.burnMax(stack);CompoundTag n=stack.getTag();s.burnMax=max;s.burn=n!=null&&n.contains("burn")?Math.max(1,Math.min(max,n.getInt("burn"))):max;s.inhaleTotal=n!=null?Math.max(0,n.getInt("inhale")):0;s.lit=false;s.readCigarette(n);}
    static void beginLighting(ServerPlayer p,PlayerState s){
        int slot=chooseLighter(p);if(slot<0){tip(p,"no_lighter");s.idle();return;}
        if(p.isUnderWater()){tip(p,"underwater");s.idle();return;}
        selectLighter(p,s,slot);s.retry=p.getRandom().nextFloat()<Rules.failChance(s.stress);SmokingBenefits.triple(p,s);s.start("lighting",30);
    }
    static void finishInhale(ServerPlayer p,PlayerState s,boolean cough){
        s.exhaleStrength=Math.min(1,Math.max(0,s.inhaleTicks)/80f);s.lastPuff=p.level().getGameTime();s.exhaleTicks=cough?25:Math.min(90,Math.max(15,s.inhaleTicks+10));s.start(cough?"cough":"exhale",s.exhaleTicks);
        String clip=cough?"cough":s.exhaleStrength>.55f?"exhale_long":s.exhaleStrength>.22f?"exhale_medium":"exhale";
        float recordedSeconds=s.exhaleStrength>.55f?3.6f:s.exhaleStrength>.22f?1.97f:.92f;
        float pitch=cough?1:Math.max(.8f,Math.min(1.4f,recordedSeconds/(s.exhaleTicks/20f)));
        Network.stopFoley(p);sound(p,clip,cough?.40f:.26f,pitch);if(cough)p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,25,0,false,false));
    }
    public static void give(Player p,ItemStack item){if(!p.getInventory().add(item))p.drop(item,false);}
    static void applyRelief(ServerPlayer p,PlayerState s){
        PressureSettings settings=PressureSettings.get(p.server);if(!settings.enabled)return;
        if(s.inhaleTotal>=Rules.INHALE_TARGET){s.smoked++;s.stress=Rules.clampStress(s.stress-settings.smokingRelief(Brand.byId(s.mouth).premium,s.smoked));}
    }
    public static boolean beginDisposal(ServerPlayer p,BlockPos tray){
        PlayerState s=state(p);if(!usable(p)||s.mouth<0||!hand(p).isEmpty()||tray!=null&&!s.lit)return false;
        s.cancelAction();s.sourceSlot=p.getInventory().selected;s.trayTarget=tray==null?null:tray.immutable();
        s.actionItem=new ItemStack(Embers.CIGARETTES.get(Brand.byId(s.mouth)).get());
        s.actionItem.getOrCreateTag().putInt("burn",s.burn);s.actionItem.getOrCreateTag().putInt("burnMax",s.burnMax);s.actionItem.getOrCreateTag().putInt("inhale",s.inhaleTotal);s.actionItem.getOrCreateTag().putBoolean("lit",s.lit);s.writeCigarette(s.actionItem.getOrCreateTag());
        s.start(tray==null?"toss":"tray_out",tray==null?28:34);Network.sync(p);return true;
    }
    public static boolean extinguish(ServerPlayer p,boolean water,boolean burnt,boolean tray){
        PlayerState s=state(p);if(s.mouth<0||!s.lit)return false;
        applyRelief(p,s);
        if(!tray)p.drop(ButtItem.of(Brand.byId(s.mouth)),false);
        sound(p,water?"water_out":burnt?"burn_hurt":"extinguish",.3f,1);
        if(burnt)p.hurt(p.damageSources().hotFloor(),2);
        ItemStack finished=new ItemStack(Embers.CIGARETTES.get(Brand.byId(s.mouth)).get());s.clearMouth();s.actionItem=finished;s.start(water?"water_out":tray?"tray_out":"stomp",tray?32:25);Network.sync(p);return true;
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||!p.isAlive())return;
        PlayerState s=state(p);long day=p.server.overworld().getDayTime()/24000;if(s.day<day)s.resetDay(day);
        boolean pressureEnabled=PressureSettings.get(p.server).enabled;if(s.stressEnabled!=pressureEnabled||!pressureEnabled&&s.stress!=0)applyPressureSwitch(p,pressureEnabled);
        String previousAction=s.action;boolean previouslyLit=s.lit;int previousMouth=s.mouth,previousReverse=s.reverseTicks;
        if(s.mouth>=0&&s.lit){
            if(p.isUnderWater())extinguish(p,true,false,false);
            else {
                s.burn-=s.action.equals("inhale")?7:1;
                if(s.action.equals("inhale")){s.inhaleTicks++;s.inhaleTotal++;if(s.inhaleTotal==1)SmokingAdvancements.award(p,"first_smoke");}
                if(s.burn<=0)extinguish(p,false,true,false);
            }
        }
        if(s.exhaleTicks>0)s.exhaleTicks--;
        FoodRestriction.bind(p);SmokingBenefits.tick(p,s);
        actionTick(p,s);
        if(pressureEnabled&&!p.isCreative())stressTick(p,s);
        if(p.tickCount%10==0||!s.action.equals(previousAction)||s.lit!=previouslyLit||s.mouth!=previousMouth||s.reverseTicks>previousReverse)Network.sync(p);
        if(p.tickCount%20==0)save(p);
        s.lastX=p.getX();s.lastZ=p.getZ();
    }
    static void actionTick(ServerPlayer p,PlayerState s){
        if(s.action.equals("idle"))return;
        s.actionTicks++;
        if(!usable(p)){s.idle();return;}
        if(Set.of("prepare","offer","fidget","share_fire","lighter_ready","lighter_burn").contains(s.action)&&(s.sourceSlot!=p.getInventory().selected||!ItemStack.isSameItem(hand(p),s.actionItem))){s.idle();return;}
        int t=s.actionTicks;
        switch(s.action){
            case "prepare" -> {
                if(t==10&&s.actionItem.getItem() instanceof SmokingItem packItem&&packItem.pack)sound(p,"pack_open",.40f,1);
                if(t==18&&s.actionItem.getItem() instanceof SmokingItem packItem&&packItem.pack)sound(p,"pack_take",.3f,1);
                if(t>=s.actionDuration){
                    if(!(hand(p).getItem() instanceof SmokingItem item)){s.idle();return;}
                    int lighter=chooseLighter(p);if(lighter<0){tip(p,"no_lighter");s.idle();return;}
                    if(item.pack){
                        if(s.stress==100){int count=SmokingItem.remaining(hand(p));int spill=Math.max(1,(int)Math.ceil(count*.7));for(int i=0;i<spill;i++)if(SmokingItem.take(hand(p)))p.drop(new ItemStack(Embers.CIGARETTES.get(item.brand).get()),true);sound(p,"drop",.4f,1);s.start("spill",32);return;}
                        if(!SmokingItem.take(hand(p))){s.idle();return;}
                        if(s.stress>=91&&p.getRandom().nextFloat()<.4f){p.drop(new ItemStack(Embers.CIGARETTES.get(item.brand).get()),true);s.start("spill",30);sound(p,"drop",.3f,1);return;}
                        s.freshCigarette();s.burn=s.burnMax=item.brand.burnTicks();s.inhaleTotal=0;s.lit=false;
                    }else {restoreCigarette(s,hand(p));hand(p).shrink(1);}
                    s.mouth=item.brand.ordinal();if(!item.pack)sound(p,"pack_take",.3f,1);beginLighting(p,s);
                }
            }
            case "mouth_hold" -> {if(t>=8)beginLighting(p,s);}
            case "lighting" -> {
                if(p.isUnderWater()){s.idle();tip(p,"underwater");return;}
                if(t==3)lighterSound(p,s,"open",.38f);
                if(t==8||s.addictionStrikes==3&&(t==13||t==18)||s.addictionStrikes!=3&&s.retry&&t==15){lighterSound(p,s,"strike",.3f);}
                int ignite=s.addictionStrikes==3?20:s.retry?18:11;
                if(t==ignite){if(!ignitionFuel(p,s)){tip(p,"empty_lighter");s.idle();return;}lighterSound(p,s,"burn",.23f);}
                if(t==ignite+5){s.lit=true;}
                if(t>=s.actionDuration){lighterSound(p,s,"close",.25f);s.idle();}
            }
            case "inhale" -> {if(s.actionDuration==8&&t>=8)finishInhale(p,s,false);else {if(t==38)sound(p,"inhale_deep",.16f,1);if(t>80)finishInhale(p,s,true);}}
            case "prepare_stomp" -> {if(t>=20){beginDisposal(p,null);}}
            case "tray_out","toss" -> {
                if(s.mouth>=0&&(s.sourceSlot!=p.getInventory().selected||!hand(p).isEmpty())){s.idle();return;}
                int contact=s.action.equals("tray_out")?18:15;
                if(t==contact&&s.mouth>=0){
                    AshtrayEntity tray=null;
                    if(s.trayTarget!=null){
                        if(p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(s.trayTarget))>25||!(p.level().getBlockEntity(s.trayTarget) instanceof AshtrayEntity a)||a.count>=AshtrayEntity.CAPACITY){s.idle();return;}tray=a;
                    }
                    ItemStack dropped=s.lit?ButtItem.of(Brand.byId(s.mouth)):s.actionItem.copy();
                    if(s.lit)applyRelief(p,s);
                    s.clearMouth();
                    if(tray!=null){tray.deposit(true,ButtItem.brand(dropped));sound(p,"extinguish",.3f,1);}
                    else {var look=p.getLookAngle();net.minecraft.world.entity.item.ItemEntity entity=dropped.is(Embers.BUTT.get())?new ButtEntity(Embers.BUTT_ENTITY.get(),p.level()):new net.minecraft.world.entity.item.ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),dropped);entity.setItem(dropped);entity.setThrower(p.getUUID());entity.setPickUpDelay(35);entity.setPos(p.getX()+look.x*.48,p.getEyeY()-.28,p.getZ()+look.z*.48);entity.setDeltaMovement(look.x*.13,.15,look.z*.13);p.level().addFreshEntity(entity);sound(p,"drop",.22f,1);}
                    Network.sync(p);
                }
                if(t>=s.actionDuration)s.idle();
            }
            case "offer" -> {if(s.actionDuration==8){var mob=MobSmoking.target(p,s.partner);if(mob==null){s.idle();return;}if(t>=8){if(MobSmoking.give(p,mob)){s.actionItem=hand(p).copy();s.start("give",20);sound(p,"pack_take",.4f,1);}else s.idle();}return;}if(t==12)sound(p,"pack_take",.40f,1);if(t>=60){sound(p,"pack_return",.25f,1);s.idle();}}
            case "exhale","cough" -> {if(t>=s.actionDuration){s.idle();Network.stopFoley(p);}}
            case "receive" -> {
                ServerPlayer giver=partner(p,s.partner);
                if(giver==null||!state(giver).action.equals("offer")||!hand(p).isEmpty()){s.idle();return;}
                if(t>=20){ItemStack pack=hand(giver);if(pack.getItem() instanceof SmokingItem item&&item.pack&&SmokingItem.take(pack)){p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Embers.CIGARETTES.get(item.brand).get()));state(giver).actionItem=pack.copy();state(giver).start("give",25);sound(giver,"pack_take",.3f,1);Network.sync(giver);s.start("received",25);}else s.idle();}
            }
            case "share_fire" -> {
                if(p.isUnderWater()){s.idle();return;}
                if(t==12&&!ignitionFuel(p,s)){tip(p,"empty_lighter");s.idle();return;}
                if(t==12){lighterSound(p,s,"strike",.3f);lighterSound(p,s,"burn",.22f);}
                ServerPlayer other=partner(p,s.partner);
                if(other==null)for(ServerPlayer candidate:p.serverLevel().players())if(partner(p,candidate.getId())!=null&&state(candidate).mouth>=0&&!state(candidate).lit){other=candidate;break;}
                Mob mob=MobSmoking.target(p,s.partner);if(t>=25&&mob!=null&&MobSmoking.light(p,mob)){s.start("share_done",25);return;}
                if(t>=25&&other!=null){PlayerState target=state(other);if(target.mouth>=0&&!target.lit&&!other.isUnderWater()&&other.getLookAngle().dot(p.getLookAngle())<.25){target.lit=true;tip(other,"borrowed_fire");Network.sync(other);s.start("share_done",25);}}
                if(t>=60)s.idle();
            }
            case "lighter_ready" -> {
                if(p.isUnderWater()||LighterItem.fuel(hand(p))<=0){s.idle();return;}
                if(t>=6){s.start("lighter_burn",Integer.MAX_VALUE);lighterSound(p,s,"strike",.30f);}
            }
            case "lighter_burn" -> {
                if(p.isUnderWater()||!LighterItem.burnTick(hand(p))){s.idle();Network.stopFoley(p);}
            }
            case "fidget" -> {
                if(p.isUnderWater()){s.idle();return;}
                if(t==8){if(!ignitionFuel(p,s)){s.idle();return;}lighterSound(p,s,"strike",.3f);lighterSound(p,s,"burn",.22f);}
                if(t==26&&s.variant>0)lighterSound(p,s,"close",.24f);
                if(t==s.actionDuration-6)lighterSound(p,s,"close",.40f);
                if(t>=s.actionDuration)s.idle();
            }
            default -> {if(t>=s.actionDuration)s.idle();}
        }
    }
    static void addStress(ServerPlayer p,int source){PressureSettings settings=PressureSettings.get(p.server);if(!settings.enabled)return;PlayerState s=state(p);long day=p.server.overworld().getDayTime()/24000;if(s.day<day)s.resetDay(day);s.addStress(source,settings.gain[source],settings.caps[source]);}
    public static void applyPressureSwitch(ServerPlayer p,boolean enabled){PlayerState s=state(p);s.stressEnabled=enabled;if(enabled)return;s.stress=s.ores=s.kills=s.animalKills=s.dimensionTicks=s.rainTicks=s.idleTicks=s.bedTicks=s.reverseTicks=0;s.resetDay(p.server.overworld().getDayTime()/24000);s.blindTimer=s.nauseaTimer=1800;s.reverseCooldown=100;p.removeEffect(Embers.UNDEAD_FOCUS.get());p.removeEffect(Embers.COLLAPSE.get());
        PressureEffects.clear(p);save(p);}
    static void stressTick(ServerPlayer p,PlayerState s){
        PressureSettings settings=PressureSettings.get(p.server);
        if(p.level().dimension()==Level.NETHER||p.level().dimension()==Level.END){if(++s.dimensionTicks>=12000){s.dimensionTicks=0;addStress(p,2);}}
        if(p.level().isRainingAt(p.blockPosition().above())&&!p.isUnderWater()){if(++s.rainTicks>=1200){s.rainTicks=0;addStress(p,5);}}
        if(Math.abs(p.getX()-s.lastX)+Math.abs(p.getZ()-s.lastZ)<.003&&!p.isSleeping()&&p.onGround()){if(++s.idleTicks>=3600){s.idleTicks=0;int r=Math.min(s.stress,Rules.dailyGrant(s.idleRelief,settings.relief[9],settings.reliefCaps[2]));s.idleRelief+=r;s.stress=Rules.clampStress(s.stress-r);}}else s.idleTicks=0;
        if(p.isSleeping()){if(++s.bedTicks%200==0){int r=Math.min(s.stress,Rules.dailyGrant(s.bedRelief,settings.relief[8],settings.reliefCaps[1]));s.bedRelief+=r;s.stress=Rules.clampStress(s.stress-r);}}else s.bedTicks=0;
        int tier=Rules.tier(s.stress);
        if(tier>0&&p.tickCount%20==0){
            PressureEffects.apply(p,MobEffects.MOVEMENT_SLOWDOWN,30,true);
            if(tier<=2)PressureEffects.apply(p,MobEffects.DAMAGE_BOOST,30,true);
            if(tier>=2)PressureEffects.apply(p,MobEffects.WEAKNESS,30,true);
            if(tier==2)p.addEffect(new MobEffectInstance(Embers.UNDEAD_FOCUS.get(),30,0,false,false,true));
            if(tier==4)p.addEffect(new MobEffectInstance(Embers.COLLAPSE.get(),30,0,false,false,true));
        }
        int period=Rules.hungerPeriod(s.stress);if(period>0&&p.tickCount%period==0){p.getFoodData().setFoodLevel(Math.max(0,p.getFoodData().getFoodLevel()-1));p.getFoodData().setSaturation(Math.min(p.getFoodData().getSaturationLevel(),p.getFoodData().getFoodLevel()));}
        if(tier>=2&&--s.blindTimer<=0){PressureEffects.apply(p,MobEffects.BLINDNESS,tier==2?40:tier==3?100:140,false);s.blindTimer=tier==4?1200:1200+p.getRandom().nextInt(1201);}
        if(tier>=3&&--s.nauseaTimer<=0){PressureEffects.apply(p,MobEffects.CONFUSION,100,false);s.nauseaTimer=1200+p.getRandom().nextInt(1201);}
        if(s.reverseTicks>0)s.reverseTicks--;
        if(tier==4){
            if(p.tickCount%200==0&&p.getHealth()>4)p.setHealth(Rules.collapseHealth(p.getHealth()));
            if(--s.reverseCooldown<=0){s.reverseCooldown=100;if(Math.abs(p.getX()-s.lastX)+Math.abs(p.getZ()-s.lastZ)>.003&&p.getRandom().nextFloat()<.3)s.reverseTicks=20;}
            if(p.tickCount%40==0)sound(p,"heartbeat",.3f,1.1f);
            if(p.tickCount%100==0)sound(p,"breath",.3f,.9f);
        }else s.reverseTicks=0;
        if(tier>0&&p.tickCount%900==0)sound(p,"sigh_"+tier,.2f,1);
    }
    @SubscribeEvent public void blockBreak(BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&PressureSettings.get(p.server).enabled&&!p.isCreative()&&e.getState().is(net.minecraftforge.common.Tags.Blocks.ORES)){PlayerState s=state(p);if(++s.ores>=15){s.ores=0;addStress(p,0);}}}
    @SubscribeEvent public void damage(LivingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>4&&!p.isCreative())addStress(p,3);}
    @SubscribeEvent public void hurt(LivingHurtEvent e){if(e.getSource().getEntity() instanceof ServerPlayer p&&PressureSettings.get(p.server).enabled&&p.hasEffect(Embers.UNDEAD_FOCUS.get())&&e.getEntity().getMobType()==MobType.UNDEAD)e.setAmount(e.getAmount()+2.5f);}
    public static boolean harmlessAnimal(LivingEntity e){
        // Animal family includes domestic dogs/wolves and livestock; fish have their own family.
        return !(e instanceof net.minecraft.world.entity.animal.AbstractFish)&&e instanceof net.minecraft.world.entity.animal.Animal a&&!(e instanceof Enemy);
    }
    @SubscribeEvent public void death(LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p){PlayerState s=state(p);if(!p.isCreative())addStress(p,4);s.clearMouth();s.idle();save(p);}
        else if(e.getSource().getEntity() instanceof ServerPlayer p&&PressureSettings.get(p.server).enabled&&!p.isCreative()){
            PlayerState s=state(p);long day=p.server.overworld().getDayTime()/24000;if(s.day<day)s.resetDay(day);
            if(e.getEntity() instanceof Enemy){if(++s.kills>=4){s.kills=0;addStress(p,1);}}
            else if(harmlessAnimal(e.getEntity())){if(++s.animalKills%3==0)addStress(p,6);Network.sync(p);}
        }
    }
    @SubscribeEvent public void fishing(ItemFishedEvent e){if(e.getEntity() instanceof ServerPlayer p&&PressureSettings.get(p.server).enabled){PressureSettings settings=PressureSettings.get(p.server);PlayerState s=state(p);int count=e.getDrops().stream().filter(i->i.is(ItemTags.FISHES)).mapToInt(ItemStack::getCount).sum();int r=Math.min(s.stress,Rules.dailyGrant(s.fishRelief,count*settings.relief[6],settings.reliefCaps[0]));s.fishRelief+=r;s.stress=Rules.clampStress(s.stress-r);}}
    @SubscribeEvent public void wake(PlayerWakeUpEvent e){if(e.getEntity() instanceof ServerPlayer p&&PressureSettings.get(p.server).enabled&&!e.wakeImmediately()&&p.getSleepTimer()>=100){PressureSettings settings=PressureSettings.get(p.server);PlayerState s=state(p);int r=Math.min(s.stress,Rules.dailyGrant(s.bedRelief,settings.relief[7],settings.reliefCaps[1]));s.bedRelief+=r;s.stress=Rules.clampStress(s.stress-r);}}
    @SubscribeEvent public void login(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){PlayerState s=state(p);s.idle();s.lit=false;applyPressureSwitch(p,PressureSettings.get(p.server).enabled);Network.sync(p);for(ServerPlayer other:p.server.getPlayerList().getPlayers())Network.sendTo(p,other);}}
    @SubscribeEvent public void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){state(p).idle();state(p).lit=false;save(p);STATES.remove(p.getUUID());}}
    @SubscribeEvent public void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone e){e.getEntity().getPersistentData().put("embers",e.getOriginal().getPersistentData().getCompound("embers").copy());STATES.remove(e.getEntity().getUUID());}
    @SubscribeEvent public void track(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof ServerPlayer t)Network.sendTo(p,t);}
    @SubscribeEvent public void stopping(net.minecraftforge.event.server.ServerStoppingEvent e){for(ServerPlayer p:e.getServer().getPlayerList().getPlayers())save(p);}
    @SubscribeEvent public void stop(ServerStoppedEvent e){STATES.clear();}
    @SubscribeEvent public void commands(RegisterCommandsEvent e){
        if(Boolean.getBoolean("embers.qa"))e.getDispatcher().register(Commands.literal("embers_qa").requires(s->s.hasPermission(2))
            .then(Commands.literal("reset").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();PlayerState s=state(p);s.cancelAction();s.clearMouth();s.stress=0;s.lastRequest=-100;s.lastPuff=-100;Network.sync(p);return 1;}))
            .then(Commands.literal("sneak").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).idle();state(p).lastRequest=-100;input(p,new Network.Input(0,true,-1));return 1;}))
            .then(Commands.literal("start").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).idle();state(p).lastRequest=-100;int target=-1;for(ServerPlayer t:p.serverLevel().players())if(partner(p,t.getId())!=null){target=t.getId();break;}input(p,new Network.Input(0,p.isShiftKeyDown(),target));return 1;}))
            .then(Commands.literal("release").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();input(p,new Network.Input(1,false,-1));return 1;})));
        e.getDispatcher().register(Commands.literal("embers").requires(s->s.hasPermission(2))
        .then(Commands.literal("stress").then(Commands.argument("value",IntegerArgumentType.integer(0,100)).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();state(p).stress=IntegerArgumentType.getInteger(c,"value");Network.sync(p);return 1;})))
        .then(Commands.literal("kit").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();for(Brand b:Brand.values())give(p,new ItemStack(Embers.PACKS.get(b).get()));for(LighterKind k:LighterKind.values())give(p,new ItemStack(Embers.LIGHTERS.get(k).get()));give(p,new ItemStack(Embers.STATION.get()));give(p,new ItemStack(Embers.ASHTRAY.get()));give(p,new ItemStack(Embers.GLASS_ASHTRAY.get()));give(p,new ItemStack(Embers.BRASS_ASHTRAY.get()));give(p,new ItemStack(Items.LAVA_BUCKET));return 1;}))
        .then(Commands.literal("status").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal(state(p).save().toString()),false);return 1;}))
    );}
}
