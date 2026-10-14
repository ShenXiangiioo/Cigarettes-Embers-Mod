package world.embers.mixin;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import world.embers.client.DynamicLighting;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightMixin {
    @Inject(method="getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I",at=@At("RETURN"),cancellable=true)
    private void cigarettes$light(long position,BlockState state,CallbackInfoReturnable<Integer> result){
        int emitted=DynamicLighting.emission(this,position);
        if(emitted>result.getReturnValueI())result.setReturnValue(emitted);
    }
}
