package net.chyme.perplayerdifficulty.mixins;

import net.chyme.perplayerdifficulty.PlayerDifficultyManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.entity.mob.MobEntity.canMobSpawn;

@Mixin(HostileEntity.class)
public class MobMixin {

    @Inject(method = "canSpawnInDark", at = @At("HEAD"), cancellable = true)
    private static void personalDifficultyCanSpawnInDark(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 128.0, false);
        ServerPlayerEntity nearestPlayer = (player instanceof ServerPlayerEntity sp) ? sp : null;

        Difficulty difficulty = nearestPlayer != null
                ? PlayerDifficultyManager.getPlayerDifficulty(nearestPlayer)
                : world.getDifficulty(); // fallback

        boolean result = difficulty != Difficulty.PEACEFUL
                && (SpawnReason.isTrialSpawner(spawnReason) || HostileEntity.isSpawnDark(world, pos, random))
                && canMobSpawn(type, world, spawnReason, pos, random);

        cir.setReturnValue(result);
    }

    @Inject(method = "canSpawnIgnoreLightLevel", at = @At("HEAD"), cancellable = true)
    private static void personalDifficultyCanSpawnIgnoreLightLevel(EntityType<? extends HostileEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 128.0, false);
        ServerPlayerEntity nearestPlayer = (player instanceof ServerPlayerEntity sp) ? sp : null;

        Difficulty difficulty = nearestPlayer != null
                ? PlayerDifficultyManager.getPlayerDifficulty(nearestPlayer)
                : world.getDifficulty(); // fallback

        boolean result = difficulty != Difficulty.PEACEFUL
                && canMobSpawn(type, world, spawnReason, pos, random);

        cir.setReturnValue(result);
    }
}

@Mixin(MobEntity.class)
class MobEntityMixin {

    @Redirect(
            method = "initEquipment",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;getDifficulty()Lnet/minecraft/world/Difficulty;"
            )
    )
    private Difficulty useNearestPlayerDifficulty(World world) {
        PlayerEntity player = world.getClosestPlayer((MobEntity) (Object) this, 128.0);
        ServerPlayerEntity nearestPlayer = (player instanceof ServerPlayerEntity sp) ? sp : null;

        return nearestPlayer != null
                ? PlayerDifficultyManager.getPlayerDifficulty(nearestPlayer)
                : world.getDifficulty(); // fallback
    }
}

@Mixin(LivingEntity.class)
class LivingEntityTargetMixin {
    @Redirect(method = "canTarget(Lnet/minecraft/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getDifficulty()Lnet/minecraft/world/Difficulty;"))
    private Difficulty redirectDifficultyForCanTarget(World world, LivingEntity target) {
        // If target is a player, use their personal difficulty
        if (target instanceof ServerPlayerEntity serverPlayer) {
            return PlayerDifficultyManager.getPlayerDifficulty(serverPlayer);
        }
        return world.getDifficulty();
    }
}

// Additional mixin for phantom spawning specifically
@Mixin(PhantomSpawner.class)
class PhantomSpawnerMixin {

    @Redirect(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;isSpectator()Z"))
    private boolean redirectIsSpectator(ServerPlayerEntity player) {
        // First check if the original spectator check would fail
        if (player.isSpectator()) {
            return true; // Still a spectator, skip this player
        }

        // Additional check: if player is on peaceful, treat them as if they're a spectator for phantom spawning
        Difficulty playerDifficulty = PlayerDifficultyManager.getPlayerDifficulty(player);
        return playerDifficulty == Difficulty.PEACEFUL; // Skip phantom spawning for this player
    }
}

