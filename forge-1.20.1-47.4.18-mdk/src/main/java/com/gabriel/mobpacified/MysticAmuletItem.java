package com.gabriel.mobpacified;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

// O comportamento do amuleto fica no handler de PlayerInteractEvent.EntityInteract
// (em mobpacified.java), porque o evento roda antes do interact do mob.
// Esta classe existe só para a tooltip.
public class MysticAmuletItem extends Item {

    public MysticAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mobpacified.mystic_amulet.tooltip.pacify")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mobpacified.mystic_amulet.tooltip.follow")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.mobpacified.mystic_amulet.tooltip.consumed")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
