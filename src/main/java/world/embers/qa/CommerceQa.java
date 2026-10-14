package world.embers.qa;
import world.embers.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

/** Isolated QA jar only. Exercises real Commerce SavedData with the disposable QA player. */
public final class CommerceQa {
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void run(MinecraftServer server,UUID id,Path directory){
        JsonObject report=new JsonObject();
        try {
            ServerPlayer p=server.getPlayerList().getPlayer(id);ServerLevel level=p.serverLevel();
            Class<?> bc=Class.forName("com.codex.commerce.data.BankAccountSavedData"),hc=Class.forName("com.codex.commerce.data.CommerceTransactionSavedData");
            Object bank=bc.getMethod("get",ServerLevel.class).invoke(null,level),history=hc.getMethod("get",ServerLevel.class).invoke(null,level);
            bc.getMethod("openAccount",ServerPlayer.class).invoke(bank,p);bc.getMethod("deposit",UUID.class,long.class).invoke(bank,id,1000L);
            long balance=(long)bc.getMethod("getBalance",UUID.class).invoke(bank,id);String treasury=(String)bc.getField("DEFAULT_BANK_ID").get(null);
            long before=(long)bc.getMethod("getTreasuryBalance",String.class).invoke(bank,treasury);
            int historyBefore=((List<?>)hc.getMethod("recent",UUID.class).invoke(history,id)).size();
            BlockPos pos=p.blockPosition().offset(0,0,2);level.setBlockAndUpdate(pos,Embers.STATION.get().defaultBlockState());StationEntity be=(StationEntity)level.getBlockEntity(pos);be.fee=7;
            ItemStack lighter=new ItemStack(Embers.LIGHTERS.get(LighterKind.OIL_BRASS).get());lighter.getOrCreateTag().putInt("fuel",0);be.setItem(0,lighter.copy());be.setItem(1,ItemStack.EMPTY);
            StationMenu menu=new StationMenu(12,p.getInventory(),be,pos);
            check(menu.canRefill()&&be.getItem(2).isEmpty(),"Paid station must be ready without lava and without a preview item");
            check(menu.clickMenuButton(p,0),"Paid refill failed");check(!menu.clickMenuButton(p,0),"Repeated refill charged twice");
            long afterBalance=(long)bc.getMethod("getBalance",UUID.class).invoke(bank,id),afterTreasury=(long)bc.getMethod("getTreasuryBalance",String.class).invoke(bank,treasury);
            check(afterBalance==balance-7&&afterTreasury==before+7,"Money not conserved");check(LighterItem.fuel(be.getItem(2))==LighterItem.MAX_FUEL&&be.getItem(0).isEmpty()&&be.getItem(1).isEmpty(),"Refill inventory mismatch");
            List<?> entries=(List<?>)hc.getMethod("recent",UUID.class).invoke(history,id);Object entry=entries.stream().filter(e->{try{return e.getClass().getMethod("type").invoke(e).equals("cigarette_refill");}catch(Exception ex){throw new RuntimeException(ex);}}).findFirst().orElseThrow();
            check(entries.size()==Math.min(20,historyBefore+1),"Phone history did not add one row");
            var display=(net.minecraft.network.chat.Component)hc.getMethod("transactionDisplay",entry.getClass()).invoke(null,entry);report.addProperty("phoneDisplay",display.getString());
            var label=(net.minecraft.network.chat.Component)Class.forName("com.codex.commerce.util.CommerceText").getMethod("component",String.class).invoke(null,"+7 / embers.refill_fee");report.addProperty("treasuryDisplay",label.getString());
            check(!label.getString().contains("embers.refill_fee"),"Treasury category not localized");
            be.setItem(0,lighter.copy());check(!menu.clickMenuButton(p,0),"Occupied output allowed another debit");
            check((long)bc.getMethod("getBalance",UUID.class).invoke(bank,id)==afterBalance,"Occupied output changed balance");
            be.removeItem(2,1);be.setItem(0,lighter.copy());
            lighter.getOrCreateTag().putInt("fuel",0);be.setItem(1,new ItemStack(Items.LAVA_BUCKET));be.fee=1_000_000_000L;
            check(!menu.clickMenuButton(p,0)&&LighterItem.fuel(lighter)==0&&be.getItem(1).is(Items.LAVA_BUCKET)&&be.getItem(2).isEmpty(),"Insufficient funds consumed stock");
            check((long)bc.getMethod("getBalance",UUID.class).invoke(bank,id)==afterBalance,"Rejected transaction debited money");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            AdminNetwork.apply(p,new AdminNetwork.Request(4,pos,0,0,0,0,999));check(be.fee==1_000_000_000L,"Tablet-less request changed price");
            PressureSettings settings=PressureSettings.get(server);int gain=settings.gain[0];AdminNetwork.apply(p,new AdminNetwork.Request(1,null,1,0,99,99,0));check(settings.gain[0]==gain,"Missing tablet menu changed global settings");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("commerce:administrator_tablet"))));
            AdminNetwork.apply(p,new AdminNetwork.Request(4,pos,0,0,0,0,-1));check(be.fee==1_000_000_000L,"Negative fee accepted");
            AdminNetwork.apply(p,new AdminNetwork.Request(4,pos,0,0,0,0,7));check(be.fee==7,"Administrator could not set station fee");
            StationEntity copy=new StationEntity(pos,be.getBlockState());copy.load(be.saveWithoutMetadata());check(copy.fee==7,"Fee not persistent");
            be.fee=0;check(menu.clickMenuButton(p,0),"Free refill failed");check((long)bc.getMethod("getBalance",UUID.class).invoke(bank,id)==afterBalance,"Free refill charged money");be.fee=7;be.setChanged();
            report.addProperty("walletDelta",afterBalance-balance);report.addProperty("treasuryDelta",afterTreasury-before);report.addProperty("result","PASS: paid without lava, output only after payment, occupied output protected, free refill, one debit, treasury, phone history, insufficient funds, permissions, invalid fee, persistence");
        }catch(Throwable e){report.addProperty("result","FAIL: "+e);com.mojang.logging.LogUtils.getLogger().error("Commerce QA failed",e);}
        try{Files.writeString(directory.resolve("commerce-check.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
    }
}
