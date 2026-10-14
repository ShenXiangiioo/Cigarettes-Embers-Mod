package world.embers.qa;
import world.embers.*;
import world.embers.production.*;
import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.*;
import java.util.*;

/** Actual loaded-mod food restoration probe, kept out of the release jar. */
public final class ProductionQa {
    public static void run(MinecraftServer server,UUID id,Path game){ServerPlayer p=server.getPlayerList().getPlayer(id);JsonObject report=new JsonObject();JsonArray checks=new JsonArray();int total=0;CompoundTag previous=SmokingBenefits.data(p).copy();
        try{FoodRestriction.bind(p);p.removeAllEffects();SmokingBenefits.data(p).putInt("addiction",0);p.getFoodData().setFoodLevel(5);p.getFoodData().setSaturation(0);SmokingBenefits.data(p).putInt("addiction",72000);
            for(Item item:BuiltInRegistries.ITEM){if(item==Items.ROTTEN_FLESH||!item.isEdible())continue;ResourceLocation key=BuiltInRegistries.ITEM.getKey(item);if(key.getNamespace().equals("minecraft")&&item!=Items.COOKED_BEEF&&item!=Items.GOLDEN_CARROT)continue;p.getFoodData().setFoodLevel(5);p.getFoodData().setSaturation(0);ItemStack stack=new ItemStack(item);int before=p.getFoodData().getFoodLevel();float sat=p.getFoodData().getSaturationLevel();FoodRestriction.enter(p,stack);try{item.finishUsingItem(stack,p.level(),p);}finally{FoodRestriction.leave();}boolean pass=p.getFoodData().getFoodLevel()<=before&&p.getFoodData().getSaturationLevel()<=sat;JsonObject check=new JsonObject();check.addProperty("item",key.toString());check.addProperty("food",p.getFoodData().getFoodLevel());check.addProperty("saturation",p.getFoodData().getSaturationLevel());check.addProperty("pass",pass);checks.add(check);total++;if(!pass)throw new IllegalStateException("Food restoration bypass: "+key);p.removeAllEffects();}
            p.getFoodData().setFoodLevel(5);p.getFoodData().setSaturation(0);p.eat(p.level(),new ItemStack(Items.ROTTEN_FLESH));report.addProperty("rottenFood",p.getFoodData().getFoodLevel());report.addProperty("rottenHunger",p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER));report.addProperty("allPass",p.getFoodData().getFoodLevel()==9&&!p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER));
        }catch(Throwable ex){report.addProperty("allPass",false);report.addProperty("error",ex.toString());}finally{p.getPersistentData().put("embers_benefits",previous);p.removeAllEffects();p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);report.addProperty("foodsTested",total);report.add("checks",checks);try{Files.writeString(game.resolve("production-food-check.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception ex){throw new RuntimeException(ex);}}
    }
    public static void garden(ServerPlayer p){var level=p.serverLevel();for(int i=0;i<4;i++){Tobacco t=Tobacco.values()[i];for(int j=0;j<4;j++){BlockPos at=new BlockPos(245+i*2,-60,241+j);level.setBlock(at.below(),t==Tobacco.YELLOW?Blocks.SAND.defaultBlockState():Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE,7),3);level.setBlock(at,((TobaccoCrop)Production.CROPS.get(t).get()).getStateForAge(j==0?0:j==1?3:j==2?5:7),3);}}
        for(int i=0;i<3;i++){BlockPos at=new BlockPos(235+i,-60,242);var state=Production.RACK.get().defaultBlockState().setValue(DryingRackBlock.FACING,Direction.NORTH);level.setBlock(at,state,3);level.setBlock(at.above(),state.setValue(DryingRackBlock.UPPER,true),3);if(level.getBlockEntity(at) instanceof DryingRackEntity rack){for(int j=0;j<3;j++){rack.tobacco[j]=(i+j)%4;rack.progress[j]=(i*3+j)/8d;}rack.changed();}}
    }
}
