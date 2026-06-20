package com.example.examplemod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod("mobpacified")
public class ExampleMod {
    public static final String MOD_ID = "mobpacified";

    public ExampleMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack itemstack = event.getItemStack();
        if (itemstack.is(Items.NAME_TAG) && event.getTarget() instanceof Mob mob) {
            if (itemstack.hasCustomHoverName() && itemstack.getHoverName().getString().equalsIgnoreCase("Amigao")) {
                resetWarden(mob);
            }
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Mob agressor && isAmigao(agressor)) {
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Mob vitima && isAmigao(vitima)) {
            event.setCanceled(true);

            Entity agressor = event.getSource().getEntity();

            if (agressor instanceof net.minecraft.world.entity.player.Player player) {
                if (vitima instanceof Warden warden) resetWarden(warden);
                return;
            }

            if (agressor instanceof LivingEntity inimigo) {
                AABB area = vitima.getBoundingBox().inflate(35.0D);
                List<Mob> aliados = vitima.level().getEntitiesOfClass(Mob.class, area);

                for (Mob aliado : aliados) {
                    // VINGANÇA RESTRITA: Ignora Warden, Creeper e agora também o Esqueleto
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

    @SubscribeEvent
    public void onLivingTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isAmigao(mob)) {
            if (mob instanceof Warden) return;

            LivingEntity alvo = event.getNewTarget();
            if (alvo instanceof net.minecraft.world.entity.player.Player) {
                event.setNewTarget(null);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob mob && isAmigao(mob)) {
            // Warden
            if (mob instanceof Warden warden) {
                if (warden.getTarget() instanceof net.minecraft.world.entity.player.Player ||
                        warden.getTarget() == null || !warden.getTarget().isAlive()) {
                    resetWarden(warden);
                }
                warden.level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                        warden.getBoundingBox().inflate(20.0D)).forEach(p -> {
                    if (p.hasEffect(MobEffects.DARKNESS)) p.removeEffect(MobEffects.DARKNESS);
                });
            }

            // Creeper
            if (mob instanceof Creeper creeper) {
                if (creeper.getSwellDir() > 0) creeper.setSwellDir(-1);
                if (creeper.getTarget() != null) creeper.setTarget(null);
            }

            // Skeleton: Impede ele de focar em qualquer um (evita briga por flecha errada)
            if (mob instanceof Skeleton skeleton) {
                if (skeleton.getTarget() != null) {
                    skeleton.setTarget(null);
                }
            }

            // Slime
            if (mob instanceof Slime slime) {
                if (slime.getTarget() instanceof net.minecraft.world.entity.player.Player) {
                    slime.setTarget(null);
                    slime.getNavigation().stop();
                }
            }
        }
    }

    @SubscribeEvent
    public void onExplosionStart(ExplosionEvent.Start event) {
        if (event.getExplosion().getExploder() instanceof Creeper creeper && isAmigao(creeper)) {
            event.setCanceled(true);
        }
    }

    private void resetWarden(Mob mob) {
        mob.setTarget(null);
        if (mob instanceof Warden warden) {
            warden.clearAnger(null);
            warden.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        }
    }

    private boolean isAmigao(Mob mob) {
        return mob.hasCustomName() && mob.getCustomName().getString().equalsIgnoreCase("Amigao");
    }
}