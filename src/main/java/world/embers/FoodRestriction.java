package world.embers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.*;

/** Common nutrition boundary, shared by vanilla and mods using FoodData. */
public final class FoodRestriction {
    public interface Owner {void embers$owner(Player player);Player embers$owner();}
    private record Meal(Player player,boolean rotten,boolean restricted,int food,float saturation){}
    private static final ThreadLocal<Deque<Meal>> MEALS=ThreadLocal.withInitial(ArrayDeque::new);
    public static void bind(Player p){if(p.getFoodData() instanceof Owner owner)owner.embers$owner(p);}
    public static boolean blocked(FoodData food){return food instanceof Owner o&&o.embers$owner()!=null&&SmokingBenefits.addicted(o.embers$owner())&&!rottenMeal(o.embers$owner());}
    public static void enter(Player p,ItemStack item){bind(p);var food=p.getFoodData();MEALS.get().push(new Meal(p,item.is(Items.ROTTEN_FLESH),SmokingBenefits.addicted(p),food.getFoodLevel(),food.getSaturationLevel()));}
    public static void leave(){if(!MEALS.get().isEmpty()){Meal meal=MEALS.get().pop();if(meal.restricted&&!meal.rotten){var food=meal.player.getFoodData();if(food.getFoodLevel()>meal.food)food.setFoodLevel(meal.food);if(food.getSaturationLevel()>meal.saturation)food.setSaturation(meal.saturation);}}if(MEALS.get().isEmpty())MEALS.remove();}
    public static boolean rottenMeal(Player p){return !MEALS.get().isEmpty()&&MEALS.get().peek().player==p&&MEALS.get().peek().rotten;}
}
