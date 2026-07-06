package com.example.mobpacified;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = "mobpacified")
public class mobpacified {

    // Chaves usadas no NBT persistente do mob (sobrevivem ao salvar/carregar o mundo)
    private static final String TAG_FOLLOWING = "mobpacified_following";
    private static final String TAG_OWNER = "mobpacified_owner";

    private static boolean isAmigao(Mob mob) {
        // Verifica se tem nome customizado
        if (mob.hasCustomName()) {
            // Pega o nome em formato de texto simples e converte para minúsculo
            String nome = mob.getCustomName().getString().toLowerCase();

            // Se o nome CONTÉM a palavra "amigao" (ignora aspas, espaços, etc)
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
            // Se o mob for um Amigao e tentar focar em algo, cancelamos o alvo
            event.setNewAboutToBeSetTarget(null);
        }

        // Se algo tentar focar no Amigao, bloqueamos
        if (event.getOriginalAboutToBeSetTarget() instanceof Mob vitima && isAmigao(vitima)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    // As cabeças laterais do Wither miram alvos aleatórios sozinhas e atiram no
    // mesmo tick, antes do nosso .Post limpar os alvos. Então, em vez de brigar
    // com a mira, cancelamos o próprio crânio ao nascer: se o dono é um amigão,
    // o projétil nunca entra no mundo (não voa nem explode).
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof WitherSkull skull
                && skull.getOwner() instanceof Mob dono && isAmigao(dono)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Entity agressor = event.getSource().getEntity();

        if (agressor instanceof Mob mobAgressor && isAmigao(mobAgressor)) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof Mob vitima && isAmigao(vitima)) {
            event.setCanceled(true);
        }
    }

    // CORREÇÃO: Mudamos de .Pre para .Post para rodar DEPOIS da IA teimosa do mod
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob mob && isAmigao(mob)) {

            // Se mesmo assim o mob tentou focar em alguém no turno dele, a gente zera e trava a perna dele
            if (mob.getTarget() != null) {
                mob.setTarget(null);
                mob.getNavigation().stop(); // Força o mob a parar de andar na direção do alvo
            }

            mob.setAggressive(false);
            mob.setLastHurtByMob(null);

            // Limpa as memórias genéricas de ataque
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
            
            // Wither: as cabeças laterais miram sozinhas (alvos alternativos),
            // então zerar getTarget() não basta. Limpamos os dois alvos das cabeças
            // (id 0 = sem alvo) pra ele parar de atirar crânios nos mobs.
            if (mob instanceof WitherBoss wither) {
                wither.setAlternativeTarget(1, 0);
                wither.setAlternativeTarget(2, 0);
                // O som do disparo é um level event que só toca se o mob não for
                // silencioso. Silenciando o Wither, some o barulho de atirar
                // (e os demais sons dele) — o crânio já é cancelado ao nascer.
                if (!wither.isSilent()) {
                    wither.setSilent(true);
                }
            }

            // Wither: as cabeças laterais miram sozinhas (alvos alternativos),
            // então zerar getTarget() não basta. Limpamos os dois alvos das cabeças
            // (id 0 = sem alvo) pra ele parar de atirar crânios nos mobs.
            if (mob instanceof WitherBoss wither) {
                wither.setAlternativeTarget(1, 0);
                wither.setAlternativeTarget(2, 0);
                // O som do disparo é um level event que só toca se o mob não for
                // silencioso. Silenciando o Wither, some o barulho de atirar
                // (e os demais sons dele) — o crânio já é cancelado ao nascer.
                if (!wither.isSilent()) {
                    wither.setSilent(true);
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
                        mob.getNavigation().moveTo(owner, 1.2);
                    } else if (dist >= 40.0) {
                        // Teleporta se ficou muito longe (ex.: dono atravessou portal ou correu)
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
