package fi.dy.masa.itemscroller.event;

import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.AccessorUtils;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.util.InputUtils;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();

    private final Minecraft mc = Minecraft.getInstance();
    private int recipeListX;
    private int recipeListY;
    private int recipesPerColumn;
    private int columnWidth;
    private int columns;
    private int numberTextWidth;
    private int gapColumn;
    private int entryHeight;
    private double scale;

    public static RenderEventHandler instance()
    {
        return INSTANCE;
    }

    public void renderRecipeView(GuiGraphics drawContext, Minecraft mc, int mouseX, int mouseY)
    {
        if (GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen<?> gui &&
            InputUtils.isRecipeViewOpen())
        {
            RecipeStorage recipes = RecipeStorage.getInstance();
            final int first = recipes.getFirstVisibleRecipeId();
            final int countPerPage = recipes.getRecipeCountPerPage();
            final int lastOnPage = first + countPerPage - 1;

            this.calculateRecipePositions(gui);

            drawContext.pose().pushMatrix();
            drawContext.pose().translate(this.recipeListX, this.recipeListY);
            drawContext.pose().scale((float) this.scale, (float) this.scale);

            String str = StringUtils.translate("itemscroller.gui.label.recipe_page", (first / countPerPage) + 1, recipes.getTotalRecipeCount() / countPerPage);

            drawContext.drawString(mc.font, str, 16, -12, 0xC0C0C0C0, false);

            for (int i = 0, recipeId = first; recipeId <= lastOnPage; ++i, ++recipeId)
            {
                ItemStack stack = recipes.getRecipe(recipeId).getResult();
                boolean selected = recipeId == recipes.getSelection();
                int row = i % this.recipesPerColumn;
                int column = i / this.recipesPerColumn;

                this.renderStoredRecipeStack(drawContext, stack, recipeId, row, column, gui, selected);
            }

            if (Configs.Generic.CRAFTING_RENDER_RECIPE_ITEMS.getBooleanValue())
            {
                final int recipeId = this.getHoveredRecipeId(mouseX, mouseY, recipes, gui);
                RecipePattern recipe = recipeId >= 0 ? recipes.getRecipe(recipeId) : recipes.getSelectedRecipe();

                this.renderRecipeItems(drawContext, recipe, recipes.getRecipeCountPerPage(), gui);
            }

            drawContext.pose().popMatrix();
        }
    }

    public void onDrawScreenPost(GuiGraphics drawContext, Minecraft mc, int mouseX, int mouseY)
    {
        this.renderRecipeView(drawContext, mc, mouseX, mouseY);

        if (GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen<?> gui)
        {
            int bufferedCount = ClickPacketBuffer.getBufferedActionsCount();

            if (bufferedCount > 0)
            {
                drawContext.drawString(mc.font, "Buffered slot clicks: " + bufferedCount, 10, 10, 0xFFD0D0D0, false);
            }

            if (InputUtils.isRecipeViewOpen())
            {
                RecipeStorage recipes = RecipeStorage.getInstance();
                final int recipeId = this.getHoveredRecipeId(mouseX, mouseY, recipes, gui);

                drawContext.pose().pushMatrix();
                drawContext.pose().translate(0, 0);      // z = 300.f

                if (recipeId >= 0)
                {
                    RecipePattern recipe = recipes.getRecipe(recipeId);
                    this.renderHoverTooltip(drawContext, mouseX, mouseY, recipe, gui);
                }
                else if (Configs.Generic.CRAFTING_RENDER_RECIPE_ITEMS.getBooleanValue())
                {
                    RecipePattern recipe = recipes.getSelectedRecipe();
                    ItemStack stack = this.getHoveredRecipeIngredient(mouseX, mouseY, recipe, recipes.getRecipeCountPerPage(), gui);

                    if (!InventoryUtils.isStackEmpty(stack))
                    {
                        InventoryOverlay.renderStackToolTip(drawContext, (int) mouseX, (int) mouseY, stack, mc);
                    }
                }

                drawContext.pose().popMatrix();
            }
        }
    }

    private void calculateRecipePositions(AbstractContainerScreen<?> gui)
    {
        RecipeStorage recipes = RecipeStorage.getInstance();
        final int gapHorizontal = 2;
        final int gapVertical = 2;
        final int stackBaseHeight = 16;

        this.recipesPerColumn = 9;
        this.columns = (int) Math.ceil((double) recipes.getRecipeCountPerPage() / (double) this.recipesPerColumn);
        this.numberTextWidth = 12;
        this.gapColumn = 4;

        int usableHeight = GuiUtils.getScaledWindowHeight();
        int usableWidth = AccessorUtils.getGuiLeft(gui);
        // Scale the maximum stack size by taking into account the relative gap size
        double gapScaleVertical = (1D - (double) gapVertical / (double) (stackBaseHeight + gapVertical));
        // the +1.2 is for the gap and page text height on the top and bottom
        int maxStackDimensionsVertical = (int) ((usableHeight / ((double) this.recipesPerColumn + 1.2)) * gapScaleVertical);
        // assume a maximum of 3x3 recipe size for now... thus columns + 3 stacks rendered horizontally
        double gapScaleHorizontal = (1D - (double) gapHorizontal / (double) (stackBaseHeight + gapHorizontal));
        int maxStackDimensionsHorizontal = (int) (((usableWidth - (this.columns * (this.numberTextWidth + this.gapColumn))) / (this.columns + 3 + 0.8)) * gapScaleHorizontal);
        int stackDimensions = (int) Math.min(maxStackDimensionsVertical, maxStackDimensionsHorizontal);

        this.scale = (double) stackDimensions / (double) stackBaseHeight;
        this.entryHeight = stackBaseHeight + gapVertical;
        this.recipeListX = usableWidth - (int) ((this.columns * (stackBaseHeight + this.numberTextWidth + this.gapColumn) + gapHorizontal) * this.scale);
        this.recipeListY = (int) (this.entryHeight * this.scale);
        this.columnWidth = stackBaseHeight + this.numberTextWidth + this.gapColumn;
    }

    private void renderHoverTooltip(GuiGraphics drawContext, double mouseX, double mouseY, RecipePattern recipe,
                                    AbstractContainerScreen<?> gui)
    {
        ItemStack stack = recipe.getResult();

        if (!InventoryUtils.isStackEmpty(stack))
        {
            InventoryOverlay.renderStackToolTip(drawContext, (int) mouseX, (int) mouseY, stack, this.mc);
        }
    }

    public int getHoveredRecipeId(int mouseX, int mouseY, RecipeStorage recipes, AbstractContainerScreen<?> gui)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            this.calculateRecipePositions(gui);
            final int stackDimensions = (int) (16 * this.scale);

            for (int column = 0; column < this.columns; ++column)
            {
                int startX = this.recipeListX + (int) ((column * this.columnWidth + this.gapColumn + this.numberTextWidth) * this.scale);

                if (mouseX >= startX && mouseX <= startX + stackDimensions)
                {
                    for (int row = 0; row < this.recipesPerColumn; ++row)
                    {
                        int startY = this.recipeListY + (int) (row * this.entryHeight * this.scale);

                        if (mouseY >= startY && mouseY <= startY + stackDimensions)
                        {
                            return recipes.getFirstVisibleRecipeId() + column * this.recipesPerColumn + row;
                        }
                    }
                }
            }
        }

        return -1;
    }

    private void renderStoredRecipeStack(GuiGraphics drawContext, ItemStack stack, int recipeId, int row, int column,
                                         AbstractContainerScreen<?> gui, boolean selected)
    {
        final Font font = this.mc.font;
        final String indexStr = String.valueOf(recipeId + 1);

        int x = column * this.columnWidth + this.gapColumn + this.numberTextWidth;
        int y = row * this.entryHeight;
        this.renderStackAt(drawContext, stack, x, y, selected);

        float scale = 0.75F;
        x = x - (int) (font.width(indexStr) * scale) - 2;
        y = row * this.entryHeight + this.entryHeight / 2 - font.lineHeight / 2;

        drawContext.pose().pushMatrix();
        drawContext.pose().translate(x, y);
        drawContext.pose().scale(scale, scale);

        drawContext.drawString(font, indexStr, 0, 0, 0xFFC0C0C0, false);

        drawContext.pose().popMatrix();
    }

    private void renderRecipeItems(GuiGraphics drawContext,
                                   RecipePattern recipe, int recipeCountPerPage,
                                   AbstractContainerScreen<?> gui)
    {
        ItemStack[] items = recipe.getRecipeItems();
        final int recipeDimensions = (int) Math.ceil(Math.sqrt(Math.min(recipe.getRecipeLength(), 9)));
        int x = -3 * 17 + 2;
        int y = 3 * this.entryHeight;

        for (int i = 0, row = 0; row < recipeDimensions; row++)
        {
            for (int col = 0; col < recipeDimensions; col++, i++)
            {
                //int xOff = col * 17;
                //int yOff = row * 17;
                int xOff = col > 0 ? col * 17 : 0;
                int yOff = row > 0 ? row * 17 : 0;

                this.renderStackAt(drawContext, items[i], x + xOff, y + yOff, false);
            }
        }
    }

    private ItemStack getHoveredRecipeIngredient(int mouseX, int mouseY,
                                                 RecipePattern recipe, int recipeCountPerPage,
                                                 AbstractContainerScreen<?> gui)
    {
        final int recipeDimensions = (int) Math.ceil(Math.sqrt(Math.min(recipe.getRecipeLength(), 9)));
        int scaledStackDimensions = (int) (16 * this.scale);
        int scaledGridEntry = (int) (17 * this.scale);
        int x = this.recipeListX - (int) ((3 * 17 - 2) * this.scale);
        int y = this.recipeListY + (int) (3 * this.entryHeight * this.scale);

        if (mouseX >= x && mouseX <= x + recipeDimensions * scaledGridEntry &&
            mouseY >= y && mouseY <= y + recipeDimensions * scaledGridEntry)
        {
            for (int i = 0, row = 0; row < recipeDimensions; row++)
            {
                for (int col = 0; col < recipeDimensions; col++, i++)
                {
                    int xOff = col * scaledGridEntry;
                    int yOff = row * scaledGridEntry;
                    int xStart = x + xOff;
                    int yStart = y + yOff;

                    if (mouseX >= xStart && mouseX < xStart + scaledStackDimensions &&
                        mouseY >= yStart && mouseY < yStart + scaledStackDimensions)
                    {
                        return recipe.getRecipeItems()[i];
                    }
                }
            }
        }

        return ItemStack.EMPTY;
    }

    private void renderStackAt(GuiGraphics drawContext, ItemStack stack, int x, int y, boolean border)
    {
        final int w = 16;
//        int xAdj = (int) ((x) * this.scale) + this.recipeListX;
//        int yAdj = (int) ((y) * this.scale) + this.recipeListY;
//        int wAdj = (int) ((w) * this.scale);

        if (border)
        {
            // Draw a light/white border around the stack
            RenderUtils.drawOutline(drawContext, x - 1, y - 1, w + 2, w + 2, 0xFFFFFFFF);
        }

        // light background for the item
        RenderUtils.drawRect(drawContext, x, y, w, w, 0x20FFFFFF);

        if (!InventoryUtils.isStackEmpty(stack))
        {
            stack = stack.copy();
            InventoryUtils.setStackSize(stack, 1);

            drawContext.pose().pushMatrix();
            drawContext.pose().translate(0, 0);      // z = 100.f
            drawContext.renderItem(stack, x, y);
            drawContext.pose().popMatrix();
        }
    }
}
