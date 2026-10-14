package world.embers.mixin;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.codex.commerce.data.CommerceTransactionSavedData",remap=false)
public abstract class CommerceTransactionMixin {
    @Inject(method="transactionDisplay",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void embers$refillEntry(@Coerce Object entry,CallbackInfoReturnable<Component> cir){try{if(entry.getClass().getMethod("type").invoke(entry).equals("cigarette_refill"))cir.setReturnValue(Component.translatable("transaction.embers.refill",entry.getClass().getMethod("amount").invoke(entry),entry.getClass().getMethod("note").invoke(entry)));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
