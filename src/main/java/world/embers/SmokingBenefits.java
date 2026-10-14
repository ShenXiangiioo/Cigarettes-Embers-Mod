package world.embers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

@Mod.EventBusSubscriber(modid=Embers.ID)
public final class SmokingBenefits {
    public static final List<RegistryObject<MobEffect>> EFFECTS=new ArrayList<>();
    public static final RegistryObject<MobEffect> ADDICTION=Embers.EFFECTS.register("addiction",()->new Embers.EmbersEffect(MobEffectCategory.HARMFUL,0x777063));
    static {String[] keys={"dragon_slayer","soaring_dragon","apple_lover","satiation"};int[] colors={0x7B7768,0x918AAC,0x91AB62,0xC1A267};for(int i=0;i<4;i++){int index=i;EFFECTS.add(Embers.EFFECTS.register(keys[i],()->new Embers.EmbersEffect(MobEffectCategory.BENEFICIAL,colors[index])));}}
    public static void register(){}
    public static CompoundTag data(Player p){CompoundTag root=p.getPersistentData();if(!root.contains("embers_benefits"))root.put("embers_benefits",new CompoundTag());return root.getCompound("embers_benefits");}
    public static boolean addicted(Player p){return data(p).getInt("addiction")>0;}
    public static boolean active(Player p,int group){return data(p).getInt("effect"+group)>0&&!addicted(p);}
    static int group(Brand b){if(b.horizontalCase())return 0;if(b==Brand.HETIANXIA||b==Brand.HETIANXIA_SLIM)return 1;if(b==Brand.CHAPMAN||b==Brand.CHAPMAN_SLIM)return 2;if(b==Brand.ZHONGHUA||b==Brand.ZHONGHUA_SLIM)return 3;return -1;}
    public static void tick(ServerPlayer p,PlayerState s){
        CompoundTag n=data(p);PressureSettings rules=PressureSettings.get(p.server);long day=p.server.overworld().getDayTime()/24000;
        if(!n.contains("day")||day>n.getLong("day")){n.putLong("day",day);n.putInt("daily",0);}
        int addiction=n.getInt("addiction");if(!rules.addictionEnabled){addiction=0;n.putInt("daily",0);}else if(addiction>0){addiction--;if(addiction==0)n.putInt("daily",0);}n.putInt("addiction",addiction);
        if(s.mouth>=0&&s.lit&&s.inhaleTotal>=Rules.INHALE_TARGET&&!s.cigaretteCounted){
            s.cigaretteCounted=true;
            if(rules.addictionEnabled&&addiction==0){int count=n.getInt("daily")+1;n.putInt("daily",count);if(count>15){addiction=72000;n.putInt("addiction",addiction);}}
            int group=group(Brand.byId(s.mouth));s.benefitSpent=true;
            if(addiction==0&&rules.effectsEnabled&&group>=0&&rules.brandEffects[group]&&n.getInt("cooldown"+group)==0&&n.getInt("effect"+group)==0){n.putInt("effect"+group,1200);n.putString("source"+group,s.cigaretteId);p.addEffect(new MobEffectInstance(EFFECTS.get(group).get(),1200,0,false,false,true));if(group==1)grantFlight(p,n);}
        }
        for(int i=0;i<4;i++){
            int cd=n.getInt("cooldown"+i);if(cd>0)n.putInt("cooldown"+i,cd-1);
            int remaining=n.getInt("effect"+i);if(remaining<=0)continue;
            boolean allowed=rules.effectsEnabled&&rules.brandEffects[i]&&addiction==0;
            if(!allowed){end(p,i);continue;}
            if((i==0||i==2)&&s.mouth>=0&&s.lit&&s.cigaretteId.equals(n.getString("source"+i)))remaining=1201;
            remaining--;n.putInt("effect"+i,remaining);
            if(i==3&&remaining%200==0)p.getFoodData().eat(1,1f);
            if(remaining==0){n.putInt("cooldown"+i,1200);end(p,i);continue;}
            if(p.tickCount%20==0)p.addEffect(new MobEffectInstance(EFFECTS.get(i).get(),remaining,0,false,false,true));
        }
        if(addiction>0){if(p.tickCount%20==0||!p.hasEffect(ADDICTION.get()))p.addEffect(new MobEffectInstance(ADDICTION.get(),addiction,0,false,false,true));}else p.removeEffect(ADDICTION.get());
        if(n.getBoolean("landing")){
            if(p.onGround()||p.isInWater()||p.isCreative()||p.isSpectator()||p.getY()<p.level().getMinBuildHeight()-32){n.putBoolean("landing",false);n.putInt("cooldown1",1200);}
            else {p.fallDistance=0;if(p.getDeltaMovement().y<-.12)p.setDeltaMovement(p.getDeltaMovement().multiply(1,0,1).add(0,-.12,0));p.hurtMarked=true;p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,25,0,false,false));}
        }
    }
    private static void grantFlight(ServerPlayer p,CompoundTag n){if(!p.getAbilities().mayfly){n.putBoolean("flightGranted",true);p.getAbilities().mayfly=true;p.onUpdateAbilities();}}
    public static void end(ServerPlayer p,int group){CompoundTag n=data(p);if(n.getInt("effect"+group)>0||group==1&&n.getBoolean("flightGranted"))n.putInt("cooldown"+group,1200);n.putInt("effect"+group,0);p.removeEffect(EFFECTS.get(group).get());if(group==1&&n.getBoolean("flightGranted")){n.putBoolean("flightGranted",false);if(!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.onUpdateAbilities();n.putBoolean("landing",!p.onGround());}}}
    public static boolean triple(ServerPlayer p,PlayerState s){if(!addicted(p))return false;if(s.addictionStrikes==0)s.addictionStrikes=p.getRandom().nextBoolean()?3:1;return s.addictionStrikes==3;}
    @SubscribeEvent public static void hurt(LivingHurtEvent e){if(e.getSource().getEntity() instanceof ServerPlayer p&&active(p,0)&&(e.getEntity().getMobType()==MobType.UNDEAD||e.getEntity().getType()==EntityType.ENDER_DRAGON)&&e.getSource().getDirectEntity()!=null)e.setAmount(e.getAmount()+5);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void leaves(BlockEvent.BreakEvent e){if(!(e.getPlayer() instanceof ServerPlayer p)||p.isCreative()||!active(p,2)||!e.getState().is(BlockTags.LEAVES)||!e.getState().hasProperty(BlockStateProperties.PERSISTENT)||e.getState().getValue(BlockStateProperties.PERSISTENT)||p.getMainHandItem().getItem() instanceof ShearsItem||EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH,p.getMainHandItem())>0)return;if(p.getRandom().nextFloat()<.3)Block.popResource(p.level(),e.getPos(),new ItemStack(Items.APPLE,2));}
    @SubscribeEvent public static void foodStart(LivingEntityUseItemEvent.Start e){if(e.getEntity() instanceof Player p)FoodRestriction.bind(p);}
    @SubscribeEvent public static void foodFinish(LivingEntityUseItemEvent.Finish e){if(e.getEntity() instanceof ServerPlayer p&&e.getItem().is(Items.MILK_BUCKET))for(int i=0;i<4;i++)end(p,i);}
    @SubscribeEvent public static void cure(MobEffectEvent.Remove e){if(e.getEntity() instanceof ServerPlayer p)for(int i=0;i<EFFECTS.size();i++)if(e.getEffect()==EFFECTS.get(i).get()&&data(p).getInt("effect"+i)>0)end(p,i);}
    @SubscribeEvent public static void rottenEffect(MobEffectEvent.Applicable e){if(e.getEntity() instanceof Player p&&addicted(p)&&FoodRestriction.rottenMeal(p)&&e.getEffectInstance().getEffect()==MobEffects.HUNGER)e.setResult(Event.Result.DENY);}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p){for(int i=0;i<4;i++)end(p,i);data(p).putBoolean("landing",false);}}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){e.getEntity().getPersistentData().put("embers_benefits",e.getOriginal().getPersistentData().getCompound("embers_benefits").copy());}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){CompoundTag n=data(p);if(n.getInt("effect1")>0)grantFlight(p,n);FoodRestriction.bind(p);}}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)end(p,1);}
}
