package world.embers.mixin;

import world.embers.FoodRestriction;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Run before nutrition mods which replace eat() and cancel the vanilla method.
@Mixin(value=FoodData.class,priority=2000)
public abstract class FoodDataMixin implements FoodRestriction.Owner {
    @Unique private Player embers$player;
    @Shadow(aliases="f_38696_") private int foodLevel;
    @Shadow(aliases="f_38697_") private float saturationLevel;
    @Override public void embers$owner(Player p){embers$player=p;}
    @Override public Player embers$owner(){return embers$player;}
    @Inject(method="eat(IF)V",at=@At("HEAD"),cancellable=true)
    private void embers$eat(int food,float saturation,CallbackInfo ci){if(FoodRestriction.blocked((FoodData)(Object)this))ci.cancel();}
    @Inject(method="setFoodLevel",at=@At("HEAD"),cancellable=true)
    private void embers$food(int value,CallbackInfo ci){if(value>foodLevel&&FoodRestriction.blocked((FoodData)(Object)this))ci.cancel();}
    @Inject(method="setSaturation",at=@At("HEAD"),cancellable=true)
    private void embers$saturation(float value,CallbackInfo ci){if(value>saturationLevel&&FoodRestriction.blocked((FoodData)(Object)this))ci.cancel();}
    @Inject(method="tick",at=@At("HEAD"))
    private void embers$bind(Player player,CallbackInfo ci){embers$player=player;}
}
