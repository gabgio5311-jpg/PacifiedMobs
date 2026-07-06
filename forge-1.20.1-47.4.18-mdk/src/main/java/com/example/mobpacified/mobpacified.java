package com.example.mobpacified;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

// IMPORTS CORRIGIDOS PARA O FORGE 1.20.1
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mod("mobpacified")
@Mod.EventBusSubscriber(modid = "mobpacified")
public class mobpacified {

    // Chaves usadas no NBT persistente do mob (sobrevivem ao salvar/carregar o mundo)
    private static final String TAG_FOLLOWING = "mobpacified_following";
    private static final String TAG_OWNER = "mobpacified_owner";

    private static boolean isAmigao(Mob mob) {
        if (mob.hasCustomName()) {
            String nome = mob.getCustomName().getString().toLowerCase();
            if (nome.contains("amigao")) {
                return true;
            }
        }
        return false;
    }

    // Shift + botão direito num amigão alterna o modo "seguir o jogador"
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        // Só reage a uma das mãos para não alternar duas vezes no mesmo clique
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) return;

        if (event.getTarget() instanceof Mob mob && isAmigao(mob)) {
            CompoundTag data = mob.getPersistentData();
            boolean seguindo = !data.getBoolean(TAG_FOLLOWING);
            data.putBoolean(TAG_FOLLOWING, seguindo);

            String nome = mob.getName().getString();
            if (seguindo) {
                data.putUUID(TAG_OWNER, player.getUUID());
                player.sendSystemMessage(Component.literal(nome + " está te seguindo!"));
            } else {
                data.remove(TAG_OWNER);
                mob.getNavigation().stop();
                player.sendSystemMessage(Component.literal(nome + " parou de te seguir."));
            }

            // Impede que o clique acione outra interação (montar, tosquiar, etc.)
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
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

            // Modo "seguir": anda até o dono enquanto o estado estiver ativo
            CompoundTag data = mob.getPersistentData();
            if (data.getBoolean(TAG_FOLLOWING) && data.hasUUID(TAG_OWNER)) {
                UUID ownerId = data.getUUID(TAG_OWNER);
                Player owner = mob.level().getPlayerByUUID(ownerId);

                if (owner != null && owner.isAlive()) {
                    double dist = mob.distanceTo(owner);
                    if (dist > 3.0 && dist < 40.0) {
                        // Teleporta se ficou muito longe (ex.: dono atravessou portal ou correu)
                        mob.getNavigation().moveTo(owner, 1.2);
                    } else if (dist >= 40.0) {
                        mob.moveTo(owner.getX(), owner.getY(), owner.getZ(),
                                mob.getYRot(), mob.getXRot());
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }
        }
    }
}