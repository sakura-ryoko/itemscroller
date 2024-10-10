package fi.dy.masa.itemscroller.recipes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.slot.Slot;

import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class RecipeStorage
{
    private static final int MAX_PAGES   = 8;           // 8 Pages of 18 = 144 total slots
    private static final int MAX_RECIPES = 18;          // 8 Pages of 18 = 144 total slots
    private static final RecipeStorage INSTANCE = new RecipeStorage(MAX_RECIPES * MAX_PAGES);
    private final RecipePattern[] recipes;
    private int selected;
    private boolean dirty;

    public static RecipeStorage getInstance()
    {
        return INSTANCE;
    }

    public RecipeStorage(int recipeCount)
    {
        this.recipes = new RecipePattern[recipeCount];
        this.initRecipes();
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            this.clearRecipes();
        }
    }

    private void initRecipes()
    {
        for (int i = 0; i < this.recipes.length; i++)
        {
            this.recipes[i] = new RecipePattern();
        }
    }

    private void clearRecipes()
    {
        for (int i = 0; i < this.recipes.length; i++)
        {
            this.clearRecipe(i);
        }
    }

    public int getSelection()
    {
        return this.selected;
    }

    public void changeSelectedRecipe(int index)
    {
        if (index >= 0 && index < this.recipes.length)
        {
            this.selected = index;
            this.dirty = true;
        }
    }

    public void scrollSelection(boolean forward)
    {
        this.changeSelectedRecipe(this.selected + (forward ? 1 : -1));
    }

    public int getFirstVisibleRecipeId()
    {
        return this.getCurrentRecipePage() * this.getRecipeCountPerPage();
    }

    public int getTotalRecipeCount()
    {
        return this.recipes.length;
    }

    public int getRecipeCountPerPage()
    {
        return MAX_RECIPES;
    }

    public int getCurrentRecipePage()
    {
        return this.getSelection() / this.getRecipeCountPerPage();
    }

    /**
     * Returns the recipe for the given index.
     * If the index is invalid, then the first recipe is returned, instead of null.
     */
    @Nonnull
    public RecipePattern getRecipe(int index)
    {
        if (index >= 0 && index < this.recipes.length)
        {
            return this.recipes[index];
        }

        return this.recipes[0];
    }

    @Nonnull
    public RecipePattern getSelectedRecipe()
    {
        return this.getRecipe(this.getSelection());
    }

    public void storeCraftingRecipeToCurrentSelection(Slot slot, HandledScreen<?> gui, boolean clearIfEmpty)
    {
        this.storeCraftingRecipe(this.getSelection(), slot, gui, clearIfEmpty);
    }

    public void storeCraftingRecipe(int index, Slot slot, HandledScreen<?> gui, boolean clearIfEmpty)
    {
        this.getRecipe(index).storeCraftingRecipe(slot, gui, clearIfEmpty);
        this.dirty = true;
    }

    public void clearRecipe(int index)
    {
        this.getRecipe(index).clearRecipe();
        this.dirty = true;
    }

    public void onAddToRecipeBook(RecipeDisplayEntry entry)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (RecipePattern recipe : this.recipes)
        {
            if (recipe.matchClientRecipeBookEntry(entry, mc))
            {
                recipe.storeNetworkRecipeId(entry.id());
                recipe.storeRecipeDisplayEntry(entry);
            }
        }
    }

    private void readFromNBT(NbtCompound nbt, @Nonnull DynamicRegistryManager registryManager)
    {
        if (nbt == null || nbt.contains("Recipes", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        for (int i = 0; i < this.recipes.length; i++)
        {
            this.recipes[i].clearRecipe();
        }

        NbtList tagList = nbt.getList("Recipes", Constants.NBT.TAG_COMPOUND);
        int count = tagList.size();

        for (int i = 0; i < count; i++)
        {
            NbtCompound tag = tagList.getCompound(i);

            int index = tag.getByte("RecipeIndex");

            if (index >= 0 && index < this.recipes.length)
            {
                this.recipes[index].readFromNBT(tag, registryManager);

                if (tag.contains("LastNetworkId"))
                {
                    this.recipes[index].storeNetworkRecipeId(new NetworkRecipeId(tag.getInt("LastNetworkId")));
                }
            }
        }

        this.changeSelectedRecipe(nbt.getByte("Selected"));
    }

    private NbtCompound writeToNBT(@Nonnull DynamicRegistryManager registryManager)
    {
        NbtList tagRecipes = new NbtList();
        NbtCompound nbt = new NbtCompound();

        for (int i = 0; i < this.recipes.length; i++)
        {
            if (this.recipes[i].isValid())
            {
                RecipePattern entry = this.recipes[i];
                NbtCompound tag = entry.writeToNBT(registryManager);
                tag.putByte("RecipeIndex", (byte) i);

                if (entry.getNetworkRecipeId() != null)
                {
                    tag.putInt("LastNetworkId", entry.getNetworkRecipeId().index());
                }
                tagRecipes.add(tag);
            }
        }

        nbt.put("Recipes", tagRecipes);
        nbt.putByte("Selected", (byte) this.selected);

        return nbt;
    }

    private String getFileName()
    {
        if (Configs.Generic.SCROLL_CRAFT_RECIPE_FILE_GLOBAL.getBooleanValue() == false)
        {
            String worldName = StringUtils.getWorldOrServerName();

            if (worldName != null)
            {
                return "recipes_" + worldName + ".nbt";
            }
            else
            {
                return "recipes_unknown.nbt";
            }
        }

        return "recipes.nbt";
    }

    private File getSaveDir()
    {
        return new File(FileUtils.getMinecraftDirectory(), Reference.MOD_ID);
    }

    public void readFromDisk(@Nonnull DynamicRegistryManager registryManager)
    {
        try
        {
            File saveDir = this.getSaveDir();

            if (saveDir != null)
            {
                File file = new File(saveDir, this.getFileName());

                if (file.exists())
                {
                    if (file.isFile() && file.canRead())
                    {
                        this.initRecipes();

                        FileInputStream is = new FileInputStream(file);
                        this.readFromNBT(NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes()), registryManager);
                        is.close();
                    }
                    else
                    {
                        ItemScroller.logger.warn("RecipeStorage#readFromDisk(): Error reading recipes from file '{}'", file.getPath());
                    }
                }
            }
        }
        catch (Exception e)
        {
            ItemScroller.logger.warn("RecipeStorage#readFromDisk(): Failed to read recipes from file", e);
        }
    }

    public void writeToDisk(@Nonnull DynamicRegistryManager registryManager)
    {
        if (this.dirty)
        {
            try
            {
                File saveDir = this.getSaveDir();

                if (saveDir.exists() == false)
                {
                    if (saveDir.mkdirs() == false)
                    {
                        ItemScroller.logger.error("RecipeStorage#writeToDisk(): Failed to create the recipe storage directory '{}'", saveDir.getPath());
                        return;
                    }
                }

                File fileTmp  = new File(saveDir, this.getFileName() + ".tmp");
                File fileReal = new File(saveDir, this.getFileName());
                FileOutputStream os = new FileOutputStream(fileTmp);
                NbtIo.writeCompressed(this.writeToNBT(registryManager), os);
                os.close();

                if (fileReal.exists())
                {
                    if (fileReal.delete() == false)
                    {
                        ItemScroller.logger.warn("RecipeStorage#writeToDisk(): failed to delete file {} ", fileReal.getName());
                    }
                }

                if (fileTmp.renameTo(fileReal) == false)
                {
                    ItemScroller.logger.warn("RecipeStorage#writeToDisk(): failed to rename file {} ", fileTmp.getName());
                }
                this.dirty = false;
            }
            catch (Exception e)
            {
                ItemScroller.logger.warn("RecipeStorage#writeToDisk(): Failed to write recipes to file!", e);
            }
        }
    }
}
