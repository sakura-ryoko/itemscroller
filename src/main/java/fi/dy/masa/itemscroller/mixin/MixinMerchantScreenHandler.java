package fi.dy.masa.itemscroller.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.village.Merchant;
import net.minecraft.village.TradeOfferList;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.villager.IMerchantScreenHandler;
import fi.dy.masa.itemscroller.villager.VillagerUtils;

@Mixin(MerchantScreenHandler.class)
public abstract class MixinMerchantScreenHandler extends ScreenHandler implements IMerchantScreenHandler
{
    @Shadow @Final private Merchant merchant;
    @Unique
    @Nullable private TradeOfferList customList;

    protected MixinMerchantScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId)
    {
        super(type, syncId);
    }

    @Inject(method = "getRecipes", at = @At("HEAD"), cancellable = true)
    private void replaceTradeList(CallbackInfoReturnable<TradeOfferList> cir)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
        {
            if (this.customList != null)
            {
                //ItemScroller.printDebug("onTradeListSet(): this.customList.size(): {}", this.customList.size());

                cir.setReturnValue(this.customList);
            }
            /*
            else
            {
                ItemScroller.logger.error("onTradeListSet(): this.customList == NULL");

                if (!this.merchant.getOffers().isEmpty())
                {
                    ItemScroller.logger.warn("onTradeListSet(): this.customList --> setting from this.merchant.getOffers()");

                    this.customList = VillagerUtils.buildCustomTradeList(this.merchant.getOffers());
                }
                else
                {
                    ItemScroller.logger.warn("onTradeListSet(): this.merchant.getOffers() isEmpty()");
                }
            }
             */
        }
    }

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void onTradeListSet(TradeOfferList offers, CallbackInfo ci)
    {
        //ItemScroller.printDebug("onTradeListSet(): offers.size(): {}", offers.size());

        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
        {
            if (this.customList == null || this.customList.isEmpty())
            {
                this.customList = VillagerUtils.buildCustomTradeList(offers);
                //ItemScroller.printDebug("onTradeListSet(): [NEW] this.customList.size(): {}", this.customList.size());
            }
            else if (this.customList.size() != offers.size())
            {
                //ItemScroller.printDebug("onTradeListSet(): this.customList.size() != offers.size() --> REBUILD");

                this.customList = VillagerUtils.buildCustomTradeList(offers);

                //ItemScroller.printDebug("onTradeListSet(): [REBUILD] this.customList.size(): {}", this.customList.size());
            }
        }
    }

    @Override
    public TradeOfferList itemscroller$getOriginalList()
    {
        return this.merchant.getOffers();
    }
}
