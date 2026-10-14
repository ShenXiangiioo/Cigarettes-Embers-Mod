package world.embers.qa;

import world.embers.compat.*;
import world.embers.production.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public final class CultivationQa {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void run(MinecraftServer server,UUID id,Path directory) {
        JsonObject report=new JsonObject();
        try {
            ServerPlayer p=server.getPlayerList().getPlayer(id);var level=p.serverLevel();
            Class<?> type=Class.forName("com.codex.commerce.data.QualificationCertificateSavedData"),menu=Class.forName("com.codex.commerce.menu.QualificationDispenserMenu");
            Object data=type.getMethod("get",ServerLevel.class).invoke(null,level);
            var field=type.getDeclaredField("certificates");field.setAccessible(true);List<Object> records=(List<Object>)field.get(data);
            // Remove prior QA-only records, preserving any non-QA data in this disposable world.
            records.removeIf(c->{try{return ((String)c.getClass().getMethod("number").invoke(c)).startsWith("QA12-");}catch(Exception e){throw new RuntimeException(e);}});
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("commerce:administrator_tablet"))));
            BlockPos dispenser=p.blockPosition().offset(1,0,1);level.setBlockAndUpdate(dispenser,BuiltInRegistries.BLOCK.get(new ResourceLocation("commerce:qualification_dispenser")).defaultBlockState());
            menu.getMethod("open",ServerPlayer.class,BlockPos.class,int.class).invoke(null,p,dispenser,0);
            check(CommerceBridge.administrator(p),"QA player is authorized");
            check(!CultivationPermits.get(level).allows(p),"No permission before issue");
            int container=p.containerMenu.containerId;
            var request=new CultivationNetwork.Issue(dispenser,container,"Grower",p.getGameProfile().getName(),"QA12-"+System.currentTimeMillis(),"1",List.of());
            check(CultivationNetwork.issue(p,request),"Issue real bound certificate");
            check(!CultivationNetwork.issue(p,request),"Stale/replayed menu cannot issue twice");
            check(CultivationPermits.get(level).allows(p),"Bound UUID permission applies without carrying certificate");
            Object certificate=records.get(records.size()-1);CompoundTag record=(CompoundTag)certificate.getClass().getMethod("toTag").invoke(certificate);
            check(CultivationPermits.load(CultivationPermits.get(level).save(new CompoundTag())).allows(p),"Saved permission index resolves live Commerce data");
            JsonArray plants=new JsonArray();int offset=0;
            for(Tobacco tobacco:Tobacco.values()) {
                BlockPos ground=p.blockPosition().offset(offset++,0,3);level.setBlockAndUpdate(ground,Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(ground.above(),Blocks.AIR.defaultBlockState());
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Production.SEEDS.get(tobacco).get(),2));
                p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false)));
                check(level.getBlockState(ground.above()).is(Production.CROPS.get(tobacco).get()),"All colors plant with permit: "+tobacco);plants.add(tobacco.id);
            }
            report.add("plantedColors",plants);
            // Exact server authority is re-read, rather than cached as a permanent player flag.
            int index=records.indexOf(certificate);CompoundTag expired=record.copy();expired.putLong("ExpiresAt",System.currentTimeMillis()-1);
            records.set(index,certificate.getClass().getMethod("fromTag",CompoundTag.class).invoke(null,expired));
            check(!CultivationPermits.get(level).allows(p),"Expiration takes effect immediately");
            for(int i=0;i<4;i++) check(level.getBlockState(p.blockPosition().offset(i,1,3)).is(Production.CROPS.get(Tobacco.values()[i]).get()),"Expired permit preserves planted crops");
            BlockPos ground=p.blockPosition().offset(4,0,3);level.setBlockAndUpdate(ground,Blocks.FARMLAND.defaultBlockState());level.setBlockAndUpdate(ground.above(),Blocks.AIR.defaultBlockState());
            Tobacco wrong=Arrays.stream(Tobacco.values()).filter(t->!level.getBiome(ground.above()).is(t.biomes)).findFirst().orElseThrow();
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Production.SEEDS.get(wrong).get(),2));p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),Direction.UP,ground,false)));
            check(level.getBlockState(ground.above()).isAir(),"Expired permit rejects mismatched biome placement");
            records.set(index,certificate);type.getMethod("revoke",String.class).invoke(data,record.getString("Id"));check(!CultivationPermits.get(level).allows(p),"Revocation takes effect immediately");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("commerce:administrator_tablet"))));
            menu.getMethod("open",ServerPlayer.class,BlockPos.class,int.class).invoke(null,p,dispenser,0);
            var anonymous=new CultivationNetwork.Issue(dispenser,p.containerMenu.containerId,"Unassigned grower","","QA12-free-"+System.currentTimeMillis(),"1",List.of());
            check(CultivationNetwork.issue(p,anonymous),"Can issue unassigned certificate");Object free=records.get(records.size()-1);
            check(!CultivationPermits.get(level).allows(p),"Unassigned certificate is not a bearer bypass");
            Object bound=type.getMethod("bindUnassigned",free.getClass(),ServerPlayer.class).invoke(data,free,p);check(bound!=null&&CultivationPermits.get(level).allows(p),"Commerce claim binds permit to UUID");
            type.getMethod("revoke",String.class).invoke(data,bound.getClass().getMethod("id").invoke(bound));
            menu.getMethod("open",ServerPlayer.class,BlockPos.class,int.class).invoke(null,p,dispenser,0);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            check(!CultivationNetwork.issue(p,new CultivationNetwork.Issue(dispenser,p.containerMenu.containerId,"Forged",p.getGameProfile().getName(),"QA12-forged","X",List.of())),"No tablet cannot issue");
            p.closeContainer();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("commerce:administrator_tablet"))));
            report.addProperty("result","PASS");report.addProperty("checks",15);report.addProperty("dispenser",dispenser.toShortString());
        } catch(Throwable e){report.addProperty("result","FAIL: "+e);com.mojang.logging.LogUtils.getLogger().error("Cultivation QA failed",e);}
        try{Files.writeString(directory.resolve("cultivation-check.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
    }
}
