package world.embers.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="com.codex.commerce.client.LoginSecurityAdminScreen",remap=false)
public abstract class CommerceTabletMixin {
    @Shadow(remap=false) private Button addButton(Component label,int x,int y,int width,Button.OnPress press){throw new AssertionError();}
    @Inject(method="buildWorldPage",at=@At("TAIL"),require=0,remap=false)
    private void embers$worldButton(CallbackInfo ci){addButton(Component.translatable("screen.embers.admin.title"),18,292,176,b->world.embers.client.PressureScreen.open((Screen)(Object)this));}
}
