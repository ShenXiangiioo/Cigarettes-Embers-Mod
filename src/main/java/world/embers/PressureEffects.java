package world.embers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Marks our exact effect instances; never guesses ownership from duration or potion particles. */
@Mod.EventBusSubscriber(modid=Embers.ID)
public final class PressureEffects {
    public interface Owned {boolean embers$pressure();void embers$pressure(boolean owned);}
    private static boolean owned(MobEffectInstance e){return e instanceof Owned o&&o.embers$pressure();}
    public static void apply(ServerPlayer p,MobEffect type,int ticks,boolean icon){MobEffectInstance current=p.getEffect(type);if(current!=null&&!owned(current))return;MobEffectInstance effect=new MobEffectInstance(type,ticks,0,false,false,icon);((Owned)effect).embers$pressure(true);p.addEffect(effect);if(p.getEffect(type) instanceof Owned o)o.embers$pressure(true);}
    public static void clear(ServerPlayer p){for(var e:java.util.List.copyOf(p.getActiveEffects()))if(owned(e))p.removeEffect(e.getEffect());p.removeEffect(Embers.UNDEAD_FOCUS.get());p.removeEffect(Embers.COLLAPSE.get());}
    @SubscribeEvent public static void added(MobEffectEvent.Added e){if(!owned(e.getEffectInstance())){MobEffectInstance old=e.getEntity().getEffect(e.getEffectInstance().getEffect());if(old instanceof Owned o)o.embers$pressure(false);}}
}
