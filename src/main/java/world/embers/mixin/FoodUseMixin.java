package world.embers.mixin;

import world.embers.FoodRestriction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FoodUseMixin {
    @Inject(method="completeUsingItem",at=@At("HEAD"))
    private void embers$begin(CallbackInfo ci){if((Object)this instanceof Player p)FoodRestriction.enter(p,p.getUseItem());}
    @Inject(method="completeUsingItem",at=@At("RETURN"))
    private void embers$end(CallbackInfo ci){if((Object)this instanceof Player)FoodRestriction.leave();}
}
