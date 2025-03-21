package fi.dy.masa.itemscroller.recipes;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.recipe.display.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import fi.dy.masa.itemscroller.mixin.recipe.IMixinIngredient;

public class RecipeUtils
{
    public static String getRecipeCategoryId(RecipeBookCategory category)
    {
        RegistryKey<RecipeBookCategory> key = Registries.RECIPE_BOOK_CATEGORY.getKey(category).orElse(null);

        if (key != null)
        {
            return key.getValue().toString();
        }

        return "";
    }

    public static @Nullable RecipeBookCategory getRecipeCategoryFromId(String id)
    {
        RegistryEntry.Reference<RecipeBookCategory> catReference = Registries.RECIPE_BOOK_CATEGORY.getEntry(Identifier.tryParse(id)).orElse(null);

        if (catReference != null && catReference.hasKeyAndValue())
        {
            return catReference.value();
        }

        return null;
    }

    public static boolean compareStacksAndIngredients(List<ItemStack> left, List<Ingredient> right, int count, Type type)
    {
        if (left.isEmpty() || right.isEmpty())
        {
            //System.out.print("compareStacksAndIngredients() --> EMPTY!!!\n");
            return false;
        }

        //System.out.printf("compareStacksAndIngredients() Type: [%s], count [%s] --> START\n", type.toString(), count);
        //dumpStacks(left, "LF");
        //dumpIngs(right, "RT");

        if (type == Type.SHAPELESS)
        {
            return compareShapelessRecipe(left, right);
        }
        else if (type == Type.SHAPED)
        {
            return compareShapedRecipe(left, right);
        }

        // Other recipe type
        return false;
    }

    public static boolean compareShapedRecipe(List<ItemStack> left, List<Ingredient> right)
    {
        //System.out.printf("compareShapedRecipe() --> size left [%d], right [%d]\n", left.size(), right.size());
        int lPos = 0;

        for (int i = 0; i < right.size(); i++)
        {
            ItemStack lStack = left.get(lPos);

            while (lStack.isEmpty())
            {
                lPos++;

                if (lPos < 9)
                {
                    lStack = left.get(lPos);
                    //System.out.printf(" compareShapedRecipe() [%d] left [%s] (Advance Left), right [%d]\n", lPos, lStack.toString(), i);
                }
                else
                {
                    break;
                }
            }

            if (!checkMatchingItemsEach(lStack, lPos, i, right.get(i)))
            {
                //System.out.print(" FAIL (Shaped)\n");
                return false;
            }

            lPos++;
        }

        //System.out.print(" PASS (Shaped)\n");
        return true;
    }

    public static boolean compareShapelessRecipe(List<ItemStack> left, List<Ingredient> right)
    {
        //System.out.printf("compareShapelessRecipe() --> size left [%d], right [%d]\n", left.size(), right.size());

        for (int i = 0; i < left.size(); i++)
        {
            ItemStack lStack = left.get(i);
            boolean pass = false;

            //System.out.printf(" compareShapelessRecipe() [%d] left [%s] -->\n", i, lStack.toString());
            if (lStack.isEmpty()) continue;

            for (int rPos = 0; rPos < right.size(); rPos++)
            {
                if (checkMatchingItemsEach(lStack, i, rPos, right.get(rPos)))
                {
                    //System.out.print(" PASS-EACH\n");
                    pass = true;
                }
            }

            if (!pass)
            {
                //System.out.print(" FAIL (Shapeless)\n");
                return false;
            }
        }

        //System.out.print(" PASS (Shapeless)\n");
        return true;
    }

    private static boolean checkMatchingItemsEach(ItemStack lStack, int lPos, int i, Ingredient ri)
    {
        List<RegistryEntry<Item>> rItems = ((IMixinIngredient) (Object) ri).itemscroller_getEntries().stream().toList();
        //List<RegistryEntry<Item>> rItems = ri.getMatchingItems().toList();

        for (RegistryEntry<Item> rItem : rItems)
        {
            //System.out.printf(" checkMatchingItemsEach() [%d] left [%s] / [%d] right [%s] -->", lPos, lStack, i, rItem.getIdAsString());

            if (ri.test(lStack))
            {
                //System.out.print(" valid (Test test)\n");
                return true;
            }
            else if (areStacksEqual(lStack, new ItemStack(rItem)))
            {
                //System.out.print(" valid (Stack test)\n");
                return true;
            }
        }

        //System.out.print(" !not valid (Default)\n");
        return false;
    }

    public static boolean areStacksEqual(ItemStack left, ItemStack right)
    {
        return ItemStack.areItemsEqual(left, right) && left.getCount() == right.getCount();
    }

    private static void dumpStacks(List<ItemStack> stacks, String side)
    {
        int i = 0;

        System.out.printf("DUMP [%s] -->\n", side);
        for (ItemStack stack : stacks)
        {
            System.out.printf(" %s[%d] // [%s]\n", side, i, stack.toString());
            i++;
        }
        System.out.printf("DUMP END [%s]\n", side);
    }

    private static void dumpIngs(List<Ingredient> ings, String side)
    {
        int i = 0;

        System.out.printf("DUMP [%s] -->\n", side);
        for (Ingredient ing : ings)
        {
            //List<RegistryEntry<Item>> items = ing.getMatchingItems().toList();
            List<RegistryEntry<Item>> items = ((IMixinIngredient) (Object) ing).itemscroller_getEntries().stream().toList();

            System.out.printf(" %s[%d] //", side, i);

            for (RegistryEntry<Item> item : items)
            {
                System.out.printf(" [%s]", item.getIdAsString());
            }

            System.out.print("// []\n");
            i++;
        }

        System.out.printf("DUMP END [%s]\n", side);
    }

    public enum Type
    {
        FURNACE,
        SHAPED,
        SHAPELESS,
        SMITHING,
        STONECUTTER,
        UNKNOWN;

        public static Type fromRecipeDisplay(RecipeDisplay type)
        {
            return switch (type)
            {
                case FurnaceRecipeDisplay ignored -> FURNACE;
                case ShapelessCraftingRecipeDisplay ignored -> SHAPELESS;
                case ShapedCraftingRecipeDisplay ignored -> SHAPED;
                case SmithingRecipeDisplay ignored -> SMITHING;
                case StonecutterRecipeDisplay ignored -> STONECUTTER;
                case null, default -> UNKNOWN;
            };
        }
    }
}
