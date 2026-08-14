package com.example.mobpacified;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // NeoForge 1.21.1: DeferredRegister.createItems em vez de DeferredRegister.create(ForgeRegistries.ITEMS, ...)
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(mobpacified.MOD_ID);

    public static final DeferredItem<Item> MYSTIC_AMULET = ITEMS.register("mystic_amulet",
            () -> new MysticAmuletItem(new Item.Properties()));

}
