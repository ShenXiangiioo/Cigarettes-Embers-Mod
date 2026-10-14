package world.embers;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.*;
import java.util.function.Consumer;

public class LighterItem extends Item {
    public static final int MAX_FUEL=90, TICKS_PER_USE=200;
    public final LighterKind kind;
    public LighterItem(LighterKind k){super(new Properties().stacksTo(1));kind=k;}
    public static int fuel(ItemStack s){return s.hasTag()&&s.getTag().contains("fuel")?Math.max(0,Math.min(MAX_FUEL,s.getTag().getInt("fuel"))):MAX_FUEL;}
    public static boolean spend(ItemStack s){int n=fuel(s);if(n<=0)return false;s.getOrCreateTag().putInt("fuel",n-1);return true;}
    /** Save fractional burn time on the actual stack, including across releases and reloads. */
    public static int flameTicks(ItemStack s){return s.hasTag()?Math.max(0,Math.min(TICKS_PER_USE-1,s.getTag().getInt("flameTicks"))):0;}
    public static boolean burnTick(ItemStack s){
        if(fuel(s)<=0)return false;
        int ticks=flameTicks(s)+1;
        if(ticks>=TICKS_PER_USE){spend(s);ticks=0;}
        s.getOrCreateTag().putInt("flameTicks",ticks);return fuel(s)>0;
    }
    public static boolean needsRefill(ItemStack s){return fuel(s)<MAX_FUEL||flameTicks(s)>0;}
    public static void refill(ItemStack s){s.getOrCreateTag().putInt("fuel",MAX_FUEL);s.getOrCreateTag().remove("flameTicks");}
    @Override public boolean isBarVisible(ItemStack s){return fuel(s)<MAX_FUEL;}
    @Override public int getBarWidth(ItemStack s){return Math.round(13f*fuel(s)/MAX_FUEL);}
    @Override public int getBarColor(ItemStack s){return 0xCC9A53;}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> t,TooltipFlag f){t.add(Component.translatable("tooltip.embers.fuel",fuel(s),MAX_FUEL).withStyle(ChatFormatting.GRAY));}
    @Override public boolean onEntityItemUpdate(ItemStack stack,ItemEntity entity){
        if(entity.level() instanceof ServerLevel l&&(entity.isOnFire()||entity.isInLava()||l.getBlockState(entity.blockPosition()).is(net.minecraft.tags.BlockTags.FIRE))){
            if(entity.isRemoved())return true;
            l.sendParticles(ParticleTypes.EXPLOSION,entity.getX(),entity.getY()+.1,entity.getZ(),1,0,0,0,0);
            l.playSound(null,entity.blockPosition(),Embers.sound("lighter_pop"),SoundSource.BLOCKS,.65f,1.2f);
            for(LivingEntity target:l.getEntitiesOfClass(LivingEntity.class,entity.getBoundingBox().inflate(3)))if(target.distanceToSqr(entity)<=9)target.hurt(l.damageSources().explosion(null,null),2);
            entity.discard();return true;
        }return false;
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> c){c.accept(new world.embers.client.ItemExtension());}
}
