package world.embers.qa;

import world.embers.*;
import world.embers.production.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import com.google.gson.*;
import java.nio.file.*;

/** Isolated-world probes only; omitted from release jars. */
public final class RevisionQa {
    public static void resetBenefits(ServerPlayer p){
        for(int i=0;i<4;i++)SmokingBenefits.end(p,i);p.getPersistentData().remove("embers_benefits");
        var rules=PressureSettings.get(p.server);rules.effectsEnabled=true;rules.brandEffects=new boolean[]{true,true,true,true};rules.addictionEnabled=false;
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getFoodData().setFoodLevel(6);p.getFoodData().setSaturation(0);
        ServerEvents.state(p).clearMouth();ServerEvents.state(p).cancelAction();Network.sync(p);
    }
    public static void report(ServerPlayer p,Path path){
        JsonObject o=new JsonObject();o.addProperty("benefits",SmokingBenefits.data(p).toString());
        var s=ServerEvents.state(p);o.addProperty("inhaleTotal",s.inhaleTotal);o.addProperty("counted",s.cigaretteCounted);o.addProperty("mouth",s.mouth);o.addProperty("mayfly",p.getAbilities().mayfly);o.addProperty("food",p.getFoodData().getFoodLevel());
        JsonArray effects=new JsonArray();for(int i=0;i<4;i++){JsonObject e=new JsonObject();e.addProperty("group",i);e.addProperty("active",SmokingBenefits.active(p,i));e.addProperty("icon",p.hasEffect(SmokingBenefits.EFFECTS.get(i).get()));e.addProperty("remaining",SmokingBenefits.data(p).getInt("effect"+i));effects.add(e);}o.add("effects",effects);
        JsonArray villagers=new JsonArray();for(var v:p.serverLevel().getEntitiesOfClass(Villager.class,p.getBoundingBox().inflate(16))){JsonObject vj=new JsonObject();vj.addProperty("id",v.getId());vj.addProperty("smoking",MobSmoking.data(v).toString());vj.addProperty("discount",MobSmoking.discount(v));vj.addProperty("position",v.position().toString());JsonArray offers=new JsonArray();for(var offer:v.getOffers()){JsonObject offerJson=new JsonObject();offerJson.addProperty("base",offer.getBaseCostA().toString());offerJson.addProperty("cost",offer.getCostA().toString());offerJson.addProperty("special",offer.getSpecialPriceDiff());offers.add(offerJson);}vj.add("offers",offers);villagers.add(vj);}o.add("villagers",villagers);
        try{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(o));}catch(Exception e){throw new RuntimeException(e);}
    }
}
