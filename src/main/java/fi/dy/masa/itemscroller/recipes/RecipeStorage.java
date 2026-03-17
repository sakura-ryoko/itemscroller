package fi.dy.masa.itemscroller.recipes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.util.DataFileUtils;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.config.Configs;

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

    public void storeCraftingRecipeToCurrentSelection(Slot slot, AbstractContainerScreen<?> gui, boolean clearIfEmpty, boolean fromKeybind, Minecraft mc)
    {
        this.storeCraftingRecipe(this.getSelection(), slot, gui, clearIfEmpty, fromKeybind, mc);
    }

    public void storeCraftingRecipe(int index, Slot slot, AbstractContainerScreen<?> gui, boolean clearIfEmpty, boolean fromKeybind, Minecraft mc)
    {
        this.getRecipe(index).storeCraftingRecipe(slot, gui, clearIfEmpty, fromKeybind, mc);
        this.dirty = true;
    }

    public void clearRecipe(int index)
    {
        this.getRecipe(index).clearRecipe();
        this.dirty = true;
    }

    private boolean isEmpty()
    {
        return !(this.recipes.length > 0) || this.isRecipesEmpty();
    }

    private boolean isRecipesEmpty()
    {
        boolean empty = true;

	    for (RecipePattern recipe : this.recipes)
	    {
		    if (!recipe.isEmpty())
		    {
			    empty = false;
		    }
	    }

        return empty;
    }

    public void onAddToRecipeBook(RecipeDisplayEntry entry)
    {
        Minecraft mc = Minecraft.getInstance();

        // DEBUG
//        RecipeBookUtils.toggleDebugLog(true);
//        RecipeBookUtils.toggleAnsiColorLog(true);

        for (RecipePattern recipe : this.recipes)
        {
            if (!recipe.isEmpty())
            {
                List<RecipeBookUtils.Type> types;

                if (recipe.getRecipeType() != null)
                {
                    types = List.of(recipe.getRecipeType());
                }
                else
                {
                    types = List.of(RecipeBookUtils.Type.SHAPED, RecipeBookUtils.Type.SHAPELESS);
                }

                if (RecipeBookUtils.matchClientRecipeBookEntry(recipe.getResult(), Arrays.asList(recipe.getRecipeItems()), entry, types, mc))
//                if (recipe.matchClientRecipeBookEntry(entry, mc))
                {
                    ItemScroller.debugLog("onAddToRecipeBook(): Positive Match for result stack: [{}] networkId [{}]", recipe.getResult().toString(), entry.id().index());
                    recipe.storeNetworkRecipeId(entry.id());
                    recipe.storeRecipeCategory(entry.category());
                    recipe.storeRecipeDisplayEntry(entry);
                    recipe.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(entry.display()));
                    break;
                }
            }
        }
    }

    private void readFromNBT(CompoundData data, @Nonnull RegistryAccess registryManager)
    {
        if (data == null || data.contains("Recipes", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        for (int i = 0; i < this.recipes.length; i++)
        {
            this.recipes[i].clearRecipe();
        }

        ListData tagList = data.getList("Recipes");
        int count = tagList.size();

        for (int i = 0; i < count; i++)
        {
	        CompoundData tag = tagList.getCompoundAt(i);

            int index = tag.getByte("RecipeIndex");

            if (index >= 0 && index < this.recipes.length)
            {
                this.recipes[index].readFromData(tag, registryManager);

                if (tag.contains("RecipeCategory", Constants.NBT.TAG_STRING))
                {
                    this.recipes[index].storeRecipeCategory(RecipeBookUtils.getRecipeCategoryFromId(tag.getString("RecipeCategory")));
                }
                if (tag.contains("LastNetworkId", Constants.NBT.TAG_INT))
                {
                    this.recipes[index].storeNetworkRecipeId(new RecipeDisplayId(tag.getInt("LastNetworkId")));
                }
                if (tag.contains("RecipeType", Constants.NBT.TAG_STRING))
                {
                    String recipeType = tag.getString("RecipeType");

                    if (!recipeType.isEmpty())
                    {
                        for (RecipeBookUtils.Type type : RecipeBookUtils.Type.values())
                        {
                            if (type.name().equalsIgnoreCase(recipeType))
                            {
                                this.recipes[index].storeRecipeType(type);
                            }
                        }
                    }

                }
            }
        }

        this.changeSelectedRecipe(data.getByte("Selected"));
    }

    private CompoundData writeToNBT(@Nonnull RegistryAccess registry)
    {
        ListData tagRecipes = new ListData();
        CompoundData data = new CompoundData();

        if (this.isEmpty())
        {
            return data;
        }

        for (int i = 0; i < this.recipes.length; i++)
        {
            if (this.recipes[i].isValid())
            {
                RecipePattern entry = this.recipes[i];
                CompoundData tag = entry.writeToData(registry);
                tag.putByte("RecipeIndex", (byte) i);

                if (entry.getRecipeCategory() != null)
                {
                    String id = RecipeBookUtils.getRecipeCategoryId(entry.getRecipeCategory());

                    if (!id.isEmpty())
                    {
                        tag.putString("RecipeCategory", id);
                    }
                }
                if (entry.getNetworkRecipeId() != null)
                {
                    tag.putInt("LastNetworkId", entry.getNetworkRecipeId().index());
                }
                if (entry.getRecipeType() != null)
                {
                    tag.putString("RecipeType", entry.getRecipeType().name().toLowerCase());
                }

                tagRecipes.add(tag);
            }
        }

	    data.put("Recipes", tagRecipes);
	    data.putByte("Selected", (byte) this.selected);

        return data;
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

    private Path getSaveDir()
    {
        return FileUtils.getMinecraftDirectory().resolve(Reference.MOD_ID);
    }

    public void readFromDisk(@Nonnull RegistryAccess registry)
    {
        try
        {
            Path saveDir = this.getSaveDir();

            if (Files.isDirectory(saveDir))
            {
                Path file = saveDir.resolve(this.getFileName());

                if (Files.exists(file))
                {
                    CompoundData nbtIn = DataFileUtils.readCompoundDataFromNbtFile(file);

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

    public void writeToDisk(@Nonnull RegistryAccess registry)
    {
        if (this.dirty)
        {
            try
            {
                Path saveDir = this.getSaveDir();

                if (!Files.exists(saveDir))
                {
                    FileUtils.createDirectoriesIfMissing(saveDir);
                    //ItemScroller.debugLog("writeToDisk(): Creating directory '{}'.", saveDir.toAbsolutePath());
                }

                if (Files.isDirectory(saveDir))
                {
                    Path fileTmp = saveDir.resolve(this.getFileName() + ".tmp");
                    Path fileReal = saveDir.resolve(this.getFileName());

//                    NbtUtils.writeCompressed(this.writeToNBT(registry), fileTmp);
	                CompoundData data = this.writeToNBT(registry);

                    // Don't save a file if there are no recipe's to save.
                    if (data.isEmpty())
                    {
                        if (Files.exists(fileReal))
                        {
                            Files.delete(fileReal);
                        }

                        this.dirty = false;
                        return;
                    }

	                DataFileUtils.writeCompoundDataToCompressedNbtFile(fileTmp, data);

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
