package world.embers.mixin;

import world.embers.FoodRestriction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @Inject(method="eat",at=@At("HEAD"))
    private void embers$beginMeal(Level level,ItemStack stack,CallbackInfoReturnable<ItemStack> ci){FoodRestriction.enter((Player)(Object)this,stack);}
    @Inject(method="eat",at=@At("RETURN"))
    private void embers$endMeal(Level level,ItemStack stack,CallbackInfoReturnable<ItemStack> ci){FoodRestriction.leave();}
}
