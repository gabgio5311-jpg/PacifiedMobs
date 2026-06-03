package com.example.examplemod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

@EventBusSubscriber(modid = "mobpacified")
public class ExampleMod {

    // 1. Método que verifica se o mob tem a Name Tag "Amigao"
    private boolean isAmigao(Mob mob) {
        return mob.hasCustomName() && mob.getCustomName().getString().equalsIgnoreCase("Amigao");
    }

    // 2. Método que limpa completamente a raiva e a memória do Warden
    private void resetWarden(Mob mob) {
        mob.setTarget(null);
        if (mob instanceof Warden warden) {
            warden.clearAnger(null);
            warden.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_SOUND_DELAY);
        }
    }

    // 3. Evento de Dano (Roda antes do dano ser aplicado)
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof Mob vitima && isAmigao(vitima)) {
            Entity agressor = event.getSource().getEntity();

            // Se o agressor for vivo e NÃO for um Player, pede ajuda aos aliados
            if (agressor instanceof LivingEntity inimigo && !(inimigo instanceof Player)) {
                AABB area = vitima.getBoundingBox().inflate(35.0D);
                List<Mob> aliados = vitima.level().getEntitiesOfClass(Mob.class, area);

                for (Mob aliado : aliados) {
                    if (isAmigao(aliado) &&
                            !(aliado instanceof Warden) &&
                            !(aliado instanceof Creeper) &&
                            !(aliado instanceof Skeleton)) {
                        aliado.setTarget(inimigo);
                    }
                }
            }
        }
    }

    // 4. Evento de Tick (Atualizado para o NeoForge 1.21.1)
    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob mob && isAmigao(mob)) {

            // Controle do Warden
            if (mob instanceof Warden warden) {
                if (warden.getTarget() instanceof Player ||
                        warden.getTarget() == null || !warden.getTarget().isAlive()) {
                    resetWarden(warden);
                }
                // Remove o efeito de escuridão dos jogadores próximos
                warden.level().getEntitiesOfClass(Player.class,
                        warden.getBoundingBox().inflate(20.0D)).forEach(p -> {
                    if (p.hasEffect(MobEffects.DARKNESS)) p.removeEffect(MobEffects.DARKNESS);
                });
            }

            // Controle do Creeper
            if (mob instanceof Creeper creeper) {
                if (creeper.getSwellDir() > 0) creeper.setSwellDir(-1);
                if (creeper.getTarget() instanceof Player) creeper.setTarget(null);
            }

            // Controle do Skeleton
            if (mob instanceof Skeleton skeleton) {
                if (skeleton.getTarget() instanceof Player) {
                    skeleton.setTarget(null);
                }
            }

            // Controle do Slime
            if (mob instanceof Slime slime) {
                if (slime.getTarget() instanceof Player) {
                    slime.setTarget(null);
                    slime.getNavigation().stop();
                }
            }
        }
    }
}