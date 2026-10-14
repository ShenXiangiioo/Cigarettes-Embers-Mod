package world.embers.client;
import world.embers.PlayerState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import java.util.*;
public final class ClientState {
    private static final Map<UUID,PlayerState> STATES=new HashMap<>();
    private static final Set<UUID> CANCELING=new HashSet<>();
    public static void accept(UUID id,CompoundTag tag){if(tag!=null){PlayerState state=PlayerState.load(tag);if(CANCELING.contains(id)){if(!state.action.equals("idle"))return;CANCELING.remove(id);}PlayerState old=STATES.get(id);if(old!=null&&old.mouth==state.mouth&&old.lit&&state.lit){state.emberHeat=old.emberHeat;state.emberHeatOld=old.emberHeatOld;}STATES.put(id,state);}}
    public static void cancel(Player player){get(player).cancelAction();CANCELING.add(player.getUUID());ClientFoley.stop(player.getUUID());FirstPersonSmoke.cancel();}
    public static PlayerState get(Player p){return STATES.computeIfAbsent(p.getUUID(),id->new PlayerState());}
    public static float heat(PlayerState s,float partial){return s.emberHeatOld+(s.emberHeat-s.emberHeatOld)*Math.max(0,Math.min(1,partial));}
    public static void clear(){STATES.clear();CANCELING.clear();ClientFoley.clear();DynamicLighting.clear();FirstPersonSmoke.clear();}
    public static void tick(){for(PlayerState s:STATES.values()){s.emberHeatOld=s.emberHeat;float target=s.lit&&s.mouthVisible()&&s.action.equals("inhale")?1:0;s.emberHeat+=Math.max(-.055f,Math.min(.07f,target-s.emberHeat));if(!s.action.equals("idle"))s.actionTicks=Math.min(s.actionTicks+1,s.actionDuration);if(s.lit)s.burn=Math.max(0,s.burn-(s.action.equals("inhale")?7:1));if(s.exhaleTicks>0)s.exhaleTicks--;if(s.reverseTicks>0)s.reverseTicks--;}}
}
