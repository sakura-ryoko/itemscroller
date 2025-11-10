package fi.dy.masa.itemscroller.mixin.screen;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.gui.ItemScrollerIcons;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import fi.dy.masa.itemscroller.villager.*;

@Mixin(MerchantScreen.class)
public abstract class MixinMerchantScreen extends AbstractContainerScreen<MerchantMenu>
{
    @Unique @Nullable private FavoriteData favoriteData;
    @Shadow private int shopItem;
    @Shadow int scrollOff;
    @Unique private int indexStartOffsetLast = -1;
    @Shadow protected abstract boolean canScroll(int listSize);

    private MixinMerchantScreen(MerchantMenu handler, Inventory inventory, Component title)
    {
        super(handler, inventory, title);
    }

    @Inject(
            method = "renderContents",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;renderScroller(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/item/trading/MerchantOffers;)V")
    )
    private void fixRenderScrollBar(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
            Configs.Generic.VILLAGER_TRADE_LIST_REMEMBER_SCROLL.getBooleanValue())
        {
            VillagerData data = VillagerDataStorage.getInstance().getDataForLastInteractionTarget();
            int listSize = this.menu.getOffers().size();

            if (data != null && this.canScroll(listSize))
            {
                this.scrollOff = this.getClampedIndex(data.getTradeListPosition());
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("RETURN"))
    private void onMouseScrollPost(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
            Configs.Generic.VILLAGER_TRADE_LIST_REMEMBER_SCROLL.getBooleanValue() &&
            this.indexStartOffsetLast != this.scrollOff)
        {
            int index = this.getClampedIndex(this.scrollOff);
            VillagerDataStorage.getInstance().setTradeListPosition(index);
            this.indexStartOffsetLast = index;
        }
    }

    @Inject(method = "mouseDragged", at = @At("RETURN"))
    private void onMouseDragPost(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
            Configs.Generic.VILLAGER_TRADE_LIST_REMEMBER_SCROLL.getBooleanValue() &&
            this.indexStartOffsetLast != this.scrollOff)
        {
            int index = this.getClampedIndex(this.scrollOff);
            VillagerDataStorage.getInstance().setTradeListPosition(index);
            this.indexStartOffsetLast = index;
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
        {
            int visibleIndex = this.getHoveredTradeButtonIndex(click.x(), click.y());
            int realIndex = VillagerUtils.getRealTradeIndexFor(visibleIndex, this.menu);

            if (realIndex >= 0)
            {
                // right click, trade everything with this trade
                if (click.input() == 1)
                {
                    InventoryUtils.villagerTradeEverythingPossibleWithTrade(visibleIndex);
                    cir.setReturnValue(true);
                }
                // Middle click, toggle trade favorite
                else if (click.input() == 2)
                {
                    if (Hotkeys.MODIFIER_TOGGLE_VILLAGER_GLOBAL_FAVORITE.getKeybind().isKeybindHeld())
                    {
                        MerchantOffer trade = this.menu.getOffers().get(visibleIndex);
                        VillagerDataStorage.getInstance().toggleGlobalFavorite(trade);
                    }
                    else
                    {
                        VillagerDataStorage.getInstance().toggleFavorite(realIndex);
                    }

                    this.favoriteData = null; // Force a re-build of the list

                    // Rebuild the custom list based on the new favorites (See the Mixin for MerchantScreenHandler#setOffers())
                    this.menu.setOffers(((IMerchantScreenHandler) this.menu).itemscroller$getOriginalList());

                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "postButtonClick", at = @At("HEAD"), cancellable = true)
    private void fixRecipeIndex(CallbackInfo ci)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue() &&
            this.getMenu() instanceof IMerchantScreenHandler)
        {
            if (VillagerUtils.switchToTradeByVisibleIndex(this.shopItem))
            {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderContents", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;tradeOfferButtons:[Lnet/minecraft/client/gui/screens/inventory/MerchantScreen$TradeOfferButton;"))
    private void renderFavoriteMarker(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
        {
            FavoriteData favoriteData = this.favoriteData;

            if (favoriteData == null)
            {
                favoriteData = VillagerDataStorage.getInstance().getFavoritesForCurrentVillager(this.menu);
                this.favoriteData = favoriteData;
            }

            int numFavorites = favoriteData.favorites.size();

            if (numFavorites > 0 && this.scrollOff < numFavorites)
            {
                int screenX = (this.width - this.imageWidth) / 2;
                int screenY = (this.height - this.imageHeight) / 2;
                int buttonsStartX = screenX + 5;
                int buttonsStartY = screenY + 16 + 2;
                int x = buttonsStartX + 89 - 8;
                int y = buttonsStartY + 2;
                float z = 300;
                IGuiIcon icon = favoriteData.isGlobal ? ItemScrollerIcons.STAR_5_PURPLE : ItemScrollerIcons.STAR_5_YELLOW;

                for (int i = 0; i < (numFavorites - this.scrollOff); ++i)
                {
                    //RenderUtils.bindTexture(icon.getTexture());
                    icon.renderAt(context, x, y, z, false, false);
                    y += 20;
                }
            }
        }
    }

    @Unique
    private int getClampedIndex(int index)
    {
        int listSize = this.menu.getOffers().size();
        return Math.max(0, Math.min(index, listSize - 7));
    }

    @Unique
    private int getHoveredTradeButtonIndex(double mouseX, double mouseY)
    {
        int screenX = (this.width - this.imageWidth) / 2;
        int screenY = (this.height - this.imageHeight) / 2;
        int buttonsStartX = screenX + 5;
        int buttonsStartY = screenY + 16 + 2;
        int buttonWidth = 89;
        int buttonHeight = 20;

        if (mouseX >= buttonsStartX && mouseX <= buttonsStartX + buttonWidth &&
            mouseY >= buttonsStartY && mouseY <= buttonsStartY + 7 * buttonHeight)
        {
            return this.scrollOff + (((int) mouseY - buttonsStartY) / buttonHeight);
        }

        return -1;
    }
}
