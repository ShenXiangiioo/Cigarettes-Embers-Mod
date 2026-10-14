package world.embers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
public final class AshtrayItem extends BlockItem {
    public AshtrayItem(Block block){super(block,new Properties());}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> c){c.accept(new world.embers.client.ItemExtension());}
}
