package fi.dy.masa.itemscroller.recipes;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import fi.dy.masa.itemscroller.mixin.IMixinIngredient;

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

    public static boolean compareStacksAndIngredients(List<ItemStack> left, List<Ingredient> right, int count)
    {
        if (left.isEmpty() || right.isEmpty())
        {
            //System.out.print("compare() --> EMPTY!!!\n");
            return false;
        }

        //System.out.printf("compare() --> size left [%d], right [%d]\n", left.size(), right.size());
        //dumpStacks(left, "LF");
        //dumpIngs(right, "RT");

        int lPos = 0;

        for (int i = 0; i < right.size(); i++)
        {
            ItemStack lStack = left.get(lPos);
            Ingredient ri = right.get(i);

            while (lStack.isEmpty())
            {
                lPos++;
                lStack = left.get(lPos);
                //System.out.printf("compare() [%d] left [%s] (Advance Left), right [%d]\n", lPos, lStack.toString(), i);
            }

            List<RegistryEntry<Item>> rItems = ((IMixinIngredient) (Object) ri).itemscroller_getEntries().stream().toList();
            //List<RegistryEntry<Item>> rItems = ri.getMatchingItems().toList();
            boolean match = false;

            for (RegistryEntry<Item> rItem : rItems)
            {
                //System.out.printf("compare() [%d] left [%s] / [%d] right [%s]\n", lPos, lStack, i, rItem.getIdAsString());

                if (ri.test(lStack))
                {
                    //System.out.print(" not valid (Test test)");
                    match = true;
                }
                else if (areStacksEqual(lStack, new ItemStack(rItem)))
                {
                    //System.out.print(" not valid (Stack test)");
                    match = true;
                }
            }
            if (!match)
            {
                //System.out.print(" FAIL\n");
                return false;
            }

            lPos++;
        }

        //System.out.print(" PASS\n");
        return true;
    }

    public static boolean areStacksEqual(ItemStack left, ItemStack right)
    {
        return ItemStack.areItemsEqual(left, right) && left.getCount() == right.getCount();
    }

    private static void dumpStacks(List<ItemStack> stacks, String side)
    {
        int i = 0;

        //System.out.printf("DUMP [%s] -->\n", side);
        for (ItemStack stack : stacks)
        {
            //System.out.printf("%s[%d] // [%s]\n", side, i, stack.toString());
            i++;
        }
        //System.out.printf("DUMP END [%s]\n", side);
    }

    private static void dumpIngs(List<Ingredient> ings, String side)
    {
        int i = 0;

        //System.out.printf("DUMP [%s] -->\n", side);
        for (Ingredient ing : ings)
        {
            //List<RegistryEntry<Item>> items = ing.getMatchingItems().toList();
            List<RegistryEntry<Item>> items = ((IMixinIngredient) (Object) ing).itemscroller_getEntries().stream().toList();

            //System.out.printf("%s[%d] //", side, i);

            for (RegistryEntry<Item> item : items)
            {
                //System.out.printf(" [%s]", item.getIdAsString());
            }

            //System.out.print("// []\n");
            i++;
        }

        //System.out.printf("DUMP END [%s]\n", side);
    }
}
