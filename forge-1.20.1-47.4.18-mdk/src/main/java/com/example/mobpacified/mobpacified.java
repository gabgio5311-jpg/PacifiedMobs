package com.example.mobpacified;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;

// IMPORTS CORRIGIDOS PARA O FORGE 1.20.1
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;

@Mod("mobpacified")
@Mod.EventBusSubscriber(modid = "mobpacified")
public class mobpacified {

    private static boolean isAmigao(Mob mob) {
        if (mob.hasCustomName()) {
            String nome = mob.getCustomName().getString().toLowerCase();
            if (nome.contains("amigao")) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isAmigao(mob)) {
            event.setNewTarget(null);
        }

        if (event.getOriginalTarget() instanceof Mob vitima && isAmigao(vitima)) {
            event.setNewTarget(null);
        }
    }

    // CORRIGIDO: Usando LivingAttackEvent em vez de LivingIncomingDamageEvent
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity agressor = event.getSource().getEntity();

        if (agressor instanceof Mob mobAgressor && isAmigao(mobAgressor)) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof Mob vitima && isAmigao(vitima)) {
            event.setCanceled(true);
        }
    }

    // CORRIGIDO: Usando LivingTickEvent em vez de EntityTickEvent
    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob mob && isAmigao(mob)) {

            if (mob.getTarget() != null) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            mob.setAggressive(false);
            mob.setLastHurtByMob(null);

            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            }

            if (mob instanceof Warden warden) {
                warden.clearAnger(null);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_SOUND_DELAY);
                warden.getBrain().eraseMemory(MemoryModuleType.IS_PANICKING);

                warden.level().getEntitiesOfClass(Player.class,
                        warden.getBoundingBox().inflate(20.0D)).forEach(p -> {
                    if (p.hasEffect(MobEffects.DARKNESS)) {
                        p.removeEffect(MobEffects.DARKNESS);
                    }
                });
            }

            if (mob instanceof Creeper creeper) {
                if (creeper.getSwellDir() > 0) {
                    creeper.setSwellDir(-1);
                }
            }
        }
    }
}