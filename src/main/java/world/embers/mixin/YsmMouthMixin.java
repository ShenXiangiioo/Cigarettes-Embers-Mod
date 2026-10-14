package world.embers.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.embers.client.YsmMouth;

/** Optional hook, signature verified against the user's Forge YSM 2.6.5 distribution. */
@Pseudo
@Mixin(targets="com.elfmcys.yesstevemodel.oO0OO0000o0oOo00oOo00000",remap=false)
public abstract class YsmMouthMixin {
    @Inject(method="Oo0Oo0o00O00Oo0OOoOOoooo(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILcom/elfmcys/yesstevemodel/oo0OooOO0oOoOoOoo00oO000;FFFFFF)V",at=@At("HEAD"),require=0,remap=false)
    private void embers$mouth(PoseStack poses,MultiBufferSource buffers,int light,@Coerce Object actor,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){YsmMouth.render(poses,buffers,light,actor);}
}
