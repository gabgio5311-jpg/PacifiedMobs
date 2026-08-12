package com.example.mobpacified;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, mobpacified.MOD_ID);

    public static final RegistryObject<Item> MYSTIC_AMULET = ITEMS.register("mystic_amulet",
            () -> new MysticAmuletItem(new Item.Properties()));

}
