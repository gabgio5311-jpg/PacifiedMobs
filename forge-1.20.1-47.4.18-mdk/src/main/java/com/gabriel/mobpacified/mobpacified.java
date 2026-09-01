package com.gabriel.mobpacified;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.UUID;

// IMPORTS CORRIGIDOS PARA O FORGE 1.20.1
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@Mod(mobpacified.MOD_ID)
@Mod.EventBusSubscriber(modid = mobpacified.MOD_ID)
public class mobpacified {

    public static final String MOD_ID = "mobpacified";

    // Chaves usadas no NBT persistente do mob (sobrevivem ao salvar/carregar o mundo)
    private static final String TAG_PACIFIED = "mobpacified_pacified";
    private static final String TAG_FOLLOWING = "mobpacified_following";
    private static final String TAG_OWNER = "mobpacified_owner";

    // Sem este construtor os DeferredRegister nunca entram no mod event bus
    // e o item/aba criativa não chegam a existir no jogo.
    // O contexto é injetado pelo FML (FMLJavaModLoadingContext.get() está depreciado).
    public mobpacified(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
    }

    // Um mob é controlado por este mod quando foi marcado pelo Amuleto Místico.
    private static boolean isPacified(Mob mob) {
        return mob.getPersistentData().getBoolean(TAG_PACIFIED);
    }

    // Só faz sentido gastar um amuleto em quem pode ser hostil:
    // Enemy cobre os mobs hostis, NeutralMob cobre lobo, golem de ferro, abelha, enderman, etc.
    private static boolean podeSerPacificado(Mob mob) {
        return mob instanceof Enemy || mob instanceof NeutralMob;
    }

    // Botão direito com o Amuleto Místico pacifica o mob (gasta 1 amuleto).
    // Shift + botão direito num mob já pacificado alterna o modo "seguir o jogador".
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        // Só reage a uma das mãos para não alternar/gastar duas vezes no mesmo clique
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        if (!(event.getTarget() instanceof Mob mob)) return;

        if (player.isShiftKeyDown()) {
            if (isPacified(mob)) {
                alternarSeguir(player, mob);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!stack.is(ModItems.MYSTIC_AMULET.get())) return;

        String nome = mob.getName().getString();

        if (isPacified(mob)) {
            player.sendSystemMessage(Component.literal(nome + " já está pacificado."));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        if (!podeSerPacificado(mob)) {
            player.sendSystemMessage(Component.literal(nome + " não é um mob hostil."));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        pacificar(mob);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        player.sendSystemMessage(Component.literal(nome + " foi pacificado!"));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void pacificar(Mob mob) {
        mob.getPersistentData().putBoolean(TAG_PACIFIED, true);

        // O amuleto foi gasto neste mob: impede que ele suma por despawn natural
        mob.setPersistenceRequired();

        limparAgressividade(mob);

        if (mob.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.HEART,
                    mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                    8, 0.4D, 0.3D, 0.4D, 0.05D);
            nivel.playSound(null, mob.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    // Tira toda a agressividade do mob. Roda a cada tick (o mob volta a mirar sozinho)
    // e também no momento em que o amuleto é usado.
    private static void limparAgressividade(Mob mob) {
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

        if (mob instanceof NeutralMob neutro) {
            neutro.stopBeingAngry();
        }

        if (mob instanceof Warden warden) {
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_SOUND_DELAY);
            warden.getBrain().eraseMemory(MemoryModuleType.IS_PANICKING);

            // clearAnger(null) não limpava nada: a ira do Warden é guardada por
            // suspeito, então é preciso zerar entidade por entidade.
            warden.level().getEntitiesOfClass(LivingEntity.class,
                    warden.getBoundingBox().inflate(24.0D)).forEach(alvo -> {
                warden.clearAnger(alvo);
                if (alvo instanceof Player p && p.hasEffect(MobEffects.DARKNESS)) {
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

    private static void alternarSeguir(Player player, Mob mob) {
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
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isPacified(mob)) {
            event.setNewTarget(null);
        }

        if (event.getOriginalTarget() instanceof Mob vitima && isPacified(vitima)) {
            event.setNewTarget(null);
        }
    }

    // Mob pacificado nao causa dano em ninguem. Ele continua levando dano
    // normalmente, como qualquer outro mob.
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        Entity agressor = event.getSource().getEntity();

        if (agressor instanceof Mob mobAgressor && isPacified(mobAgressor)) {
            event.setCanceled(true);
        }
    }

    // CORRIGIDO: Usando LivingTickEvent em vez de EntityTickEvent
    @SubscribeEvent
    public static void onLivingTick(LivingTickEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity instanceof Mob mob && isPacified(mob)) {

            limparAgressividade(mob);

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
                        mob.getNavigation().stop();
                    } else {
                        mob.getNavigation().stop();
                    }
                }
            }
        }
    }
}