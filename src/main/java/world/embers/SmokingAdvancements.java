package world.embers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Embers.ID)
public final class SmokingAdvancements {
    public static void award(ServerPlayer player,String name){
        var advancement=player.server.getAdvancements().getAdvancement(new ResourceLocation(Embers.ID,name));
        if(advancement!=null&&!player.getAdvancements().getOrStartProgress(advancement).isDone())player.getAdvancements().award(advancement,"first");
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)award(player,"welcome");}
}
