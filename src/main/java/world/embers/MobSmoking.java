package world.embers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Each smoke session belongs to a single giver; no global peaceful-AI flag is changed. */
@Mod.EventBusSubscriber(modid=Embers.ID)
public final class MobSmoking {
    private static final Map<UUID,Mob> SOURCES=new HashMap<>();
    public interface OfferOwner {void embers$owner(Villager villager);}
    public static net.minecraft.world.item.trading.MerchantOffers displayOffers(net.minecraft.world.item.trading.MerchantOffers source){
        var snapshot=new net.minecraft.world.item.trading.MerchantOffers();
        for(var offer:source){var copy=new MerchantOffer(offer.createTag());copy.setSpecialPriceDiff(offer.getSpecialPriceDiff());snapshot.add(copy);}return snapshot;
    }
    public static boolean supported(Entity e){var t=e.getType();return t==EntityType.ZOMBIE||t==EntityType.SKELETON||t==EntityType.CREEPER||t==EntityType.ZOMBIE_VILLAGER||t==EntityType.VILLAGER;}
    public static CompoundTag data(Entity e){CompoundTag root=e.getPersistentData();if(!root.contains("embers_smoking")){CompoundTag n=new CompoundTag();n.putInt("brand",-1);root.put("embers_smoking",n);}return root.getCompound("embers_smoking");}
    public static boolean visible(Entity e){return supported(e)&&data(e).getInt("brand")>=0;}
    public static boolean lit(Entity e){return visible(e)&&data(e).getBoolean("lit");}
    public static int remaining(Entity e){return (int)Math.max(0,data(e).getLong("expires")-e.level().getGameTime());}
    public static Mob target(ServerPlayer p,int id){Entity e=p.level().getEntity(id);return e instanceof Mob mob&&supported(e)&&e.isAlive()&&p.distanceToSqr(e)<=9&&p.hasLineOfSight(e)&&p.getLookAngle().dot(e.getEyePosition().subtract(p.getEyePosition()).normalize())>.55?mob:null;}
    private static boolean enabled(Mob e){var r=PressureSettings.get(e.level().getServer());return e instanceof Villager?r.villagerEnabled:r.mobEnabled;}
    public static boolean give(ServerPlayer p,Mob mob){if(!enabled(mob)||visible(mob)||p.isUnderWater()||mob.isUnderWater())return false;CompoundTag n=data(mob);String key=p.getUUID().toString();if(n.getCompound("cooldowns").getLong(key)>mob.level().getGameTime())return false;ItemStack pack=p.getMainHandItem();if(!(pack.getItem() instanceof SmokingItem item)||!item.pack||!SmokingItem.take(pack))return false;
        n.putInt("brand",item.brand.ordinal());n.putBoolean("lit",false);n.putUUID("giver",p.getUUID());n.putBoolean("broken",false);n.putLong("expires",mob.level().getGameTime()+200);n.putInt("max",item.brand.burnTicks());n.putInt("discount",1+mob.getRandom().nextInt(3));
        CompoundTag cooldown=n.getCompound("cooldowns");cooldown.getAllKeys().removeIf(k->cooldown.getLong(k)<mob.level().getGameTime());cooldown.putLong(key,mob.level().getGameTime()+1200);n.put("cooldowns",cooldown);SOURCES.put(mob.getUUID(),mob);if(mob.getTarget()==p){n.putLong("protectedTick",mob.level().getGameTime());mob.setTarget(null);if(mob instanceof Creeper c&&!c.isIgnited())c.setSwellDir(-1);}sync(mob);return true;
    }
    public static boolean light(ServerPlayer p,Mob mob){if(!enabled(mob)||!visible(mob)||lit(mob)||remaining(mob)<=0||mob.isUnderWater()||p.isUnderWater())return false;CompoundTag n=data(mob);n.putBoolean("lit",true);n.putLong("expires",mob.level().getGameTime()+n.getInt("max"));SOURCES.put(mob.getUUID(),mob);sync(mob);refreshTrade(mob);return true;}
    public static boolean protectedFrom(Mob attacker,Player player){if(!supported(attacker)||attacker instanceof Villager||!attacker.isAlive()||!PressureSettings.get(player.level().getServer()).mobEnabled)return false;
        for(Mob source:SOURCES.values()){if(source.level()!=attacker.level()||source.isRemoved()||!source.isAlive()||!visible(source)||remaining(source)<=0)continue;CompoundTag n=data(source);if(n.getBoolean("broken")||!n.hasUUID("giver")||!n.getUUID("giver").equals(player.getUUID()))continue;if(source==attacker&&!lit(source))return true;if(lit(source)&&source.getType()==attacker.getType()&&source.distanceToSqr(attacker)<=400)return true;}return false;
    }
    public static int discount(Villager v){if(v==null||v.level().isClientSide||!lit(v)||remaining(v)<=0||!enabled(v)||data(v).getBoolean("broken"))return 0;Player p=v.getTradingPlayer();return p!=null&&data(v).hasUUID("giver")&&data(v).getUUID("giver").equals(p.getUUID())?Math.max(1,Math.min(3,data(v).getInt("discount"))):0;}
    private static void refreshTrade(Mob mob){if(mob instanceof Villager v&&v.getTradingPlayer() instanceof ServerPlayer p&&p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu){p.sendMerchantOffers(p.containerMenu.containerId,v.getOffers(),v.getVillagerData().getLevel(),v.getVillagerXp(),v.showProgressBar(),v.canRestock());p.containerMenu.slots.get(0).container.setChanged();p.containerMenu.broadcastChanges();}}
    public static void finish(Mob mob,boolean drop){if(!visible(mob))return;CompoundTag n=data(mob);Brand brand=Brand.byId(n.getInt("brand"));boolean wasLit=n.getBoolean("lit");n.putInt("brand",-1);n.putBoolean("lit",false);SOURCES.remove(mob.getUUID());
        if(drop){ItemStack item=wasLit?ButtItem.of(brand):new ItemStack(Embers.CIGARETTES.get(brand).get());ItemEntity entity=wasLit?new ButtEntity(Embers.BUTT_ENTITY.get(),mob.level()):new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),item);entity.setItem(item);entity.setPos(mob.getX(),mob.getEyeY()-.13,mob.getZ());entity.setDefaultPickUpDelay();entity.setDeltaMovement(0,.04,0);mob.level().addFreshEntity(entity);}sync(mob);refreshTrade(mob);
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent e){if(!(e.getEntity() instanceof Mob mob)||mob.level().isClientSide||!supported(mob))return;
        if(visible(mob)){SOURCES.put(mob.getUUID(),mob);if(!enabled(mob)||remaining(mob)<=0||mob.isUnderWater()){finish(mob,true);}else if(mob.tickCount%20==0)sync(mob);}
        if(mob.getTarget() instanceof Player p&&protectedFrom(mob,p)){data(mob).putLong("protectedTick",mob.level().getGameTime());mob.setTarget(null);mob.getNavigation().stop();if(mob instanceof Creeper c&&!c.isIgnited())c.setSwellDir(-1);}
    }
    @SubscribeEvent public static void changeTarget(LivingChangeTargetEvent e){if(e.getEntity() instanceof Mob mob&&!mob.level().isClientSide&&e.getNewTarget() instanceof Player p&&protectedFrom(mob,p)){data(mob).putLong("protectedTick",mob.level().getGameTime());e.setCanceled(true);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attacks(LivingAttackEvent e){if(e.getEntity().level().isClientSide)return;
        if(e.getEntity() instanceof Player p&&e.getSource().getEntity() instanceof Mob mob&&protectedFrom(mob,p)){e.setCanceled(true);return;}
        if(e.getEntity() instanceof Mob victim&&supported(victim)&&e.getSource().getEntity() instanceof Player p){for(Mob source:SOURCES.values()){CompoundTag n=data(source);if(source.level()==victim.level()&&n.hasUUID("giver")&&n.getUUID("giver").equals(p.getUUID())){n.putBoolean("broken",true);sync(source);refreshTrade(source);}}}
    }
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof Mob m&&!m.level().isClientSide&&supported(m))finish(m,true);}
    @SubscribeEvent public static void unload(EntityLeaveLevelEvent e){if(e.getEntity() instanceof Mob m&&!e.getLevel().isClientSide)SOURCES.remove(m.getUUID());}
    @SubscribeEvent public static void load(EntityJoinLevelEvent e){if(e.getEntity() instanceof Mob m&&!e.getLevel().isClientSide&&supported(m)&&visible(m)&&remaining(m)>0)SOURCES.put(m.getUUID(),m);}
    @SubscribeEvent public static void track(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof Mob mob&&supported(mob))Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.MobState(mob.getId(),mob.getUUID(),data(mob).copy()));}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SOURCES.clear();}
    public static void sync(Mob mob){Network.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->mob),new Network.MobState(mob.getId(),mob.getUUID(),data(mob).copy()));}
    public static boolean extendFuse(Creeper c){if(c.isIgnited()||!PressureSettings.get(c.level().getServer()).mobEnabled||!(c.getTarget() instanceof ServerPlayer p)||c.distanceToSqr(p)>36)return false;CompoundTag n=data(c);if(n.getBoolean("fuseExtended"))return false;ItemStack item=p.getMainHandItem();if(!(item.getItem() instanceof SmokingItem s)||!s.pack||SmokingItem.remaining(item)<=0||p.getLookAngle().dot(c.getEyePosition().subtract(p.getEyePosition()).normalize())<.6)return false;PlayerState state=ServerEvents.state(p);double dx=p.getX()-state.lastX,dz=p.getZ()-state.lastZ;if(dx*dx+dz*dz<.00001&&!p.isSprinting())return false;n.putBoolean("fuseExtended",true);return true;}
}
