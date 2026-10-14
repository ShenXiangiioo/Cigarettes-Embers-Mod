package world.embers.mixin;
import world.embers.PressureEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class PressureEffectMixin implements PressureEffects.Owned {
    @Unique private boolean embers$ownedPressure;
    @Override public boolean embers$pressure(){return embers$ownedPressure;}
    @Override public void embers$pressure(boolean owned){embers$ownedPressure=owned;}
    @Inject(method="save",at=@At("RETURN"))
    private void embers$saveOwner(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci){if(embers$ownedPressure)ci.getReturnValue().putBoolean("embers_pressure",true);}
    @Inject(method="load",at=@At("RETURN"))
    private static void embers$loadOwner(CompoundTag tag,CallbackInfoReturnable<MobEffectInstance> ci){if(ci.getReturnValue() instanceof PressureEffects.Owned o)o.embers$pressure(tag.getBoolean("embers_pressure"));}
}
