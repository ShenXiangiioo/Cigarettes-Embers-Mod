package world.embers.qa;
import world.embers.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(Embers.ID) @PrefixGameTestTemplate(false)
public class MobSmokingTests {
    @GameTest(template="empty") public static void all_five_smoking_mobs_pause_without_disabling_ai(GameTestHelper h){
        for(EntityType<? extends Mob> type:List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.ZOMBIE_VILLAGER,EntityType.VILLAGER)){
            FakePlayer p=player(h);Mob m=mob(h,type);m.setNoAi(false);m.setInvulnerable(true);m.setNoGravity(true);double x=m.getX(),z=m.getZ();
            try{
                h.assertTrue(MobSmoking.give(p,m)&&MobSmoking.light(p,m),"Gift and fire accepted: "+type);
                for(int tick=0;tick<20;tick++){m.getMoveControl().setWantedPosition(x+5,m.getY(),z+5,1);m.tick();}
                h.assertTrue(Math.abs(m.getX()-x)<.001&&Math.abs(m.getZ()-z)<.001&&!m.isNoAi(),"Smoking pauses actual AI movement without a NoAI flag: "+type);
                MobSmoking.finish(m,false);h.assertTrue(!m.isNoAi(),"Normal AI stays enabled after the cigarette ends: "+type);
            }finally{MobSmoking.finish(m,false);m.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void merchant_display_survives_local_and_network_delivery(GameTestHelper h){
        FakePlayer p=player(h);Villager v=mob(h,EntityType.VILLAGER);
        try{
            v.getOffers().clear();v.getOffers().add(new MerchantOffer(new ItemStack(Items.WHEAT,20),new ItemStack(Items.EMERALD),20,1,0));v.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD,8),new ItemStack(Items.APPLE,3),20,1,0));
            v.setTradingPlayer(p);MobSmoking.give(p,v);MobSmoking.light(p,v);MobSmoking.data(v).putInt("discount",3);
            var displayed=MobSmoking.displayOffers(v.getOffers());h.assertTrue(displayed.get(0).getCostA().getCount()==17&&displayed.get(1).getCostA().getCount()==5,"Local packet has concrete discounted costs without live owner");
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try{displayed.writeToStream(buffer);var remote=MerchantOffers.createFromStream(buffer);h.assertTrue(remote.get(0).getCostA().getCount()==17&&remote.get(1).getCostA().getCount()==5,"Remote packet preserves both prices");}finally{buffer.release();}
            v.setTradingPlayer(null);h.assertTrue(displayed.get(0).getCostA().getCount()==17&&v.getOffers().get(0).getCostA().getCount()==20,"Display snapshot cannot mutate authoritative live prices or depend on a later trader");
        }finally{v.setTradingPlayer(null);MobSmoking.finish(v,false);v.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void accepted_gift_cancels_primed_creeper(GameTestHelper h){FakePlayer p=player(h);Creeper c=mob(h,EntityType.CREEPER);try{var tag=c.saveWithoutId(new net.minecraft.nbt.CompoundTag());tag.putShort("Fuse",(short)2);c.load(tag);c.setTarget(p);c.setSwellDir(1);c.tick();h.assertTrue(c.isAlive(),"One tick before explosion");h.assertTrue(MobSmoking.give(p,c),"Gift accepted");c.tick();h.assertTrue(c.isAlive()&&c.getSwellDir()<0,"Gift resets a primed fuse before vanilla explosion");}finally{MobSmoking.finish(c,false);c.discard();}h.succeed();}
    static FakePlayer player(GameTestHelper h){FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"MobTest"));BlockPos at=h.absolutePos(new BlockPos(2,1,2));p.setPos(at.getX(),at.getY(),at.getZ());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Embers.PACKS.get(Brand.CABALLERO_GREEN).get()));return p;}
    static <T extends Mob>T mob(GameTestHelper h,EntityType<T> type){T e=type.create(h.getLevel());BlockPos at=h.absolutePos(new BlockPos(2,1,2));e.setPos(at.getX(),at.getY(),at.getZ()+1);e.setNoAi(true);h.getLevel().addFreshEntity(e);return e;}
    @GameTest(template="empty") public static void smoke_aura_is_per_giver_type_and_distance(GameTestHelper h){FakePlayer a=player(h),b=player(h);Mob z=mob(h,EntityType.ZOMBIE),near=mob(h,EntityType.ZOMBIE),skeleton=mob(h,EntityType.SKELETON);try{h.assertTrue(MobSmoking.give(a,z),"Gift accepted");h.assertTrue(MobSmoking.protectedFrom(z,a)&&!MobSmoking.protectedFrom(near,a),"Unlit grace is anchor only");h.assertTrue(!MobSmoking.protectedFrom(z,b),"Other player not protected");h.assertTrue(MobSmoking.light(a,z),"Light granted");h.assertTrue(MobSmoking.protectedFrom(near,a)&&!MobSmoking.protectedFrom(skeleton,a)&&!MobSmoking.protectedFrom(near,b),"Lit aura protects same type for giver only");near.setPos(z.getX()+20.1,z.getY(),z.getZ());h.assertTrue(!MobSmoking.protectedFrom(near,a),"Radius limit is twenty");near.setPos(z.getX()+20,z.getY(),z.getZ());h.assertTrue(MobSmoking.protectedFrom(near,a),"Twenty-block boundary included");}finally{MobSmoking.finish(z,false);z.discard();near.discard();skeleton.discard();}h.succeed();}
    @GameTest(template="empty") public static void unlit_timeout_drop_once_and_reoffer_cooldown(GameTestHelper h){FakePlayer p=player(h);Mob mob=mob(h,EntityType.CREEPER);try{int before=h.getLevel().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(3)).size();h.assertTrue(MobSmoking.give(p,mob),"Gift");MobSmoking.data(mob).putLong("expires",h.getLevel().getGameTime()-1);h.assertTrue(!MobSmoking.protectedFrom(mob,p)&&!MobSmoking.light(p,mob),"Expired cigarette cannot protect or light");MobSmoking.finish(mob,true);MobSmoking.finish(mob,true);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(3));h.assertTrue(drops.size()==before+1&&drops.stream().anyMatch(x->x.getItem().is(Embers.CIGARETTES.get(Brand.CABALLERO_GREEN).get())),"One matching unlit cigarette returned");h.assertTrue(!MobSmoking.give(p,mob)&&SmokingItem.remaining(p.getMainHandItem())==9,"Cooldown rejects without consuming");}finally{MobSmoking.finish(mob,false);mob.discard();}h.succeed();}
    @GameTest(template="empty") public static void villager_discount_has_no_permanent_price_change(GameTestHelper h){FakePlayer a=player(h),b=player(h);Villager v=mob(h,EntityType.VILLAGER);try{var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemStack(Items.EMERALD,10),new ItemStack(Items.DIAMOND),20,1,0));v.getOffers().clear();v.getOffers().addAll(offers);v.setTradingPlayer(a);MobSmoking.give(a,v);MobSmoking.light(a,v);MobSmoking.data(v).putInt("discount",3);var offer=v.getOffers().get(0);h.assertTrue(offer.getCostA().getCount()==7&&offer.getSpecialPriceDiff()==-3,"UI and live payment share the session discount");h.assertTrue(offer.createTag().getInt("specialPrice")==0,"Discount is never written into permanent trade data");v.setTradingPlayer(b);h.assertTrue(offer.getCostA().getCount()==10&&offer.getSpecialPriceDiff()==0,"Different trader pays normal price");v.setTradingPlayer(a);MobSmoking.data(v).putLong("expires",h.getLevel().getGameTime()-1);h.assertTrue(offer.getCostA().getCount()==10,"Expiry restores current payment price");}finally{v.setTradingPlayer(null);MobSmoking.finish(v,false);v.discard();}h.succeed();}
    @GameTest(template="empty") public static void expired_discount_cannot_take_cached_trade_output(GameTestHelper h){FakePlayer a=player(h);Villager v=mob(h,EntityType.VILLAGER);try{var offers=new MerchantOffers();offers.add(new MerchantOffer(new ItemStack(Items.EMERALD,10),new ItemStack(Items.DIAMOND),20,1,0));v.getOffers().clear();v.getOffers().addAll(offers);v.setTradingPlayer(a);MobSmoking.give(a,v);MobSmoking.light(a,v);MobSmoking.data(v).putInt("discount",2);v.getOffers();MerchantMenu menu=new MerchantMenu(1,a.getInventory(),v);menu.getSlot(0).set(new ItemStack(Items.EMERALD,8));h.assertTrue(menu.getSlot(2).hasItem(),"Discounted result visible");MobSmoking.data(v).putLong("expires",h.getLevel().getGameTime()-1);menu.clicked(2,0,ClickType.PICKUP,a);h.assertTrue(menu.getCarried().isEmpty()&&!menu.getSlot(2).hasItem()&&menu.getSlot(0).getItem().getCount()==8,"Normal click revalidates expiry before extraction");h.assertTrue(menu.quickMoveStack(a,2).isEmpty()&&a.getInventory().countItem(Items.DIAMOND)==0,"Shift click cannot extract stale output");}finally{v.setTradingPlayer(null);MobSmoking.finish(v,false);v.discard();}h.succeed();}
    @GameTest(template="empty") public static void attack_removes_only_attackers_protection(GameTestHelper h){FakePlayer a=player(h),b=player(h);Mob za=mob(h,EntityType.ZOMBIE),zb=mob(h,EntityType.ZOMBIE);try{MobSmoking.give(a,za);MobSmoking.light(a,za);MobSmoking.give(b,zb);MobSmoking.light(b,zb);MobSmoking.attacks(new net.minecraftforge.event.entity.living.LivingAttackEvent(za,h.getLevel().damageSources().playerAttack(a),1));h.assertTrue(!MobSmoking.protectedFrom(za,a)&&MobSmoking.protectedFrom(za,b),"Aggression breaks only its owner's truce");}finally{MobSmoking.finish(za,false);MobSmoking.finish(zb,false);za.discard();zb.discard();}h.succeed();}
}
