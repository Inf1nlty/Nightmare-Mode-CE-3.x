package com.itlesports.nightmaremode.mixin;

import btw.community.nightmaremode.NightmareMode;
import btw.world.util.difficulty.Difficulties;
import com.itlesports.nightmaremode.NightmareUtils;
import com.itlesports.nightmaremode.item.NMItems;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityGhast.class)
public abstract class EntityGhastMixin extends EntityFlying{
    @Shadow public abstract boolean getCanSpawnHereNoPlayerDistanceRestrictions();

    @Shadow private Entity entityTargeted;
    @Unique int rageTimer = 0;
    @Unique boolean firstAttack = true;
    @Unique boolean isCreeperVariant = false;

    public EntityGhastMixin(World world) {
        super(world);
    }

    @Inject(method = "applyEntityAttributes", at = @At("TAIL"))
    private void applyAdditionalAttributes(CallbackInfo ci){
        int progress = NightmareUtils.getWorldProgress(this.worldObj);
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setAttribute((20 + 8 * progress) * NightmareUtils.getNiteMultiplier());
        // 20 -> 28 -> 36 -> 44
    }

    @ModifyArg(method = "updateAttackStateClient", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/World;getClosestVulnerablePlayerToEntity(Lnet/minecraft/src/Entity;D)Lnet/minecraft/src/EntityPlayer;"), index = 1)
    private double increaseHordeAttackRange(double par2){
        return (NightmareMode.hordeMode || (NightmareUtils.getIsBloodMoon() && this.dimension == -1)) ? 140d : par2;
    }
    @ModifyConstant(method = "updateAttackStateClient", constant = @Constant(doubleValue = 4096.0F))
    private double increaseDetectionRangeHorde(double constant){
        return (float) ((NightmareMode.hordeMode || (NightmareUtils.getIsBloodMoon() && this.dimension == -1)) ? constant * 4d : constant);
    }
    @Redirect(method = "updateEntityActionState", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityGhast;canEntityBeSeen(Lnet/minecraft/src/Entity;)Z"))
    private boolean allowShootingThroughWallsBloodMoon(EntityGhast instance, Entity entity){
        if(NightmareUtils.getIsBloodMoon() && instance.dimension == -1){return true;}
        return instance.canEntityBeSeen(entity);
    }
    @Redirect(method = "updateAttackStateClient", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityGhast;canEntityBeSeen(Lnet/minecraft/src/Entity;)Z"))
    private boolean seePlayerThroughWallsHorde(EntityGhast instance, Entity entity){
        return NightmareMode.hordeMode || (NightmareUtils.getIsBloodMoon() && this.dimension == -1) || instance.canEntityBeSeen(entity);
    }

    @Override
    public EntityLivingData onSpawnWithEgg(EntityLivingData par1EntityLivingData) {
        if(NightmareUtils.getIsMobEclipsed(this) && this.rand.nextInt(6) == 0){
            this.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 1000000, 0));
            this.isCreeperVariant = true;
        }
        return super.onSpawnWithEgg(par1EntityLivingData);
    }

    @Inject(method = "dropFewItems", at = @At("TAIL"))
    private void allowBloodOrbDrops(boolean bKilledByPlayer, int iLootingModifier, CallbackInfo ci){
        if (bKilledByPlayer) {
            int bloodOrbID = NightmareUtils.getIsBloodMoon() ? NMItems.bloodOrb.itemID : 0;
            if (bloodOrbID > 0) {
                int var4 = this.rand.nextInt(3)+1;
                // 1 - 3
                if (iLootingModifier > 0) {
                    var4 += this.rand.nextInt(iLootingModifier + 1);
                }
                for (int var5 = 0; var5 < var4; ++var5) {
                    this.dropItem(bloodOrbID, 1);
                }
            }
            if (NightmareUtils.getIsMobEclipsed(this)) {
                for(int i = 0; i < (iLootingModifier * 2) + 1; i++) {
                    if (this.rand.nextInt(8) == 0) {
                        this.dropItem(NMItems.darksunFragment.itemID, 1);
                        if (this.rand.nextBoolean()) {
                            break;
                        }
                    }
                }

                int itemID = this.isCreeperVariant() ? NMItems.creeperTear.itemID : NMItems.ghastTentacle.itemID;

                int var4 = this.rand.nextInt(3);
                if(this.dimension == -1){
                    var4 += 4;
                }
                if (iLootingModifier > 0) {
                    var4 += this.rand.nextInt(iLootingModifier + 1);
                }
                for (int var5 = 0; var5 < var4; ++var5) {
                    if(this.rand.nextInt(3) == 0) continue;
                    this.dropItem(itemID, 1);
                }
            }
        }
    }

    @ModifyConstant(method = "attackEntityFrom", constant = @Constant(floatValue = 1000.0f))
    private float ghastEnrageOnSelfHit(float constant){
        this.rageTimer = 1;
        return 5f;
    }
    @Inject(method = "updateEntityActionState", at =@At("HEAD"))
    private void manageRageState(CallbackInfo ci){
        if(this.rageTimer > 0){
            this.rageTimer++;
            EntityPlayer entityTargeted = this.worldObj.getClosestVulnerablePlayerToEntity(this, 100.0);
            if (entityTargeted != null && this.rageTimer % (this.worldObj.getDifficulty() == Difficulties.HOSTILE ? 4 : 8) == 0) {
                Vec3 ghastLookVec = this.getLook(1.0f);
                double dFireballX = this.posX + ghastLookVec.xCoord;
                double dFireballY = this.posY + (double)(this.height / 2.0f);
                double dFireballZ = this.posZ + ghastLookVec.zCoord;
                double dDeltaX = entityTargeted.posX - dFireballX;
                double dDeltaY = entityTargeted.posY + (double)entityTargeted.getEyeHeight() - dFireballY;
                double dDeltaZ = entityTargeted.posZ - dFireballZ;
                EntityLargeFireball fireball = new EntityLargeFireball(this.worldObj, this, dDeltaX, dDeltaY, dDeltaZ);
                fireball.field_92057_e = 1;
                double dDeltaLength = MathHelper.sqrt_double(dDeltaX * dDeltaX + dDeltaY * dDeltaY + dDeltaZ * dDeltaZ);
                double dUnitDeltaX = dDeltaX / dDeltaLength;
                double dUnitDeltaY = dDeltaY / dDeltaLength;
                double dUnitDeltaZ = dDeltaZ / dDeltaLength;
                fireball.posX = dFireballX + dUnitDeltaX * 4.0;
                fireball.posY = dFireballY + dUnitDeltaY * 4.0 - (double)fireball.height / 2.0;
                fireball.posZ = dFireballZ + dUnitDeltaZ * 4.0;
                this.worldObj.spawnEntityInWorld(fireball);
            }
        }
        if(this.rageTimer > 100){
            this.rageTimer = 0;
        }
    }

    @Inject(method = "updateEntityActionState", at = @At(value = "FIELD", target = "Lnet/minecraft/src/EntityGhast;attackCounter:I",ordinal = 2),cancellable = true)
    private void manageCreeperEclipseVariant(CallbackInfo ci){
        if(NightmareUtils.getIsMobEclipsed(this) && this.isCreeperVariant()){
            this.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 40,0));
            if(this.firstAttack){
                this.worldObj.playSoundEffect(this.entityTargeted.posX,this.entityTargeted.posY,this.entityTargeted.posZ, "mob.ghast.scream",1f,0.8f);
                this.firstAttack = false;
            }

            // Get the positions of the entity and the player
            Vec3 playerPos = Vec3.createVectorHelper(this.entityTargeted.posX,this.entityTargeted.posY,this.entityTargeted.posZ);
            Vec3 entityPos = Vec3.createVectorHelper(this.posX,this.posY,this.posZ);
            // Calculate the direction vector
            Vec3 velocity = entityPos.subtract(playerPos);
            velocity.normalize();

            // Calculate the velocity vector
            velocity.scale(0.1d);
            if (this.getDistanceSqToEntity(this.entityTargeted) > 4) {
                if (this.hurtResistantTime <= 8) {
                    this.motionX = velocity.xCoord;
                    this.motionY = velocity.yCoord;
                    this.motionZ = velocity.zCoord;
                }
            } else{
                this.worldObj.newExplosion(this,this.posX,this.posY,this.posZ,6f, false, true);
                this.setDead();
            }
            ci.cancel();
        }
    }

    @Unique private boolean isCreeperVariant(){
        return this.isPotionActive(Potion.moveSpeed) || this.isCreeperVariant;
    }


    @Inject(method = "getCanSpawnHere", at = @At("HEAD"),cancellable = true)
    private void manageOverworldSpawn(CallbackInfoReturnable<Boolean> cir){
        if(this.dimension == 0){
            if (NightmareUtils.getIsBloodMoon() || NightmareUtils.getIsMobEclipsed(this)) {
                if (this.getCanSpawnHereNoPlayerDistanceRestrictions() && this.posY >= 63) {
                    cir.setReturnValue(true);
                }
            } else{
                cir.setReturnValue(false);
            }
        }
    }
    @ModifyArg(method = "getCanSpawnHere", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/World;getClosestPlayer(DDDD)Lnet/minecraft/src/EntityPlayer;"),index = 3)
    private double increaseGhastSpawnrateOnBloodMoon(double par1){
        if(NightmareUtils.getIsBloodMoon() && this.dimension == -1){return 16.0;}
        return par1;
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void killIfTooHigh(CallbackInfo ci){
        if(this.dimension == 0 && this.posY >= 200){
            this.setDead();
        }
    }

    @ModifyConstant(method = "updateEntityActionState",constant = @Constant(intValue = 10,ordinal = 0))
    private int lowerSoundThreshold(int constant){
        EntityGhast thisObj = (EntityGhast)(Object)this;
        if(thisObj.dimension == 0 && !NightmareUtils.getIsMobEclipsed(this)){
            return constant * 2;
        }
        if(thisObj.worldObj != null && NightmareUtils.getWorldProgress(thisObj.worldObj)>0){
            return constant - NightmareUtils.getWorldProgress(thisObj.worldObj)*2 -1;
            // 9 -> 7 -> 4 -> 2
        }
        return constant;
    }
    @ModifyConstant(method = "updateEntityActionState",constant = @Constant(intValue = 20,ordinal = 1))
    private int lowerAttackThreshold(int constant){
        EntityGhast thisObj = (EntityGhast)(Object)this;
        if(thisObj.dimension == 0 && !NightmareUtils.getIsMobEclipsed(this)){
            return NightmareUtils.divByNiteMultiplier(constant * 2, 10);
        }
        if(thisObj.worldObj != null && NightmareUtils.getWorldProgress(thisObj.worldObj)>0){
            return NightmareUtils.divByNiteMultiplier((int) (constant - NightmareUtils.getWorldProgress(thisObj.worldObj) * 1.5 - 5), 8);
            // 15 -> 13 -> 12 -> 10
        }
        return constant;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void manageEclipseChance(World world, CallbackInfo ci){
        NightmareUtils.manageEclipseChance(this,12);
    }

    @Inject(method = "fireAtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/World;spawnEntityInWorld(Lnet/minecraft/src/Entity;)Z"))
    private void boostGhastForwardOnEclipse(CallbackInfo ci){
        if (NightmareUtils.getIsMobEclipsed(this) && this.rand.nextInt(3) == 0) {
            Vec3 playerPos = Vec3.createVectorHelper(this.entityTargeted.posX,this.entityTargeted.posY,this.entityTargeted.posZ);
            Vec3 entityPos = Vec3.createVectorHelper(this.posX,this.posY,this.posZ);
            // Calculate the direction vector
            Vec3 velocity = entityPos.subtract(playerPos);
            velocity.normalize();

            // Calculate the velocity vector
            velocity.scale(0.02d);
            if (this.getDistanceSqToEntity(this.entityTargeted) > 256) {
                this.motionX = velocity.xCoord;
                this.motionY = velocity.yCoord;
                this.motionZ = velocity.zCoord;
            }
        }
    }

    @ModifyConstant(method = "fireAtTarget", constant = @Constant(intValue = -40))
    private int lowerAttackCooldownOnFire(int constant){
        EntityGhast thisObj = (EntityGhast)(Object)this;
        return (int) ((- 10 - thisObj.rand.nextInt(21)) * NightmareUtils.getNiteMultiplier());
        // from -10 to -30
    }
}
