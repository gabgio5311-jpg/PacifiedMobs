package com.example.mobpacified;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, mobpacified.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UMA_TAB =
            CREATIVE_TABS.register("uma_tab",
                    () -> CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.mobpacified"))
                            .icon(() -> new ItemStack(ModItems.MYSTIC_AMULET.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.MYSTIC_AMULET.get());
                            })
                            .build());
}
