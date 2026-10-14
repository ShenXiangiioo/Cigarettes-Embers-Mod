package world.embers.qa;

import world.embers.*;
import world.embers.production.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("embers") @PrefixGameTestTemplate(false)
public final class RevisionTests {
    @GameTest(template="empty") public static void all_recipe_input_permutations(GameTestHelper h){
        var m=ProductionTests.machine(h);int checked=0;
        for(var r:m.recipes())for(int a=0;a<4;a++)for(int b=0;b<4;b++)for(int c=0;c<4;c++)if(a!=b&&a!=c&&b!=c){
            m.running=false;m.result=ItemStack.EMPTY;m.clearContent();m.fuel=4;m.selected="embers:processing/not_the_recipe";
            m.setItem(2,new ItemStack(Items.PAPER));int[] layout={a,b,c};var parts=r.ingredients();
            for(int i=0;i<parts.size();i++){var part=parts.get(i);m.setItem(ProcessingRecipe.INPUT_SLOTS[layout[i]],part.ingredient().getItems()[0].copyWithCount(part.count()));}
            h.assertTrue(m.start(),"Unordered inputs match "+r.getId());ProductionTests.advance(m,6000);
            h.assertTrue(ItemStack.isSameItemSameTags(m.getItem(8),r.output),"Correct output after stale selection "+r.getId());checked++;
        }
        h.assertTrue(checked==408,"All 17 recipes in 24 layouts");h.succeed();
    }
    @GameTest(template="empty") public static void chu_combines_stacked_tobacco(GameTestHelper h){
        var m=ProductionTests.machine(h);m.setItem(0,new ItemStack(Production.DRY.get(Tobacco.RED).get(),8));m.setItem(3,new ItemStack(Items.WHEAT,16));m.setItem(1,new ItemStack(Items.LAVA_BUCKET));m.setItem(2,new ItemStack(Items.PAPER,64));
        h.assertTrue(m.start()&&m.result.is(Embers.PACKS.get(Brand.FURONGWANG).get()),"Exact reported layout crafts Chu");h.assertTrue(m.getItem(0).getCount()==6&&m.getItem(3).getCount()==15,"Consume two leaves from the same stack");m.stop();h.assertTrue(m.getItem(0).getCount()==8&&m.getItem(3).getCount()==16,"Refund exact same slots");h.succeed();
    }
    @GameTest(template="empty") public static void drying_keeps_half_on_each_removal(GameTestHelper h){
        h.setBlock(new BlockPos(1,1,1),Production.RACK.get());var rack=(DryingRackEntity)h.getBlockEntity(new BlockPos(1,1,1));rack.tobacco[0]=0;rack.progress[0]=.6;
        var item=rack.take(0);h.assertTrue(Math.abs(DryingRackEntity.storedProgress(item)-.3)<1e-8,"60% ->30%");var p=ProductionTests.player(h);p.setItemInHand(InteractionHand.MAIN_HAND,item);rack.interact(p,InteractionHand.MAIN_HAND,0);h.assertTrue(Math.abs(rack.progress[0]-.3)<1e-8,"Rehang keeps item progress");item=rack.take(0);h.assertTrue(Math.abs(DryingRackEntity.storedProgress(item)-.15)<1e-8,"Second removal halves again");rack.tobacco[0]=0;rack.progress[0]=1;h.assertTrue(rack.take(0).is(Production.DRY.get(Tobacco.RED).get()),"Finished leaf remains dry");h.succeed();
    }
    @GameTest(template="empty") public static void seed_weights_regions_and_names(GameTestHelper h){
        int[] counts=new int[4];for(int i=0;i<10;i++)counts[WildSeeds.weighted(i).ordinal()]++;h.assertTrue(java.util.Arrays.equals(counts,new int[]{4,3,2,1}),"Conditional 40/30/20/10 weights");
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);int covered=0,purple=0;
        for(var holder:registry.holders().toList())if(holder.key().location().getNamespace().equals("minecraft")&&holder.is(BiomeTags.IS_OVERWORLD)){
            int matches=0;for(var t:Tobacco.values())if(holder.is(t.biomes))matches++;h.assertTrue(matches==1,"One region covers "+holder.key().location());covered++;if(holder.is(Tobacco.PURPLE.biomes))purple++;
        }
        h.assertTrue(covered>=50&&purple==2&&registry.getHolderOrThrow(Biomes.SAVANNA).is(Tobacco.YELLOW.biomes),"Overworld covered, purple restricted, savanna yellow");
        for(var t:Tobacco.values())h.assertTrue(Production.SEEDS.get(t).get().getDescriptionId().equals("item.embers."+t.id+"_tobacco_seeds"),"Seed uses item name rather than crop name");h.succeed();
    }
    @GameTest(template="empty") public static void nine_brands_actual_ignition_and_inhale(GameTestHelper h){
        int tested=0;var originalRules=PressureSettings.get(h.getLevel().getServer());boolean originalPressure=originalRules.enabled,originalAddiction=originalRules.addictionEnabled;
        try {
        for(Brand brand:Brand.values()){
            int group=brand.horizontalCase()?0:brand==Brand.HETIANXIA||brand==Brand.HETIANXIA_SLIM?1:brand==Brand.CHAPMAN||brand==Brand.CHAPMAN_SLIM?2:brand==Brand.ZHONGHUA||brand==Brand.ZHONGHUA_SLIM?3:-1;if(group<0)continue;
            var p=ProductionTests.player(h);var rules=PressureSettings.get(p.server);rules.effectsEnabled=true;rules.brandEffects=new boolean[]{true,true,true,true};rules.addictionEnabled=false;rules.enabled=false;
            var s=ServerEvents.state(p);s.lastRequest=-100;s.lastPuff=-100;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Embers.PACKS.get(brand).get()));p.getInventory().setItem(1,new ItemStack(Embers.LIGHTERS.get(LighterKind.WHEEL).get()));
            ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<65;i++)new ServerEvents().tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));
            h.assertTrue(s.lit&&s.mouth==brand.ordinal(),"Actual pack/lighting flow "+brand);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(int puff=0;puff<2;puff++){s.lastRequest=-100;s.lastPuff=-100;ServerEvents.input(p,new Network.Input(0,false,-1));for(int i=0;i<60;i++)new ServerEvents().tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));ServerEvents.input(p,new Network.Input(1,false,-1));for(int i=0;i<75;i++)new ServerEvents().tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));if(puff==0)h.assertTrue(!SmokingBenefits.active(p,group),"Below cumulative six seconds "+brand);}
            h.assertTrue(s.inhaleTotal==120&&SmokingBenefits.active(p,group)&&p.hasEffect(SmokingBenefits.EFFECTS.get(group).get()),"Actual smoking gives correct icon/benefit "+brand);
            if(group==1)h.assertTrue(p.getAbilities().mayfly,"Survival flight granted");SmokingBenefits.end(p,group);h.assertTrue(!SmokingBenefits.active(p,group)&&SmokingBenefits.data(p).getInt("cooldown"+group)==1200,"Cleanup/cooldown "+brand);tested++;
        }
        h.assertTrue(tested==9,"All nine benefit-bearing styles tested");h.succeed();
        }finally{originalRules.enabled=originalPressure;originalRules.addictionEnabled=originalAddiction;}
    }
}
