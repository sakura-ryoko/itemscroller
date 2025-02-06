package fi.dy.masa.itemscroller.recipes;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nonnull;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.slot.Slot;

import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
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

    public void storeCraftingRecipeToCurrentSelection(Slot slot, HandledScreen<?> gui, boolean clearIfEmpty, boolean fromKeybind, MinecraftClient mc)
    {
        this.storeCraftingRecipe(this.getSelection(), slot, gui, clearIfEmpty, fromKeybind, mc);
    }

    public void storeCraftingRecipe(int index, Slot slot, HandledScreen<?> gui, boolean clearIfEmpty, boolean fromKeybind, MinecraftClient mc)
    {
        this.getRecipe(index).storeCraftingRecipe(slot, gui, clearIfEmpty, fromKeybind, mc);
        this.dirty = true;
    }

    public void clearRecipe(int index)
    {
        this.getRecipe(index).clearRecipe();
        this.dirty = true;
    }

    // TODO 1.21.2+
    /*
    public void onAddToRecipeBook(RecipeDisplayEntry entry)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (RecipePattern recipe : this.recipes)
        {
            if (!recipe.isEmpty())
            {
                if (recipe.matchClientRecipeBookEntry(entry, mc))
                {
                    ItemScroller.debugLog("onAddToRecipeBook(): Positive Match for result stack: [{}] networkId [{}]", recipe.getResult().toString(), entry.id().index());
                    recipe.storeNetworkRecipeId(entry.id());
                    recipe.storeRecipeCategory(entry.category());
                    recipe.storeRecipeDisplayEntry(entry);
                }
            }
        }
    }
     */

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

                // TODO 1.21.2+
                /*
                if (tag.contains("RecipeCategory", Constants.NBT.TAG_STRING))
                {
                    this.recipes[index].storeRecipeCategory(RecipeUtils.getRecipeCategoryFromId(tag.getString("RecipeCategory")));
                }
                if (tag.contains("LastNetworkId"))
                {
                    this.recipes[index].storeNetworkRecipeId(new NetworkRecipeId(tag.getInt("LastNetworkId")));
                }
                 */
            }
        }

        this.changeSelectedRecipe(nbt.getByte("Selected"));
    }

    private NbtCompound writeToNBT(@Nonnull DynamicRegistryManager registry)
    {
        NbtList tagRecipes = new NbtList();
        NbtCompound nbt = new NbtCompound();

        for (int i = 0; i < this.recipes.length; i++)
        {
            if (this.recipes[i].isValid())
            {
                RecipePattern entry = this.recipes[i];
                NbtCompound tag = entry.writeToNBT(registry);
                tag.putByte("RecipeIndex", (byte) i);

                // TODO 1.21.2+
                /*
                if (entry.getRecipeCategory() != null)
                {
                    String id = RecipeUtils.getRecipeCategoryId(entry.getRecipeCategory());

                    if (!id.isEmpty())
                    {
                        tag.putString("RecipeCategory", id);
                    }
                }
                if (entry.getNetworkRecipeId() != null)
                {
                    tag.putInt("LastNetworkId", entry.getNetworkRecipeId().index());
                }
                 */

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

    private Path getSaveDirAsPath()
    {
        return FileUtils.getMinecraftDirectoryAsPath().resolve(Reference.MOD_ID);
    }

    public void readFromDisk(@Nonnull DynamicRegistryManager registry)
    {
        try
        {
            Path saveDir = this.getSaveDirAsPath();

            if (Files.isDirectory(saveDir))
            {
                Path file = saveDir.resolve(this.getFileName());

                if (Files.exists(file))
                {
                    NbtCompound nbtIn = NbtUtils.readNbtFromFileAsPath(file, NbtSizeTracker.ofUnlimitedBytes());

                    if (nbtIn != null && !nbtIn.isEmpty())
                    {
                        this.initRecipes();
                        this.readFromNBT(nbtIn, registry);

                        //ItemScroller.debugLog("readFromDisk(): Successfully loaded recipe's from file '{}'", file.toAbsolutePath());
                    }
                    else
                    {
                        ItemScroller.LOGGER.warn("readFromDisk(): Error reading recipes from file '{}'", file.toAbsolutePath());
                    }
                }
                // File does not exist
            }
            else
            {
                ItemScroller.LOGGER.warn("readFromDisk(): Error reading recipes saveDir '{}'", saveDir.toAbsolutePath());
            }
        }
        catch (Exception e)
        {
            ItemScroller.LOGGER.warn("readFromDisk(): Failed to read recipes from file", e);
        }
    }

    public void writeToDisk(@Nonnull DynamicRegistryManager registry)
    {
        if (this.dirty)
        {
            try
            {
                Path saveDir = this.getSaveDirAsPath();

                if (!Files.exists(saveDir))
                {
                    FileUtils.createDirectoriesIfMissing(saveDir);
                    //ItemScroller.debugLog("writeToDisk(): Creating directory '{}'.", saveDir.toAbsolutePath());
                }

                if (Files.isDirectory(saveDir))
                {
                    Path fileTmp = saveDir.resolve(this.getFileName() + ".tmp");
                    Path fileReal = saveDir.resolve(this.getFileName());

                    NbtUtils.writeCompressed(this.writeToNBT(registry), fileTmp);

                    if (Files.exists(fileReal))
                    {
                        Files.delete(fileReal);
                    }

                    Files.move(fileTmp, fileReal);

                    //ItemScroller.debugLog("writeToDisk(): Successfully saved recipes file '{}'", fileReal.toAbsolutePath());
                    this.dirty = false;
                }
            }
            catch (Exception e)
            {
                ItemScroller.LOGGER.warn("writeToDisk(): Failed to write recipes to file!", e);
            }
        }
    }
}
