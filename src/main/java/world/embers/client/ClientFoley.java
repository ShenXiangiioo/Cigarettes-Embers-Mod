package world.embers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import world.embers.*;
import java.util.*;

/** Owner-local foley remains audible; other players hear positional sound. */
public final class ClientFoley {
    private static final Map<UUID,List<SoundInstance>> ACTIONS=new HashMap<>();
    private static final Map<String,CombustionLoop> LOOPS=new HashMap<>();
    private static class CombustionLoop extends AbstractTickableSoundInstance {
        final UUID player;final boolean lighter;final String event;
        CombustionLoop(UUID player,boolean lighter,String event){
            super(Embers.sound(event),SoundSource.PLAYERS,RandomSource.create());
            this.player=player;this.lighter=lighter;this.event=event;looping=true;delay=0;volume=.3f;update();
        }
        boolean update(){
            var mc=Minecraft.getInstance();var p=mc.level==null?null:mc.level.getPlayerByUUID(player);
            if(p==null||!p.isAlive()||p.isUnderWater())return false;
            PlayerState s=ClientState.get(p);
            if(lighter?!s.action.equals("lighter_burn")||!event.equals(LighterKind.byId(s.lighter).id+"_burn_loop"):!s.mouthVisible()||!s.lit)return false;
            relative=p==mc.player;attenuation=relative?Attenuation.NONE:Attenuation.LINEAR;
            x=relative?0:p.getX();y=relative?0:p.getEyeY();z=relative?0:p.getZ();
            volume=lighter?.70f:s.action.equals("inhale")?.12f:.40f;return true;
        }
        @Override public void tick(){if(!update())stop();}
    }
    public static void tick(){
        var mc=Minecraft.getInstance();
        LOOPS.entrySet().removeIf(e->{if(!e.getValue().update()){mc.getSoundManager().stop(e.getValue());return true;}return false;});
        if(mc.level==null)return;
        for(var p:mc.level.players()){
            if(!p.isAlive()||p.isUnderWater()||p.isSpectator())continue;
            var s=ClientState.get(p);
            if(s.action.equals("lighter_burn"))loop(p.getUUID(),true,LighterKind.byId(s.lighter).id+"_burn_loop");
        }
    }
    private static void loop(UUID id,boolean lighter,String event){
        String key=id+":"+event;
        if(!LOOPS.containsKey(key)){var sound=new CombustionLoop(id,lighter,event);LOOPS.put(key,sound);Minecraft.getInstance().getSoundManager().play(sound);}
    }
    public static int loopCount(){return LOOPS.size();}
    public static void accept(Network.Foley message){
        Minecraft mc=Minecraft.getInstance();
        if(message.sound().isEmpty()){stop(message.player());return;}
        if(mc.level==null||mc.player==null||!Embers.AUDIO.containsKey(message.sound()))return;
        var player=mc.level.getPlayerByUUID(message.player());if(player==null)return;
        boolean local=player==mc.player;
        var sound=new SimpleSoundInstance(Embers.sound(message.sound()).getLocation(),SoundSource.PLAYERS,message.volume(),message.pitch(),RandomSource.create(),false,0,
            local?SoundInstance.Attenuation.NONE:SoundInstance.Attenuation.LINEAR,
            local?0:player.getX(),local?0:player.getEyeY(),local?0:player.getZ(),local);
        mc.getSoundManager().play(sound);
        if(!message.sound().equals("heartbeat")&&!message.sound().equals("breath")&&!message.sound().startsWith("sigh")){
            var active=ACTIONS.computeIfAbsent(message.player(),id->new ArrayList<>());
            active.removeIf(s->!mc.getSoundManager().isActive(s));active.add(sound);
        }
    }
    public static void stop(UUID id){var sounds=ACTIONS.remove(id);if(sounds!=null)for(var sound:sounds)Minecraft.getInstance().getSoundManager().stop(sound);}
    public static void clear(){for(var id:List.copyOf(ACTIONS.keySet()))stop(id);for(var sound:LOOPS.values())Minecraft.getInstance().getSoundManager().stop(sound);LOOPS.clear();}
}
