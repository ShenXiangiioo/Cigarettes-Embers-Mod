package world.embers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.lang.reflect.Method;

/** Let blocks with a real use action (including third-party chairs) receive the vanilla click. */
public final class InteractionPriority {
    private static final Class<?>[] USE_ARGS={BlockState.class,Level.class,BlockPos.class,Player.class,InteractionHand.class,BlockHitResult.class};
    private static final Method BASE_USE=findBaseUse();
    private static Method findBaseUse(){
        for(String name:new String[]{"use","m_6227_"})try{return Block.class.getMethod(name,USE_ARGS);}catch(NoSuchMethodException ignored){}
        throw new IllegalStateException("Minecraft block use method unavailable");
    }
    private static final ClassValue<Boolean> INTERACTIVE=new ClassValue<>(){
        @Override protected Boolean computeValue(Class<?> type){
            try{return type.getMethod(BASE_USE.getName(),USE_ARGS).getDeclaringClass()!=BASE_USE.getDeclaringClass();}
            catch(NoSuchMethodException e){return true;}
        }
    };
    public static boolean blockOrEntity(){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return false;
        if(mc.hitResult instanceof EntityHitResult h){if(world.embers.MobSmoking.supported(h.getEntity())&&mc.player.isShiftKeyDown()&&(mc.player.getMainHandItem().getItem() instanceof world.embers.SmokingItem s&&s.pack||mc.player.getMainHandItem().getItem() instanceof world.embers.LighterItem))return false;return !(h.getEntity() instanceof Player)&&h.getEntity()!=mc.player.getVehicle();}
        if(!(mc.hitResult instanceof BlockHitResult h)||h.getType()!=HitResult.Type.BLOCK)return false;
        var state=mc.level.getBlockState(h.getBlockPos());
        return INTERACTIVE.get(state.getBlock().getClass())||state.getMenuProvider(mc.level,h.getBlockPos())!=null;
    }
}
