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
        /*
        if (left.size() != right.size())
        {
            //System.out.printf("compare() --> size left [%d] != right [%d]\n", left.size(), right.size());
            return false;
        }
         */

        //System.out.printf("compare() --> size left [%d], right [%d]\n", left.size(), right.size());
        //dumpStacks(left, "LF");
        //dumpIngs(right, "RT");

        for (int i = 0; i < left.size(); i++)
        {
            ItemStack l = left.get(i);

            if (i >= right.size())
            {
                if (count > i)
                {
                    //System.out.print(" count not valid (ran out of right entries!)\n");
                    return false;
                }
                else if (l.isEmpty() && i <= count)
                {
                    //System.out.print(" PASS (end of right side)\n");
                    return true;
                }

                //System.out.print(" not valid (ran out of right entries!)\n");
                return false;
            }
            Ingredient ri = right.get(i);
            RegistryEntry<Item> r = ri.getMatchingItems().getFirst();

            //System.out.printf("compare() [%d] left [%s] / right [%s]\n", i, l.toString(), r.getIdAsString());

            if (!ri.test(l))
            {
                //System.out.print(" not valid (Test test)\n");
                return false;
            }
            else if (!ItemStack.areItemsEqual(l, new ItemStack(r)))
            {
                //System.out.print(" not valid (Stack test)\n");
                return false;
            }

            /*
            else if (l.getCount() != r.getCount())
            {
                //System.out.print(" count not equal\n");
                return false;
            }
             */
        }

        //System.out.print(" PASS\n");
        return true;
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
            //System.out.printf("%s[%d] // [%s]\n", side, i, ing.getMatchingItems().getFirst().getIdAsString());
            i++;
        }
        //System.out.printf("DUMP END [%s]\n", side);
    }
}
