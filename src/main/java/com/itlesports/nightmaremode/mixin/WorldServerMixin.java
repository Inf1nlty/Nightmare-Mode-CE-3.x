package com.itlesports.nightmaremode.mixin;

import btw.community.nightmaremode.NightmareMode;
import com.itlesports.nightmaremode.NMUtils;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin {

    @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/SpawnerAnimals;findChunksForSpawning(Lnet/minecraft/src/WorldServer;ZZZ)I"), index = 3)
    public boolean allowSpawnAnimal(boolean spawnAnimal) {
        boolean isEclipse = NightmareMode.isEclipse;
        return ( NMUtils.isVoidWorldLoaded()) || isEclipse || spawnAnimal;
    }
}