package world.embers;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;
public final class ButtItem extends Item {
    @Override public boolean hasCustomEntity(net.minecraft.world.item.ItemStack stack){return true;}
    @Override public net.minecraft.world.entity.Entity createEntity(net.minecraft.world.level.Level level,net.minecraft.world.entity.Entity original,net.minecraft.world.item.ItemStack stack){
        if(original instanceof ButtEntity)return null;
        ButtEntity result=new ButtEntity(Embers.BUTT_ENTITY.get(),level);
        if(original instanceof net.minecraft.world.entity.item.ItemEntity item){var tag=new net.minecraft.nbt.CompoundTag();item.addAdditionalSaveData(tag);result.readAdditionalSaveData(tag);}
        result.setItem(stack);result.setPos(original.position());result.setDeltaMovement(original.getDeltaMovement());result.setYRot(original.getYRot());return result;
    }
    public static Brand brand(net.minecraft.world.item.ItemStack stack){return stack.hasTag()&&stack.getTag().contains("brand")?Brand.byId(stack.getTag().getInt("brand")):Brand.CHAPMAN;}
    public static net.minecraft.world.item.ItemStack of(Brand brand){var stack=new net.minecraft.world.item.ItemStack(Embers.BUTT.get());stack.getOrCreateTag().putInt("brand",brand.ordinal());return stack;}
    public ButtItem(){super(new Properties());}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){consumer.accept(new world.embers.client.ItemExtension());}
}
