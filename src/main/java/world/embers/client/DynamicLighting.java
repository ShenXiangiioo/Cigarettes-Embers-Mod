package world.embers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import world.embers.PlayerState;
import java.util.*;

/** Client light propagation only: no placed blocks, saved-world edits or server light changes. */
public final class DynamicLighting {
    private static volatile Map<Long,Integer> sources=Map.of();
    private static volatile LayerLightEventListener engine;
    public static int emission(Object queriedEngine,long position){return queriedEngine==engine&&sources.containsKey(position)?sources.get(position):0;}
    public static boolean flame(PlayerState s){
        int t=s.actionTicks;
        return switch(s.action){
            case "lighter_burn" -> true;
            case "lighting" -> t>=(s.addictionStrikes==3?20:s.retry?18:11)&&t<27;
            case "share_fire" -> t>=12;
            case "share_done" -> true;
            case "fidget" -> t>=8&&t<s.actionDuration-6&&(s.lighter>=5||s.variant==0||t<13||t>25);
            default -> false;
        };
    }
    public static void tick(){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null){clear();return;}
        var light=mc.level.getLightEngine().getLayerListener(LightLayer.BLOCK);
        if(light!=engine){sources=Map.of();engine=light;}
        Map<Long,Integer> next=new HashMap<>();
        for(var p:mc.level.players()){
            if(!p.isAlive()||p.isUnderWater()||p.isSpectator())continue;
            PlayerState s=ClientState.get(p);
            if(s.mouth>=0&&s.lit||flame(s))next.merge(BlockPos.containing(p.getEyePosition()).asLong(),flame(s)?(s.action.equals("lighter_burn")?15:12):s.action.equals("inhale")?Math.min(8,3+s.actionTicks):3,Math::max);
        }
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.Mob&&entity.isAlive()&&!entity.isUnderWater()&&world.embers.MobSmoking.lit(entity))next.merge(BlockPos.containing(entity.getEyePosition()).asLong(),3,Math::max);
        if(next.equals(sources))return;
        Set<Long> changed=new HashSet<>(sources.keySet());changed.addAll(next.keySet());sources=Map.copyOf(next);
        for(long pos:changed)light.checkBlock(BlockPos.of(pos));
    }
    public static void clear(){sources=Map.of();engine=null;}
}
