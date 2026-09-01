package com.gabriel.mobpacified;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, mobpacified.MOD_ID);

    public static final RegistryObject<CreativeModeTab> UMA_TAB = CREATIVE_TABS.register("uma_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mobpacified")) // Atualizado o nome do translation key
                    .icon(() -> new ItemStack(ModItems.MYSTIC_AMULET.get())) // Forma padrão de passar o item como ícone
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MYSTIC_AMULET.get());
                    })
                    .build());
}