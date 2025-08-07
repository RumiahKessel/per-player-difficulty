package net.chyme.perplayerdifficulty.mixins;

import net.chyme.perplayerdifficulty.PlayerDifficultyManager;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HungerManager.class)
public class HungerAndDamageMixin {

    @ModifyVariable(
            method = "update(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At("STORE"),
            ordinal = 0
    )
    public Difficulty modifyDifficulty(Difficulty original, ServerPlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Return the player's personal difficulty instead of the world difficulty
            return PlayerDifficultyManager.getPlayerDifficulty(serverPlayer);
        }
        // Return original difficulty if not a server player
        return original;
    }
}

@Mixin(PlayerEntity.class)
class PlayerDamageMixin {

    @Redirect(
            method = "damage", // Method name in the target class
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getDifficulty()Lnet/minecraft/world/Difficulty;"
            )
    )
    public Difficulty overrideDifficulty(ServerWorld world) {
        // Return the player's personal difficulty instead of the world difficulty
        return PlayerDifficultyManager.getPlayerDifficulty((ServerPlayerEntity) (Object) this);
    }
}

