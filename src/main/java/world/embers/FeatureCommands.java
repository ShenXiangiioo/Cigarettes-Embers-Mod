package world.embers;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Embers.ID)
public final class FeatureCommands {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        var root=Commands.literal("embers").requires(s->s.hasPermission(2));var features=Commands.literal("features");
        String[] keys={"benefits","dragon_slayer","soaring_dragon","apple_lover","satiation","addiction","mob_gifts","villager_discount"};
        for(int i=0;i<keys.length;i++){int index=i;features.then(Commands.literal(keys[i]).then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{PressureSettings.get(c.getSource().getServer()).feature(index,BoolArgumentType.getBool(c,"enabled"));c.getSource().sendSuccess(()->Component.translatable("command.embers.features"),true);return 1;})));}
        e.getDispatcher().register(root.then(features));
    }
}
