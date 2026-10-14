package world.embers.mixin;
import world.embers.MobSmoking;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperSmokingMixin {
    @Shadow(aliases="f_32270_") private int swell;
    @Shadow(aliases="f_32271_") private int maxSwell;
    @Shadow(aliases="f_32269_") private int oldSwell;
    @Inject(method="tick",at=@At("HEAD"))
    private void embers$fuse(CallbackInfo ci){Creeper c=(Creeper)(Object)this;if(c.level().isClientSide)return;var n=MobSmoking.data(c);if(MobSmoking.extendFuse(c)){n.putInt("fuseWait",80);n.putUUID("fusePlayer",c.getTarget().getUUID());}
        if(!c.isIgnited()&&c.getTarget() instanceof Player p){if(MobSmoking.protectedFrom(c,p)){c.setSwellDir(-1);swell=oldSwell=0;}else if(n.getInt("fuseWait")>0&&n.hasUUID("fusePlayer")&&p.getUUID().equals(n.getUUID("fusePlayer"))&&swell>=maxSwell-1){swell=maxSwell-2;n.putInt("fuseWait",n.getInt("fuseWait")-1);}}
        if(!c.isIgnited()&&c.getTarget()==null&&n.getLong("protectedTick")>=c.level().getGameTime()-1&&n.contains("protectedTick")){swell=oldSwell=0;c.setSwellDir(-1);}
    }
}
