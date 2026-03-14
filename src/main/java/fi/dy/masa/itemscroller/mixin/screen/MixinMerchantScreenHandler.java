package fi.dy.masa.itemscroller.mixin.screen;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.villager.IMerchantScreenHandler;
import fi.dy.masa.itemscroller.villager.VillagerUtils;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;

@Mixin(MerchantMenu.class)
public abstract class MixinMerchantScreenHandler extends AbstractContainerMenu implements IMerchantScreenHandler
{
    @Shadow @Final private Merchant trader;
    @Unique @Nullable private MerchantOffers customList;

    protected MixinMerchantScreenHandler(@Nullable MenuType<?> type, int syncId)
    {
        super(type, syncId);
    }

    @Inject(method = "getOffers", at = @At("HEAD"), cancellable = true)
    private void itemscroller_replaceTradeList(CallbackInfoReturnable<MerchantOffers> cir)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() && this.customList != null)
        {
            cir.setReturnValue(this.customList);
        }
    }

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void itemscroller_onTradeListSet(MerchantOffers offers, CallbackInfo ci)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
        {
            this.customList = VillagerUtils.buildCustomTradeList(offers);
        }
    }

    @Override
    public MerchantOffers itemscroller$getOriginalList()
    {
        return this.trader.getOffers();
    }
}
