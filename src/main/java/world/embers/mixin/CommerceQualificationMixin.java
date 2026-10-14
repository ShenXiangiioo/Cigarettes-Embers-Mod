package world.embers.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import world.embers.compat.CultivationNetwork;
import java.util.*;

@Pseudo @Mixin(targets="com.codex.commerce.client.QualificationDispenserScreen",remap=false)
public abstract class CommerceQualificationMixin {
    @Unique private boolean embers$cultivation;
    @Shadow(remap=false) private String nameDraft,holderDraft,numberDraft,validityDraft;
    @Shadow(remap=false) @Final private LinkedHashSet<?> selectedLinks;
    @Shadow(remap=false) private void button(String text,int x,int y,int width,int height,Runnable action){throw new AssertionError();}
    @Inject(method="buildControls",at=@At("TAIL"),require=0,remap=false)
    private void embers$controls(CallbackInfo ci) {
        try {
            var field=getClass().getDeclaredField("view");field.setAccessible(true);
            if(!field.get(this).toString().equals("CREATE"))return;
            String key=embers$cultivation?"screen.embers.cultivation.selected":"screen.embers.cultivation.title";
            button(Component.translatable(key).getString(),24,186,612,26,()->Minecraft.getInstance().setScreen(new world.embers.client.CultivationScreen((Screen)(Object)this,embers$cultivation,v->embers$cultivation=v)));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Commerce qualification screen changed",e);}
    }
    @Inject(method="issue",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void embers$issue(CallbackInfo ci) {
        if(!embers$cultivation)return;ci.cancel();
        if(nameDraft.isBlank()||numberDraft.isBlank()||validityDraft.isBlank())return;
        try {
            var menu=Minecraft.getInstance().player.containerMenu;
            BlockPos pos=(BlockPos)menu.getClass().getMethod("pos").invoke(menu);
            List<CompoundTag> links=new ArrayList<>();
            for(Object link:selectedLinks)links.add((CompoundTag)link.getClass().getMethod("toTag").invoke(link));
            world.embers.Network.CHANNEL.sendToServer(new CultivationNetwork.Issue(pos,menu.containerId,nameDraft,holderDraft,numberDraft,validityDraft,links));
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Commerce qualification screen changed",e);}
    }
}
