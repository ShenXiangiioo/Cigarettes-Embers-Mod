package world.embers.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.embers.MobSmoking;

@Mixin(Mob.class)
public abstract class SmokingMobMovementMixin {
    @Inject(method="serverAiStep",at=@At("TAIL"))
    private void embers$restWhileSmoking(CallbackInfo ci){
        Mob mob=(Mob)(Object)this;
        if(!MobSmoking.visible(mob)||MobSmoking.remaining(mob)<=0||MobSmoking.data(mob).getBoolean("broken"))return;
        mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.PATH);
        mob.getMoveControl().setWantedPosition(mob.getX(),mob.getY(),mob.getZ(),0);
        mob.xxa=mob.zza=0;mob.setSpeed(0);
        // Preserve gravity and swimming; do not save a NoAI flag or pin an entity in mid-air.
        if(!mob.isInWater())mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);
    }
}
