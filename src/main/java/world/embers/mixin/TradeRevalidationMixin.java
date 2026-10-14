package world.embers.mixin;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.item.ItemStack;

/** Refresh the live payment requirement before either normal or shift-click extraction. */
@Mixin(MerchantMenu.class)
public abstract class TradeRevalidationMixin extends AbstractContainerMenu {
    @Shadow(aliases="f_40028_") @Final private MerchantContainer tradeContainer;
    protected TradeRevalidationMixin(MenuType<?> type,int id){super(type,id);}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(!player.level().isClientSide)tradeContainer.updateSellItem();super.clicked(slot,button,type,player);}
    @Inject(method="quickMoveStack",at=@At("HEAD"))
    private void embers$refresh(Player player,int index,CallbackInfoReturnable<ItemStack> ci){if(index==2&&!player.level().isClientSide)tradeContainer.updateSellItem();}
}
