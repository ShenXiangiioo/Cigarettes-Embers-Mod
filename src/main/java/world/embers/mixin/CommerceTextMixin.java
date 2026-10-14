package world.embers.mixin;
import net.minecraft.network.chat.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.codex.commerce.util.CommerceText",remap=false)
public abstract class CommerceTextMixin {
    @Inject(method="component(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void embers$treasuryLabel(String value,CallbackInfoReturnable<MutableComponent> cir){if(value==null||!value.contains("embers.refill_fee"))return;try{var translate=Class.forName("com.codex.commerce.util.CommerceText").getMethod("component",String.class);MutableComponent result=Component.empty();String[] parts=value.split("embers\\.refill_fee",-1);for(int i=0;i<parts.length;i++){if(i>0)result.append(Component.translatable("embers.refill_fee"));if(!parts[i].isEmpty())result.append((Component)translate.invoke(null,parts[i]));}cir.setReturnValue(result);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}
