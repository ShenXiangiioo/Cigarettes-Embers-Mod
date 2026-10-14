package world.embers;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder(Embers.ID)
@PrefixGameTestTemplate(false)
public class EmbersGameTests {
    @GameTest(template="empty") public static void continuous_lighter_ten_second_fuel(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get());p.setItemInHand(InteractionHand.MAIN_HAND,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<6;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("lighter_burn")&&LighterItem.fuel(lighter)==90,"Long hold must open a steady flame without an upfront ignition debit");
        for(int i=0;i<199;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(LighterItem.fuel(lighter)==90&&LighterItem.flameTicks(lighter)==199,"No full use before 200 burning ticks");
        ServerEvents.actionTick(p,s);h.assertTrue(LighterItem.fuel(lighter)==89&&LighterItem.flameTicks(lighter)==0,"Exactly one use at ten seconds");
        for(int i=0;i<200;i++)ServerEvents.actionTick(p,s);h.assertTrue(LighterItem.fuel(lighter)==88&&s.action.equals("lighter_burn"),"Flame must persist through twenty seconds");
        ServerEvents.input(p,new Network.Input(1,false,-1));h.assertTrue(s.action.equals("idle"),"Release extinguishes immediately");h.succeed();
    }
    @GameTest(template="empty") public static void continuous_lighter_fraction_survives_release_and_reload(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.JET_BLACK).get());p.setItemInHand(InteractionHand.MAIN_HAND,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<106;i++)ServerEvents.actionTick(p,s);ServerEvents.input(p,new Network.Input(1,false,-1));
        ItemStack loaded=ItemStack.of(lighter.save(new net.minecraft.nbt.CompoundTag()));p.setItemInHand(InteractionHand.MAIN_HAND,loaded);s.lastRequest=-100;
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<106;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(LighterItem.fuel(loaded)==89&&LighterItem.flameTicks(loaded)==0,"Two five-second burns still cost one use after NBT reload");h.succeed();
    }
    @GameTest(template="empty") public static void last_lighter_use_extinguishes_at_ten_seconds(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.PIEZO).get());lighter.getOrCreateTag().putInt("fuel",1);p.setItemInHand(InteractionHand.MAIN_HAND,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<205;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("lighter_burn")&&LighterItem.fuel(lighter)==1,"Last use provides a full ten seconds");ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("idle")&&LighterItem.fuel(lighter)==0,"No flame remains on an empty lighter");for(int i=0;i<400;i++)ServerEvents.actionTick(p,s);h.assertTrue(LighterItem.fuel(lighter)==0,"Fuel cannot underflow");h.succeed();
    }
    @GameTest(template="empty") public static void tap_retains_fidget_and_cancel_preserves_partial_fuel(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get());p.setItemInHand(InteractionHand.MAIN_HAND,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));ServerEvents.input(p,new Network.Input(1,false,-1));h.assertTrue(s.action.equals("fidget"),"Short tap retains fidget");for(int i=0;i<60;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(LighterItem.fuel(lighter)==89,"Fidget ignition spends one use");s.lastRequest=-100;ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<49;i++)ServerEvents.actionTick(p,s);
        ServerEvents.input(p,new Network.Input(2,false,-1));for(int i=0;i<220;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("idle")&&LighterItem.flameTicks(lighter)==43&&LighterItem.fuel(lighter)==89,"Cancel freezes fractional fuel without further ticks");h.succeed();
    }
    @GameTest(template="empty") public static void changing_slot_extinguishes_held_lighter(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_CHROME).get());p.setItemInHand(InteractionHand.MAIN_HAND,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<30;i++)ServerEvents.actionTick(p,s);p.getInventory().selected=1;ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("idle")&&LighterItem.flameTicks(lighter)==24,"Switching slots stops burning before charging another tick");h.succeed();
    }
    @GameTest(template="empty") public static void refill_clears_partial_burn_and_preserves_old_remaining(GameTestHelper h){
        for(LighterKind kind:LighterKind.values()){
            ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(kind).get());h.assertTrue(LighterItem.fuel(lighter)==90,"All eight new lighters have 90 uses");
            lighter.getOrCreateTag().putInt("fuel",37);h.assertTrue(LighterItem.fuel(lighter)==37,"Existing remaining fuel is not silently replenished");
            lighter.getOrCreateTag().putInt("flameTicks",199);lighter.getOrCreateTag().putString("owner","kept");LighterItem.refill(lighter);
            h.assertTrue(LighterItem.fuel(lighter)==90&&LighterItem.flameTicks(lighter)==0&&lighter.getTag().getString("owner").equals("kept"),"Refill clears fractional consumption only");
            LighterItem.burnTick(lighter);h.assertTrue(LighterItem.needsRefill(lighter),"A partially burnt first use can also be refilled");
        }h.succeed();
    }

    @GameTest(template="empty") public static void classic_geometry_and_burn_profiles(GameTestHelper h){
        Brand classic=Brand.ZHONGHUA,slim=Brand.ZHONGHUA_SLIM;
        h.assertTrue(classic.cigaretteRadius()>.0067f&&classic.cigaretteLength(1)<.134f,"Classic cigarettes must be thicker and shorter than the prior model");
        h.assertTrue(classic.cigaretteRadius()>slim.cigaretteRadius()&&classic.cigaretteLength(1)<slim.cigaretteLength(1),"Classic and slim geometry must differ");
        h.assertTrue((classic.burnTicks()-300)*5==(slim.burnTicks()-300)*6,"Both profiles add fifteen seconds after the classic duration multiplier");
        h.assertTrue(Brand.CABALLERO_BLACK.burnTicks()==slim.burnTicks()&&Brand.CABALLERO_BLACK.cigaretteRadius()==.0049f,"Horizontal case profile must stay unchanged");h.succeed();
    }
    @GameTest(template="empty") public static void fresh_classic_from_pack_and_single_share_profile(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Embers.PACKS.get(Brand.CHAPMAN).get()));
        p.getInventory().setItem(1,new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get()));ServerEvents.input(p,new Network.Input(0,false,-1));
        for(int i=0;i<30;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.burn==Brand.CHAPMAN.burnTicks()&&s.burnMax==s.burn,"Pack preparation must use classic burn time");
        PlayerState single=new PlayerState();ServerEvents.restoreCigarette(single,new ItemStack(Embers.CIGARETTES.get(Brand.CHAPMAN).get()));
        h.assertTrue(single.burn==s.burn&&single.burnMax==s.burnMax,"A fresh single cigarette must use the same profile as its pack");h.succeed();
    }
    @GameTest(template="empty") public static void partial_classic_survives_return_reload_and_relight(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=Brand.ZHONGHUA.ordinal();s.burnMax=Brand.ZHONGHUA.burnTicks();s.burn=s.burnMax/2;s.lit=false;s.inhaleTotal=33;
        PlayerState copy=PlayerState.load(s.save());h.assertTrue(copy.remaining()==.5f&&copy.burn==s.burn,"Save/load must preserve classic fraction and remaining time");
        s.start("mouth_hold",8);ServerEvents.input(p,new Network.Input(1,false,-1));ItemStack returned=p.getInventory().getItem(0);
        h.assertTrue(SmokingItem.burnFraction(returned)==.5f&&SmokingItem.burnMax(returned)==copy.burnMax,"Returning to inventory must preserve denominator");
        ServerEvents.restoreCigarette(s,returned);h.assertTrue(s.burn==copy.burn&&s.burnMax==copy.burnMax&&s.inhaleTotal==33,"Re-lighting must not restore consumed tobacco");h.succeed();
    }
    @GameTest(template="empty") public static void legacy_partial_cigarette_keeps_remaining_time(GameTestHelper h){
        int base=Embers.BURN_SECONDS.get()*20;ItemStack item=new ItemStack(Embers.CIGARETTES.get(Brand.ZHONGHUA).get());item.getOrCreateTag().putInt("burn",base/2);
        PlayerState s=new PlayerState();ServerEvents.restoreCigarette(s,item);h.assertTrue(s.burn==base/2&&s.burnMax==base&&s.remaining()==.5f,"Legacy item must keep time and fraction without refilling");
        var n=s.save();n.remove("burnMax");PlayerState loaded=PlayerState.load(n);h.assertTrue(loaded.burn==base/2&&loaded.burnMax==base&&loaded.remaining()==.5f,"Legacy mouth save must migrate without restoring burn time");h.succeed();
    }
    @GameTest(template="empty") public static void ashtray_accepts_twenty_and_empties(GameTestHelper h){
        BlockPos at=new BlockPos(1,1,1);h.setBlock(at,Embers.ASHTRAY.get());AshtrayEntity tray=(AshtrayEntity)h.getBlockEntity(at);
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;s.burn=900;tray.count=19;
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tray.getBlockPos()),net.minecraft.core.Direction.UP,tray.getBlockPos(),false);
        var block=(AshtrayBlock)tray.getBlockState().getBlock();
        block.use(tray.getBlockState(),h.getLevel(),tray.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tray.count==19&&s.mouth==0,"Tray transfer waits for contact");for(int i=0;i<34;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(tray.count==20&&s.mouth==-1,"Twentieth butt must be accepted");
        s.mouth=1;s.lit=true;s.burn=700;
        block.use(tray.getBlockState(),h.getLevel(),tray.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tray.count==20&&s.mouth==1&&s.lit,"Full tray must preserve the next cigarette");
        p.setShiftKeyDown(true);block.use(tray.getBlockState(),h.getLevel(),tray.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tray.count==0&&p.getInventory().countItem(Embers.BUTT.get())==20,"Emptying must return all twenty butts once");h.succeed();
    }
    @GameTest(template="empty") public static void discard_mouth_clears_once_and_respects_held_item(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=1;s.burn=1000;
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));ServerEvents.input(p,new Network.Input(3,false,-1));
        h.assertTrue(s.mouth==1,"Drop-mouth packet must not discard a mouth cigarette while another item is held");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);ServerEvents.input(p,new Network.Input(3,false,-1));
        h.assertTrue(s.mouth==1&&s.action.equals("toss"),"Discard begins a removal animation");for(int i=0;i<28;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.mouth==-1&&s.action.equals("idle")&&s.actionItem.isEmpty(),"Release must clear mouth and stale render state");
        ServerEvents.input(p,new Network.Input(3,false,-1));h.assertTrue(s.mouth==-1,"Repeated packet must not restore or duplicate a cigarette");h.succeed();
    }
    @GameTest(template="empty") public static void rejected_use_keeps_no_render_snapshot(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Embers.PACKS.get(Brand.HETIANXIA).get()));
        ServerEvents.input(p,new Network.Input(0,false,-1));
        h.assertTrue(s.action.equals("idle")&&s.actionItem.isEmpty(),"Rejected use must not leave a ghost pack/cigarette snapshot");h.succeed();
    }
    @GameTest(template="empty") public static void dropping_during_preparation_cancels(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.CABALLERO_BLACK).get());
        p.setItemInHand(InteractionHand.MAIN_HAND,pack);p.getInventory().setItem(1,new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get()));
        ServerEvents.input(p,new Network.Input(0,false,-1));p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        for(int i=0;i<65;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("idle")&&s.actionItem.isEmpty()&&s.mouth==-1&&SmokingItem.remaining(pack)==10,"Dropped items must not keep animating or consume contents");h.succeed();
    }
    @GameTest(template="empty") public static void second_press_cancels_without_consuming(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.CABALLERO_BLACK).get());
        p.setItemInHand(InteractionHand.MAIN_HAND,pack);ServerEvents.input(p,new Network.Input(0,true,-1));
        h.assertTrue(s.action.equals("offer")&&s.actionDuration==60,"Offer must last exactly three seconds");
        ServerEvents.input(p,new Network.Input(0,true,-1));
        h.assertTrue(s.action.equals("idle")&&SmokingItem.remaining(pack)==10,"Second right press must cancel even during input cooldown");h.succeed();
    }
    @GameTest(template="empty") public static void cancel_keeps_burning_cigarette(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=2;s.lit=true;s.burn=800;s.inhaleTotal=43;s.start("exhale",40);s.exhaleTicks=40;
        ServerEvents.input(p,new Network.Input(2,false,-1));
        h.assertTrue(s.action.equals("idle")&&s.exhaleTicks==0&&s.inhaleTicks==0,"Cancellation must remove the pending action and smoke burst");
        h.assertTrue(s.mouth==2&&s.lit&&s.burn==800&&s.inhaleTotal==43,"Cancel must not delete the mouth cigarette or reset its burn");h.succeed();
    }
    @GameTest(template="empty") public static void lighting_in_mouth_finishes_within_three_seconds(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.burn=2400;s.stress=95;
        ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_CHROME).get());p.getInventory().setItem(1,lighter);
        ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<8;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.action.equals("lighting"),"A held use must begin ignition after eight ticks");s.retry=true;
        for(int i=0;i<30;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.lit&&s.action.equals("idle")&&LighterItem.fuel(lighter)==LighterItem.MAX_FUEL-1,"Even a retry must finish in 38 ticks and spend one fuel");h.succeed();
    }
    @GameTest(template="empty") public static void cancel_before_strike_preserves_fuel(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.burn=1200;ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get());p.getInventory().setItem(1,lighter);
        ServerEvents.beginLighting(p,s);for(int i=0;i<7;i++)ServerEvents.actionTick(p,s);ServerEvents.input(p,new Network.Input(2,false,-1));
        for(int i=0;i<80;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(!s.lit&&s.mouth==0&&LighterItem.fuel(lighter)==LighterItem.MAX_FUEL,"Canceled ignition must not later consume fuel or ignite");h.succeed();
    }
    @GameTest(template="empty") public static void only_production_tools_are_craftable(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.getId().getNamespace().equals(Embers.ID)&&r.getType()==net.minecraft.world.item.crafting.RecipeType.CRAFTING).toList();
        h.assertTrue(recipes.size()==3&&recipes.stream().allMatch(r->java.util.Set.of("refill_station","drying_rack","processing_machine").contains(r.getId().getPath())),"Only the three authorized production tools have crafting recipes");
        var r=recipes.stream().filter(x->x.getId().getPath().equals("refill_station")).findFirst().orElseThrow();h.assertTrue(r.getIngredients().size()==3&&r.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.IRON_BLOCK)))&&r.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.FLINT)))&&r.getIngredients().stream().anyMatch(i->i.test(new ItemStack(Items.CAMPFIRE))),"Station recipe must use exactly iron block, flint and campfire");h.succeed();
    }
    @GameTest(template="empty") public static void quick_click_inhales_before_exhaling(GameTestHelper h){FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;s.burn=2400;ServerEvents.input(p,new Network.Input(0,false,-1));ServerEvents.input(p,new Network.Input(1,false,-1));h.assertTrue(s.action.equals("inhale")&&s.actionDuration==8,"Same-tick release must retain a short inhalation");for(int i=0;i<8;i++){s.inhaleTicks++;s.inhaleTotal++;ServerEvents.actionTick(p,s);}h.assertTrue(s.action.equals("exhale")&&s.inhaleTotal==8&&s.exhaleTicks==18,"Short puff must count and release proportionate smoke");h.succeed();}
    @GameTest(template="empty") public static void animals_daily_quota_fish_and_legacy_state(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ServerEvents events=new ServerEvents();
        for(var type:java.util.List.of(EntityType.COW,EntityType.PIG,EntityType.SHEEP,EntityType.CHICKEN,EntityType.WOLF))h.assertTrue(ServerEvents.harmlessAnimal(type.create(h.getLevel())),"Livestock and dogs count");
        for(var type:java.util.List.of(EntityType.COD,EntityType.SALMON,EntityType.TROPICAL_FISH,EntityType.PUFFERFISH,EntityType.ZOMBIE))h.assertTrue(!ServerEvents.harmlessAnimal(type.create(h.getLevel())),"Fish and monsters are excluded");
        var cow=EntityType.COW.create(h.getLevel());var hit=new net.minecraftforge.event.entity.living.LivingDeathEvent(cow,p.damageSources().playerAttack(p));
        for(int i=0;i<2;i++)events.death(hit);h.assertTrue(s.stress==0&&s.animalKills==2,"Fewer than three kills must not grant stress");
        events.death(hit);h.assertTrue(s.stress==5,"Third animal grants five");for(int i=0;i<27;i++)events.death(hit);h.assertTrue(s.stress==20&&s.gains[6]==20,"Daily animal cap is twenty");
        s.day--;events.death(hit);events.death(hit);h.assertTrue(s.animalKills==2&&s.stress==20,"New day resets both partial count and quota");events.death(hit);h.assertTrue(s.stress==25,"New day can grant again");
        var n=s.save();n.putIntArray("gains",new int[]{4,3,2,1,5,6});PlayerState loaded=PlayerState.load(n);h.assertTrue(loaded.gains[0]==4&&loaded.gains[5]==6&&loaded.gains[6]==0,"Old six-source saves keep existing quotas");h.succeed();
    }
    @GameTest(template="empty") public static void disposal_cancel_and_missing_tray_keep_cigarette(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;s.burn=1100;s.inhaleTotal=140;s.stress=50;
        ServerEvents.beginDisposal(p,null);for(int i=0;i<9;i++)ServerEvents.actionTick(p,s);ServerEvents.input(p,new Network.Input(2,false,-1));
        for(int i=0;i<40;i++)ServerEvents.actionTick(p,s);h.assertTrue(s.mouth==0&&s.lit&&s.stress==50,"Cancel before release must preserve cigarette and relief quota");
        BlockPos pos=h.absolutePos(new BlockPos(1,1,1));ServerEvents.beginDisposal(p,pos);for(int i=0;i<34;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(s.mouth==0&&s.stress==50&&s.action.equals("idle"),"Removed tray must not consume the cigarette");h.succeed();
    }
    @GameTest(template="empty") public static void all_trays_accept_held_butts_and_preserve_excess(GameTestHelper h){
        FakePlayer p=player(h);for(var block:java.util.List.of(Embers.ASHTRAY.get(),Embers.GLASS_ASHTRAY.get(),Embers.BRASS_ASHTRAY.get())){
            BlockPos at=new BlockPos(1,1,1);h.setBlock(at,block);AshtrayEntity be=(AshtrayEntity)h.getBlockEntity(at);be.count=19;
            ItemStack held=new ItemStack(Embers.BUTT.get(),2);p.setItemInHand(InteractionHand.MAIN_HAND,held);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()),net.minecraft.core.Direction.UP,be.getBlockPos(),false);
            for(int i=0;i<3;i++)((AshtrayBlock)block).use(be.getBlockState(),h.getLevel(),be.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(be.count==20&&held.getCount()==1,"Deposit must conserve one excess butt for each style");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void butt_falls_settles_and_persists(GameTestHelper h){
        BlockPos at=new BlockPos(2,1,2);h.setBlock(at,net.minecraft.world.level.block.Blocks.STONE);var pos=h.absolutePos(at);
        ButtEntity butt=new ButtEntity(Embers.BUTT_ENTITY.get(),h.getLevel());butt.setItem(new ItemStack(Embers.BUTT.get()));butt.setPos(pos.getX()+.5,pos.getY()+1.8,pos.getZ()+.5);butt.setDeltaMovement(.006,.05,0);butt.setNeverPickUp();h.getLevel().addFreshEntity(butt);
        h.runAfterDelay(50,()->{h.assertTrue(butt.onGround()&&butt.getDeltaMovement().horizontalDistanceSqr()<.000001,"Paper butt must settle on its supporting block");var n=new net.minecraft.nbt.CompoundTag();butt.save(n);h.assertTrue(n.getString("id").equals("embers:cigarette_butt"),"Ground physics entity must persist with its type");h.succeed();});
    }
    @GameTest(template="empty") public static void exhale_strength_scales_with_inhalation(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;s.start("inhale",81);s.inhaleTicks=8;ServerEvents.input(p,new Network.Input(1,false,-1));float shortPuff=s.exhaleStrength;int shortDuration=s.exhaleTicks;
        s.start("inhale",81);s.inhaleTicks=65;ServerEvents.input(p,new Network.Input(1,false,-1));h.assertTrue(s.exhaleStrength>shortPuff*5&&s.exhaleTicks>shortDuration*3,"Long inhale must release a denser, larger and longer breath");h.assertTrue(PlayerState.load(s.save()).exhaleStrength==s.exhaleStrength,"Breath strength must reach other clients");h.succeed();
    }
    @GameTest(template="empty") public static void ashtray_residual_smoke_expires_without_reheating(GameTestHelper h){
        BlockPos at=new BlockPos(1,1,1);h.setBlock(at,Embers.GLASS_ASHTRAY.get());AshtrayEntity be=(AshtrayEntity)h.getBlockEntity(at);long now=h.getLevel().getGameTime();
        be.deposit(true);be.deposit(false);h.assertTrue(be.warmth(0,now)==40&&be.warmth(1,now)==0,"Only a freshly extinguished cigarette should smolder");
        AshtrayEntity copy=new AshtrayEntity(be.getBlockPos(),be.getBlockState());copy.load(be.getUpdateTag());h.assertTrue(copy.warmth(0,now+20)==20&&copy.warmth(0,now+41)==0,"Chunk/network reload must preserve the original expiry, never restart it");
        var source=be.buttPose(0).ash();h.assertTrue(source.x>0&&source.x<1&&source.z>0&&source.z<1,"Residual smoke must start inside the matching butt layout");
        be.empty();be.deposit(false);h.assertTrue(be.warmth(0,now)==0,"Emptying and adding a cold butt must not inherit residual smoke");h.succeed();
    }
    @GameTest(template="empty") public static void configurable_pressure_round_trip_and_invalid_requests(GameTestHelper h){
        PressureSettings config=new PressureSettings();h.assertTrue(config.configure(1,6,9,23)&&config.configure(2,0,17,0),"Valid changes rejected");
        h.assertTrue(!config.configure(1,7,4,4)&&!config.configure(1,0,-1,5)&&!config.configure(2,0,101,5),"Invalid config accepted");config.enabled=false;
        PressureSettings copy=PressureSettings.load(config.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(!copy.enabled&&copy.gain[6]==9&&copy.caps[6]==23&&copy.smokingRelief(true,1)==17,"World settings must persist");
        PlayerState s=new PlayerState();for(int i=0;i<10;i++)s.addStress(6,copy.gain[6],copy.caps[6]);h.assertTrue(s.stress==23&&s.gains[6]==23,"Configured cap must clip the last grant");h.succeed();
    }
    @GameTest(template="empty") public static void pressure_off_clears_effects_and_stops_counting(GameTestHelper h){
        FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);PressureSettings config=PressureSettings.get(p.server);boolean enabled=config.enabled;
        try{config.enabled=false;s.stress=100;s.animalKills=2;s.ores=7;s.gains[6]=15;s.reverseTicks=30;
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(Embers.COLLAPSE.get(),40));PressureEffects.apply(p,net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,30,false);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,1200,1));
            ServerEvents.applyPressureSwitch(p,false);h.assertTrue(s.stress==0&&!s.stressEnabled&&s.animalKills==0&&s.ores==0&&s.gains[6]==0&&s.reverseTicks==0,"Disabling must clear all pressure counters");
            h.assertTrue(!p.hasEffect(Embers.COLLAPSE.get())&&!p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)&&p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),"Remove pressure effects but preserve unrelated potion");
            var event=new net.minecraftforge.event.entity.living.LivingDeathEvent(EntityType.COW.create(h.getLevel()),p.damageSources().playerAttack(p));for(int i=0;i<12;i++)new ServerEvents().death(event);ServerEvents.addStress(p,0);s.addStress(3,2,100);
            h.assertTrue(s.stress==0&&s.animalKills==0&&s.gains[3]==0,"Off means no background accumulation");h.assertTrue(!PlayerState.load(s.save()).stressEnabled,"Off state must sync and persist");
        }finally{config.enabled=enabled;}h.succeed();
    }
    @GameTest(template="empty") public static void paid_station_without_commerce_preserves_inputs(GameTestHelper h){
        h.setBlock(new BlockPos(1,1,1),Embers.STATION.get());StationEntity be=(StationEntity)h.getBlockEntity(new BlockPos(1,1,1));be.fee=7;FakePlayer p=player(h);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get());lighter.getOrCreateTag().putInt("fuel",0);be.setItem(0,lighter);be.setItem(1,new ItemStack(Items.LAVA_BUCKET));
        h.assertTrue(!new StationMenu(1,p.getInventory(),be,be.getBlockPos()).clickMenuButton(p,0)&&LighterItem.fuel(lighter)==0&&be.getItem(1).is(Items.LAVA_BUCKET),"Unavailable payment must not consume items");
        AdminNetwork.apply(p,new AdminNetwork.Request(4,be.getBlockPos(),0,0,0,0,99));h.assertTrue(be.fee==7,"Unauthorized station change accepted");h.succeed();
    }
    @GameTest(template="empty") public static void archon_extended_filter_and_rod(GameTestHelper h){
        for(Brand b:Brand.values())if(b.horizontalCase()){
            h.assertTrue(b.cigaretteLength(1)>Brand.ZHONGHUA_SLIM.cigaretteLength(1)&&b.filterLength()>Brand.ZHONGHUA.filterLength(),"Archon rod and filter must both be longer");
            h.assertTrue(b.cigaretteLength(0)==b.filterLength(),"Burn must preserve the extended filter");
        }h.succeed();
    }
    @GameTest(template="empty") public static void mounted_pack_ignition_and_inhale(GameTestHelper h){
        FakePlayer p=player(h);var seat=EntityType.ARMOR_STAND.create(h.getLevel());seat.setPos(p.position());h.getLevel().addFreshEntity(seat);p.startRiding(seat,true);
        h.assertTrue(p.isPassenger(),"Seat fixture must mount the player");PlayerState s=ServerEvents.state(p);
        ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.CABALLERO_GREEN).get());p.setItemInHand(InteractionHand.MAIN_HAND,pack);
        p.getInventory().setItem(1,new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get()));ServerEvents.input(p,new Network.Input(0,false,-1));
        for(int i=0;i<60;i++)ServerEvents.actionTick(p,s);
        h.assertTrue(p.isPassenger()&&s.lit&&s.mouth==1&&SmokingItem.remaining(pack)==9,"Seated player must finish opening and ignition");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);s.lastRequest=-100;ServerEvents.input(p,new Network.Input(0,false,-1));
        h.assertTrue(s.action.equals("inhale"),"Seated inhale must start");ServerEvents.actionTick(p,s);h.assertTrue(s.action.equals("inhale"),"Mounted action must not be canceled next tick");
        ServerEvents.input(p,new Network.Input(2,false,-1));h.assertTrue(s.action.equals("idle")&&s.lit,"Cancel seated inhale preserves the cigarette");p.stopRiding();seat.discard();h.succeed();
    }
    @GameTest(template="empty") public static void occasional_support_is_selected_per_puff(GameTestHelper h){
        FakePlayer p=player(h);p.getRandom().setSeed(703);PlayerState s=ServerEvents.state(p);s.mouth=0;s.lit=true;int supported=0;
        for(int i=0;i<100;i++){s.cancelAction();s.lastRequest=s.lastPuff=-100;ServerEvents.input(p,new Network.Input(0,false,-1));if(s.supportsCigarette())supported++;}
        h.assertTrue(supported>0&&supported<45,"Hand support should occur occasionally, not on every puff");s.cancelAction();h.assertTrue(!s.supportsCigarette(),"Cancellation must remove support pose");h.succeed();
    }
    @GameTest(template="empty") public static void refill_output_blocks_second_order_and_preserves_nbt(GameTestHelper h){
        BlockPos at=new BlockPos(2,1,2);h.setBlock(at,Embers.STATION.get());StationEntity be=(StationEntity)h.getBlockEntity(at);FakePlayer p=player(h);StationMenu menu=new StationMenu(1,p.getInventory(),be,be.getBlockPos());
        ItemStack input=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get());input.getOrCreateTag().putInt("fuel",0);input.getOrCreateTag().putString("ownerMark","retained");
        be.setItem(0,input.copy());be.setItem(1,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(menu.clickMenuButton(p,0),"First order must succeed");
        h.assertTrue(be.getItem(2).getTag().getString("ownerMark").equals("retained"),"Refill must keep custom data");
        be.setItem(0,input.copy());be.setItem(1,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(!menu.clickMenuButton(p,0)&&LighterItem.fuel(be.getItem(0))==0&&be.getItem(1).is(Items.LAVA_BUCKET),"Occupied output must preserve next order");
        StationEntity restored=new StationEntity(be.getBlockPos(),be.getBlockState());restored.load(be.saveWithoutMetadata());h.assertTrue(LighterItem.fuel(restored.getItem(2))==LighterItem.MAX_FUEL&&restored.getItem(0).getTag().getString("ownerMark").equals("retained"),"Pending input and finished output survive reload");
        be.removeItem(2,1);h.assertTrue(menu.clickMenuButton(p,0),"Taking the output must enable the next order");h.succeed();
    }
    @GameTest(template="empty") public static void old_station_inventory_migrates_without_loss(GameTestHelper h){
        BlockPos at=new BlockPos(2,1,2);h.setBlock(at,Embers.STATION.get());StationEntity be=(StationEntity)h.getBlockEntity(at);
        var old=new net.minecraft.nbt.CompoundTag();var slots=net.minecraft.core.NonNullList.withSize(2,ItemStack.EMPTY);slots.set(0,new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get()));slots.set(1,new ItemStack(Items.LAVA_BUCKET));net.minecraft.world.ContainerHelper.saveAllItems(old,slots);old.putLong("fee",7);be.load(old);
        h.assertTrue(be.getContainerSize()==3&&be.getItem(0).getItem() instanceof LighterItem&&be.getItem(1).is(Items.LAVA_BUCKET)&&be.getItem(2).isEmpty()&&be.fee==7,"Old two-slot station must keep items and price");h.succeed();
    }
    @GameTest(template="empty") public static void mixed_butts_round_trip(GameTestHelper h){
        BlockPos at=new BlockPos(1,1,1);h.setBlock(at,Embers.GLASS_ASHTRAY.get());AshtrayEntity tray=(AshtrayEntity)h.getBlockEntity(at);
        for(Brand brand:Brand.values())h.assertTrue(tray.deposit(false,brand),"Deposit style "+brand);
        AshtrayEntity copy=new AshtrayEntity(tray.getBlockPos(),tray.getBlockState());copy.load(tray.getUpdateTag());
        for(int i=0;i<Brand.values().length;i++)h.assertTrue(copy.brand(i)==Brand.values()[i]&&ButtItem.brand(copy.contents().get(i))==Brand.values()[i],"Mixed styles must survive block sync/save and removal");
        var legacy=new net.minecraft.nbt.CompoundTag();legacy.putInt("count",2);copy.load(legacy);h.assertTrue(copy.count==2&&copy.brand(0)==Brand.CHAPMAN,"Legacy unbranded butts keep their original tan appearance");h.succeed();
    }
    @GameTest(template="empty") public static void branded_butt_deposit_and_retrieve(GameTestHelper h){
        BlockPos at=new BlockPos(1,1,1);h.setBlock(at,Embers.BRASS_ASHTRAY.get());AshtrayEntity tray=(AshtrayEntity)h.getBlockEntity(at);FakePlayer p=player(h);
        ItemStack butt=ButtItem.of(Brand.CABALLERO_GREEN);butt.setCount(2);p.setItemInHand(InteractionHand.MAIN_HAND,butt);
        var state=tray.getBlockState();var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(tray.getBlockPos()),net.minecraft.core.Direction.UP,tray.getBlockPos(),false);
        ((AshtrayBlock)state.getBlock()).use(state,h.getLevel(),tray.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tray.count==1&&tray.brand(0)==Brand.CABALLERO_GREEN&&butt.getCount()==1,"Held deposit must retain green filter and consume once");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);((AshtrayBlock)state.getBlock()).use(state,h.getLevel(),tray.getBlockPos(),p,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(tray.count==0&&p.getInventory().items.stream().anyMatch(s->s.is(Embers.BUTT.get())&&ButtItem.brand(s)==Brand.CABALLERO_GREEN),"Empty tray must return matching style");h.succeed();
    }
    static FakePlayer player(GameTestHelper h){FakePlayer p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"EmbersTest"));BlockPos pos=h.absolutePos(new BlockPos(2,2,2));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;}
    @GameTest(template="empty") public static void pack_conservation(GameTestHelper h){ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.ZHONGHUA).get());for(int i=0;i<10;i++)h.assertTrue(SmokingItem.take(pack),"Ten cigarettes must be available");h.assertTrue(!SmokingItem.take(pack)&&SmokingItem.remaining(pack)==0,"Empty pack must not duplicate cigarettes");h.assertTrue(SmokingItem.remaining(ItemStack.of(pack.save(new net.minecraft.nbt.CompoundTag())))==0,"Count must survive NBT round trip");h.succeed();}
    @GameTest(template="empty") public static void refill_transaction(GameTestHelper h){BlockPos at=new BlockPos(2,1,2);h.setBlock(at,Embers.STATION.get());StationEntity be=(StationEntity)h.getBlockEntity(at);FakePlayer p=player(h);ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get());lighter.getOrCreateTag().putInt("fuel",0);be.setItem(0,lighter);be.setItem(1,new ItemStack(Items.LAVA_BUCKET));StationMenu menu=new StationMenu(1,p.getInventory(),be,be.getBlockPos());h.assertTrue(menu.clickMenuButton(p,0),"Refill must succeed");h.assertTrue(LighterItem.fuel(be.getItem(2))==LighterItem.MAX_FUEL&&be.getItem(0).isEmpty()&&be.getItem(1).is(Items.BUCKET),"Fuel and bucket must change together");h.assertTrue(!menu.clickMenuButton(p,0),"Repeated refill packet must not consume or duplicate");be.setItem(1,new ItemStack(Items.LAVA_BUCKET));h.assertTrue(!menu.clickMenuButton(p,0)&&be.getItem(1).is(Items.LAVA_BUCKET),"Full lighter must keep lava");h.succeed();}
    @GameTest(template="empty") public static void finite_fuel(GameTestHelper h){for(LighterKind k:LighterKind.values()){ItemStack s=new ItemStack(Embers.LIGHTERS.get(k).get());for(int i=0;i<LighterItem.MAX_FUEL;i++)h.assertTrue(LighterItem.spend(s),"Ignition "+i+" for "+k);h.assertTrue(!LighterItem.spend(s)&&LighterItem.fuel(s)==0,"Fuel cannot underflow");}h.succeed();}
    @GameTest(template="empty") public static void cancel_and_ignite(GameTestHelper h){FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.HEHUA).get());ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.PIEZO).get());p.setItemInHand(InteractionHand.MAIN_HAND,pack);p.getInventory().setItem(1,lighter);ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<15;i++)ServerEvents.actionTick(p,s);ServerEvents.input(p,new Network.Input(2,false,-1));h.assertTrue(SmokingItem.remaining(pack)==10&&s.mouth==-1,"Canceled preparation must not consume");s.lastRequest=-100;ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<60;i++)ServerEvents.actionTick(p,s);h.assertTrue(SmokingItem.remaining(pack)==9&&s.mouth==Brand.HEHUA.ordinal(),"Exactly one cigarette must move to mouth");for(int i=0;i<100;i++)ServerEvents.actionTick(p,s);h.assertTrue(s.lit&&LighterItem.fuel(lighter)==LighterItem.MAX_FUEL-1,"One successful ignition consumes one fuel");h.succeed();}
    @GameTest(template="empty") public static void partial_state_preserved(GameTestHelper h){FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack cig=new ItemStack(Embers.CIGARETTES.get(Brand.CHAPMAN).get());cig.getOrCreateTag().putInt("burn",830);cig.getOrCreateTag().putInt("inhale",70);p.setItemInHand(InteractionHand.MAIN_HAND,cig);ServerEvents.input(p,new Network.Input(0,false,-1));ServerEvents.input(p,new Network.Input(1,false,-1));h.assertTrue(s.burn==830&&s.inhaleTotal==70&&s.mouth==Brand.CHAPMAN.ordinal(),"Partial cigarette state lost on placement");h.succeed();}
    @GameTest(template="empty") public static void daily_caps_and_persistence(GameTestHelper h){PlayerState s=new PlayerState();s.resetDay(4);for(int i=0;i<100;i++)s.addStress(3,2);h.assertTrue(s.stress==10&&s.gains[3]==10,"Damage cap exceeded");s.mouth=2;s.burn=1200;s.inhaleTotal=50;s.smoked=9;s.fishRelief=18;PlayerState loaded=PlayerState.load(s.save());h.assertTrue(loaded.stress==10&&loaded.mouth==2&&loaded.burn==1200&&loaded.smoked==9&&loaded.fishRelief==18,"Persistent state mismatch");loaded.resetDay(5);h.assertTrue(loaded.smoked==0&&loaded.gains[3]==0&&loaded.stress==10&&loaded.mouth==2,"New day must reset quotas but preserve stress and cigarette");h.succeed();}
    @GameTest(template="empty") public static void pressure_relief_requires_inhaling(GameTestHelper h){FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);s.stress=70;s.mouth=Brand.ZHONGHUA.ordinal();s.lit=true;s.inhaleTotal=119;ServerEvents.extinguish(p,false,false,true);h.assertTrue(s.stress==70&&s.smoked==0,"Insufficient inhalation must not farm relief");s.mouth=Brand.ZHONGHUA.ordinal();s.lit=true;s.inhaleTotal=120;ServerEvents.extinguish(p,false,false,true);h.assertTrue(s.stress==59&&s.smoked==1,"Premium daily first relief must be eleven");s.mouth=Brand.CHAPMAN.ordinal();s.lit=true;s.inhaleTotal=120;ServerEvents.extinguish(p,false,false,true);h.assertTrue(s.stress==54&&s.smoked==2,"Brand switching must not reset daily ordinal");h.succeed();}
    @GameTest(template="empty") public static void ashtray_save_and_cap(GameTestHelper h){h.setBlock(new BlockPos(1,1,1),Embers.ASHTRAY.get());AshtrayEntity be=(AshtrayEntity)h.getBlockEntity(new BlockPos(1,1,1));be.count=17;var n=be.saveWithoutMetadata();AshtrayEntity copy=new AshtrayEntity(be.getBlockPos(),be.getBlockState());copy.load(n);h.assertTrue(copy.count==17,"Butt count must survive chunk reload");n.putInt("count",1000);copy.load(n);h.assertTrue(copy.count==20,"Ashtray count must be bounded");h.succeed();}
    @GameTest(template="empty") public static void lighter_pop_destroys_item_only(GameTestHelper h){BlockPos at=new BlockPos(2,1,2);h.setBlock(at,net.minecraft.world.level.block.Blocks.STONE);var pos=h.absolutePos(at);ItemStack stack=new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get());ItemEntity item=new ItemEntity(h.getLevel(),pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,stack);h.getLevel().addFreshEntity(item);item.setSecondsOnFire(2);((LighterItem)stack.getItem()).onEntityItemUpdate(stack,item);h.assertTrue(item.isRemoved(),"Hot lighter must be destroyed");h.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE,at);h.succeed();}
    @GameTest(template="empty") public static void two_player_transfer_and_lending(GameTestHelper h){FakePlayer a=player(h),b=player(h);b.setPos(a.getX(),a.getY(),a.getZ()+2);a.setYRot(0);b.setYRot(180);h.getLevel().addNewPlayer(a);h.getLevel().addNewPlayer(b);try{PlayerState sa=ServerEvents.state(a),sb=ServerEvents.state(b);ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.CABALLERO_BLACK).get());a.setItemInHand(InteractionHand.MAIN_HAND,pack);ServerEvents.input(a,new Network.Input(0,true,-1));ServerEvents.input(a,new Network.Input(1,true,-1));h.assertTrue(sa.action.equals("offer"),"An offer must remain after a short click");ServerEvents.input(b,new Network.Input(0,false,a.getId()));for(int i=0;i<20;i++)ServerEvents.actionTick(b,sb);h.assertTrue(SmokingItem.remaining(pack)==9&&b.getMainHandItem().is(Embers.CIGARETTES.get(Brand.CABALLERO_BLACK).get()),"Transfer must debit giver and credit recipient exactly once");for(int i=0;i<50;i++)ServerEvents.actionTick(b,sb);h.assertTrue(SmokingItem.remaining(pack)==9&&b.getMainHandItem().getCount()==1,"Continuing ticks must not duplicate transfer");ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get());lighter.getOrCreateTag().putInt("fuel",1);a.setItemInHand(InteractionHand.MAIN_HAND,lighter);sa.idle();sa.lastRequest=-100;sb.mouth=Brand.CABALLERO_BLACK.ordinal();sb.lit=false;ServerEvents.input(a,new Network.Input(0,true,b.getId()));ServerEvents.input(a,new Network.Input(1,true,b.getId()));for(int i=0;i<30;i++)ServerEvents.actionTick(a,sa);h.assertTrue(sb.lit&&LighterItem.fuel(lighter)==0,"Shared flame must light recipient and consume one fuel");h.succeed();}finally{a.remove(Entity.RemovalReason.DISCARDED);b.remove(Entity.RemovalReason.DISCARDED);}}
    @GameTest(template="empty") public static void changing_slot_aborts_preparation(GameTestHelper h){FakePlayer p=player(h);PlayerState s=ServerEvents.state(p);ItemStack pack=new ItemStack(Embers.PACKS.get(Brand.SEVEN_STARS).get());p.setItemInHand(InteractionHand.MAIN_HAND,pack);p.getInventory().setItem(1,new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get()));ServerEvents.input(p,new Network.Input(0,false,-1));p.getInventory().selected=1;for(int i=0;i<70;i++)ServerEvents.actionTick(p,s);h.assertTrue(s.mouth==-1&&SmokingItem.remaining(pack)==10&&s.action.equals("idle"),"Switching slots must not complete a stale action");h.succeed();}
}
