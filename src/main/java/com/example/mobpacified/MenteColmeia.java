package com.example.mobpacified;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

// Mente colmeia: todos os mobs pacificados perto de uma briga atacam o mesmo alvo.
// O alvo é quem:
//   - bateu num mob pacificado;
//   - apanhou de um jogador;
//   - bateu num jogador.
// Enquanto tem alvo, o mob pacificado pode mirar e causar dano, mas só nele.
@EventBusSubscriber(modid = mobpacified.MOD_ID)
public class MenteColmeia {

    private static final String TAG_ALVO = "mobpacified_hive_target";
    private static final String TAG_ATE = "mobpacified_hive_until";

    // Raio em volta da briga em que os pacificados são convocados
    private static final double RAIO_CONVOCACAO = 32.0D;
    // Se o alvo ficar mais longe que isso do mob, ele desiste
    private static final double DISTANCIA_MAXIMA = 48.0D;
    // Sem novos golpes, a convocação acaba depois de 30 s
    private static final long DURACAO_TICKS = 20L * 30L;

    // NEOFORGE 1.21.1: LivingIncomingDamageEvent (no Forge 1.20.1 era LivingAttackEvent).
    // Ele roda depois do isInvulnerableTo: jogador no criativo não chega a disparar,
    // então apanhar no criativo não convoca ninguém. Bater, convoca.
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity vitima = event.getEntity();
        if (vitima.level().isClientSide) return;

        // getEntity() é quem causou o dano; para flecha, é quem atirou
        if (!(event.getSource().getEntity() instanceof LivingEntity agressor)) return;
        if (agressor == vitima) return;
        // Golpe da própria colmeia não convoca ninguém
        if (agressor instanceof Mob mobAgressor && mobpacified.isPacified(mobAgressor)) return;

        if (vitima instanceof Mob mobVitima && mobpacified.isPacified(mobVitima)) {
            // Alguém bateu num pacificado
            convocar(agressor, vitima);
        } else if (agressor instanceof Player) {
            // O jogador bateu em alguém
            convocar(vitima, agressor);
        } else if (vitima instanceof Player) {
            // Alguém bateu no jogador
            convocar(agressor, vitima);
        }
    }

    // Manda os pacificados em volta de "centro" atacarem "alvo".
    private static void convocar(LivingEntity alvo, LivingEntity centro) {
        if (!podeSerAlvo(alvo)) return;

        long ate = alvo.level().getGameTime() + DURACAO_TICKS;

        alvo.level().getEntitiesOfClass(Mob.class,
                centro.getBoundingBox().inflate(RAIO_CONVOCACAO),
                mob -> mob != alvo && mobpacified.isPacified(mob) && podeParticipar(mob)
        ).forEach(mob -> {
            boolean novo = !ehAlvoDaColmeia(mob, alvo);

            CompoundTag data = mob.getPersistentData();
            data.putUUID(TAG_ALVO, alvo.getUUID());
            data.putLong(TAG_ATE, ate);
            atacar(mob, alvo);

            // Partícula de raiva só quando o mob entra na briga, não a cada golpe
            if (novo && mob.level() instanceof ServerLevel nivel) {
                nivel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                        5, 0.4D, 0.3D, 0.4D, 0.0D);
            }
        });
    }

    // A colmeia só mira em mobs: jogadores e outros pacificados ficam de fora.
    private static boolean podeSerAlvo(LivingEntity alvo) {
        if (!alvo.isAlive() || alvo instanceof Player) return false;
        return !(alvo instanceof Mob mob && mobpacified.isPacified(mob));
    }

    // Creeper fica de fora: atacar para ele é explodir.
    private static boolean podeParticipar(Mob mob) {
        return !(mob instanceof Creeper);
    }

    // Usado pelo onLivingIncomingDamage/onTargetChange: este alvo é o da colmeia para este mob?
    static boolean ehAlvoDaColmeia(Mob mob, Entity alvo) {
        CompoundTag data = mob.getPersistentData();
        return alvo != null && data.hasUUID(TAG_ALVO) && data.getUUID(TAG_ALVO).equals(alvo.getUUID());
    }

    // Devolve o alvo atual, ou null se não tem (e limpa a convocação vencida).
    static LivingEntity alvoAtual(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(TAG_ALVO)) return null;

        LivingEntity alvo = null;
        if (mob.level() instanceof ServerLevel nivel
                && nivel.getEntity(data.getUUID(TAG_ALVO)) instanceof LivingEntity vivo) {
            alvo = vivo;
        }

        boolean valido = alvo != null
                && podeSerAlvo(alvo)
                && mob.level().getGameTime() <= data.getLong(TAG_ATE)
                && mob.distanceTo(alvo) <= DISTANCIA_MAXIMA;

        if (!valido) {
            data.remove(TAG_ALVO);
            data.remove(TAG_ATE);
            return null;
        }
        return alvo;
    }

    // Roda a cada tick enquanto há alvo: a IA do mob tende a largar o alvo sozinha.
    static void atacar(Mob mob, LivingEntity alvo) {
        if (mob.getTarget() != alvo) {
            mob.setTarget(alvo);
        }
        mob.setAggressive(true);

        // Mobs com "cérebro" (piglin, hoglin, warden...) leem o alvo da memória.
        // getMemory lança erro em mob sem essa memória (ravager, zumbi...), por isso o checkMemory.
        if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)
                && mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != alvo) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, alvo);
        }

        // Abelha, lobo etc. só atacam se estiverem com raiva
        if (mob instanceof NeutralMob neutro && !alvo.getUUID().equals(neutro.getPersistentAngerTarget())) {
            neutro.setPersistentAngerTarget(alvo.getUUID());
            neutro.startPersistentAngerTimer();
        }

        // As cabeças laterais do Wither escolhem alvo aleatório quando estão sem
        // nenhum. Apontando as duas pro alvo, as três cabeças atiram nele.
        if (mob instanceof WitherBoss wither) {
            wither.setAlternativeTarget(1, alvo.getId());
            wither.setAlternativeTarget(2, alvo.getId());
        }

        // O Warden larga o alvo se não estiver irritado com ele, troca de alvo se
        // ficar mais irritado com outro (o jogador, pelas vibrações) e fica parado
        // rugindo antes de atacar. Então: raiva só no alvo, sem rugido.
        if (mob instanceof Warden warden) {
            mobpacified.acalmarWarden(warden, alvo);
            if (!warden.getAngerLevel().isAngry()) {
                warden.increaseAngerAt(alvo, 80, false);
            }
        }
    }
}
