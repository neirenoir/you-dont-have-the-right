# You Don't Have The Right (Recipe)
An overengineered mod to fix the ["working as intended"](https://bugs.mojang.com/browse/MC-143214) 
`doLimitedCrafting` gamerule. Among other things.

# Features
* Furnaces and brewing stands obey `doLimitedCrafting` according to the recipe book of
the last player who changed their contents.
* Data driven potion recipes! This was not outside of the scope of the mod at all.
* Completely rewritten tick logic for furnaces and brewing stands! It should increase
performance (untested) and amount of bugs (untested, too).
* Force `doLimitedCrafting` to `true` on all dimensions, regardless of the underlying 
value of the gamerule (configurable).
* Prevent Steve from receiving "divine inspiration" recipes every time he picks up a new item 
or touches water (configurable).
* And an amazing API for developers that no one but me will ever use!

# Known uncompatibilities
* **[NERB (Not Enough Recipe Book)](https://modrinth.com/mod/nerb)**, as it disables the
recipe book entirely, which is the underlying mechanism of this entire mod.
* Any mod that modifies the tick logic of furnaces or brewing stands.
* Any mod that adds potions, as these will not work since they have no generated recipes.
Ping me to get your mod's potion recipes generated and included!

# Best used with...
* **[Better Recipe Book](https://modrinth.com/mod/brb/versions)**, with "hide locked recipes"
option enabled
* **[CraftTweaker](https://modrinth.com/mod/crafttweaker)** or **[KubeJS](https://modrinth.com/mod/kubejs)**
to actually leverage the features of this mod

# License
This mod is licensed under the MIT license.

# Modpacks
Feel free to use this mod in any modpack, where it best shines! If you could let me know that you will use 
this mod in your modpack, it would make me very happy.