[![](https://jitpack.io/v/sakura-ryoko/itemscroller.svg)](https://jitpack.io/#sakura-ryoko/itemscroller)

Item Scroller
==============
Item Scroller is a Minecraft mod that adds various convenience features for moving items
inside inventory GUIs. Examples are scrolling the mouse wheel over slots with items in them
or Shift/Ctrl + click + dragging over slots to move items from them in various ways etc.

Item scrolling is basically what the old NEI mod did and Mouse Tweaks also does.
This mod has some different drag features compared to Mouse Tweaks, and also some special
villager trading related helper features as well as crafting helper features.

For more information and downloads of the already compiled builds,
see https://www.curseforge.com/minecraft/mc-mods/item-scroller

Processing GUI Support (More Work Blocks)
=========================================
This fork adds recipe storing and mass crafting support for the following
processing GUIs, in addition to the vanilla crafting table:

* Stonecutter (切石机)
* Anvil (铁砧) - including item renaming
* Grindstone (砂轮)
* Loom (织布机)
* Smithing Table (锻造台)
* Enchantment Table (附魔台) - including enchantment level option selection

Each GUI has its own feature toggle in the config (default off):
`enableStonecutterFeatures`, `enableAnvilFeatures`, `enableGrindstoneFeatures`,
`enableLoomFeatures`, `enableSmithingFeatures`, `enableEnchantmentFeatures`.

Usage is the same as the crafting table:
* Hold the Recipe key (default A) to open the recipe view
* Pick-block (middle click) over the output slot to store the current recipe
* For the Enchantment Table, pick-block over an enchantment button to store
  the recipe with that level option
* Hold the craftEverything or massCraft key to mass craft

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/
