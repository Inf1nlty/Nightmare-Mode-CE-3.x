package com.itlesports.nightmaremode.mixin;

import btw.item.BTWItems;
import btw.world.util.difficulty.Difficulties;
import com.itlesports.nightmaremode.NightmareUtils;
import com.itlesports.nightmaremode.entity.EntityRadioactiveEnderman;
import com.itlesports.nightmaremode.item.NMItems;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EntityEnderman.class)
public abstract class EntityEndermanMixin extends EntityMob {

    public EntityEndermanMixin(World par1World) {
        super(par1World);
    }
    @Shadow protected abstract void angerNearbyEndermen(EntityPlayer targetPlayer);
    @Shadow private int teleportDelay;
    @Shadow protected abstract boolean teleportToEntity(Entity par1Entity);

    @Shadow protected abstract boolean teleportRandomly();

    @Shadow public abstract int getCarried();

    @Shadow public abstract void setCarried(int par1);

    @Unique int patience = 45;
    @Unique int inventorySwitchCooldown = 80;

    @Inject(method = "applyEntityAttributes", at = @At("TAIL"))
    private void applyAdditionalAttributes(CallbackInfo ci){
        int progress = NightmareUtils.getWorldProgress(this.worldObj);
        double bloodMoonModifier = NightmareUtils.getIsBloodMoon() ? 1.5 : 1;
        boolean isBloodMoon = bloodMoonModifier > 1;

        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setAttribute(((progress > 0 ? 0.38f : 0.35f) + (isBloodMoon ? 0.04 : 0) + (NightmareUtils.getIsMobEclipsed(this) ? 0.07 : 0)) * NightmareUtils.getNiteMultiplier());
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setAttribute((6.0 + progress * 2 + bloodMoonModifier) * NightmareUtils.getNiteMultiplier());
        // 7 -> 9 -> 11 -> 13
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setAttribute(40 + progress * 20);
        // 40 -> 60 -> 80 -> 100
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void manageEclipseChance(World world, CallbackInfo ci){
        NightmareUtils.manageEclipseChance(this,10);
    }

    @Inject(method = "dropFewItems", at = @At("TAIL"))
    private void allowBloodOrbDrops(boolean bKilledByPlayer, int iLootingModifier, CallbackInfo ci){
        int bloodOrbID = NightmareUtils.getIsBloodMoon() ? NMItems.bloodOrb.itemID : 0;
        if (bloodOrbID > 0 && bKilledByPlayer) {
            int var4 = this.rand.nextInt(4)+1;
            // 1 - 4
            if (iLootingModifier > 0) {
                var4 += this.rand.nextInt(iLootingModifier + 1);
            }
            for (int var5 = 0; var5 < var4; ++var5) {
                this.dropItem(bloodOrbID, 1);
            }
        }
    }
    @Inject(method = "dropFewItems", at = @At("HEAD"))
    private void manageEclipseShardDrops(boolean bKilledByPlayer, int lootingLevel, CallbackInfo ci){
        if (bKilledByPlayer && NightmareUtils.getIsMobEclipsed(this)) {
            for(int i = 0; i < (lootingLevel * 2) + 1; i++) {
                if (this.rand.nextInt(8) == 0) {
                    this.dropItem(NMItems.darksunFragment.itemID, 1);
                    if (this.rand.nextBoolean()) {
                        break;
                    }
                }
            }

            int itemID = Item.eyeOfEnder.itemID;

            int var4 = this.rand.nextInt(3);
            if (lootingLevel > 0) {
                var4 += this.rand.nextInt(lootingLevel + 1);
            }
            for (int var5 = 0; var5 < var4; ++var5) {
                if(this.rand.nextInt(3) == 0) continue;
                this.dropItem(itemID, 1);
            }
        }
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(intValue = 4))
    private int increaseAttemptsToTeleportPlayer(int constant){
        if(this.worldObj.getDifficulty() == Difficulties.HOSTILE){
            if (NightmareUtils.getIsBloodMoon()) {
                return 9;
            } else if(NightmareUtils.getIsMobEclipsed(this)){
                return 18;
            }
            return 6;
        }
        return constant;
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityEnderman;teleportRandomly()Z",ordinal = 0))
    private boolean helpEndermanTeleportPlayerMoreOften(EntityEnderman instance){
        if(NightmareUtils.getIsMobEclipsed(this) && this.rand.nextBoolean()){
            return true;
        }
        return this.teleportRandomly();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 16.0))
    private double canTeleportPlayerFromFurtherAway(double constant){
        return 64;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "HEAD"))
    private void incrementAbilityTimer(CallbackInfo ci){
        if (this.entityToAttack != null) {
            this.inventorySwitchCooldown = Math.max(this.inventorySwitchCooldown - 1 , 0);
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {
        if(NightmareUtils.getIsMobEclipsed(this) && entity instanceof EntityPlayer){
            int heldItemID = this.getCarried();

            if (this.inventorySwitchCooldown == 0) {
                ItemStack[] hotbar = new ItemStack[9];
                System.arraycopy(((EntityPlayer) entity).inventory.mainInventory, 0, hotbar, 0, 9);
                for(int i = 0; i < 9; i++){
                    int j = this.rand.nextInt(i + 1);
                    ItemStack temp = hotbar[i];
                    hotbar[i] = hotbar[j];
                    hotbar[j] = temp;
                }
                System.arraycopy(hotbar, 0, ((EntityPlayer) entity).inventory.mainInventory, 0, 9);

                entity.worldObj.playSoundEffect(entity.posX, entity.posY, entity.posZ, "mob.endermen.portal", 1.0f, 1.0f);
                this.inventorySwitchCooldown = 80 + this.rand.nextInt(40);
            }

            if(heldItemID == Block.tnt.blockID){
                this.worldObj.newExplosion(this, entity.posX, entity.posY, entity.posZ, 2, false, false);

                this.setCarried(0);

                double deltaX = entity.posX - this.posX;
                double deltaZ = entity.posZ - this.posZ;
                double distance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);

                // Normalize the direction vector
                deltaX /= distance;
                deltaZ /= distance;

                entity.motionX += deltaX * 4;
                entity.motionZ += deltaZ * 4;
                entity.motionY += 0.4;

                if (entity.motionY > 0.4) {
                    entity.motionY = 0.4;
                }
                entity.isAirBorne = true;
            }
        }
        return super.attackEntityAsMob(entity);
    }

    @Inject(method = "teleportTo(DDD)Z", at = @At(value = "RETURN",ordinal = 1))
    private void chanceToHaveItem(double par1, double par3, double par5, CallbackInfoReturnable<Boolean> cir){
        if (NightmareUtils.getIsMobEclipsed(this)) {
            int i = this.rand.nextInt(8);
            if(i == 0){
                this.setCarried(Block.tnt.blockID);
            }
        }
    }


    @Inject(method = "findPlayerToAttack", at = @At("TAIL"),locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void hostileInEnd(CallbackInfoReturnable<Entity> cir, EntityPlayer target){
        if (target != null){
            ItemStack var2 = target.inventory.armorInventory[3];
            if (target.dimension == 1 && this.worldObj.getDifficulty() == Difficulties.HOSTILE) {
                if (var2 == null) {
                    this.angerNearbyEndermen(target);
                    cir.setReturnValue(target);
                } else if(var2.itemID != BTWItems.enderSpectacles.itemID){
                    this.angerNearbyEndermen(target);
                    cir.setReturnValue(target);
                }
            } else {
                if(var2 != null && var2.itemID == 86){ // carved pumpkin aggros all endermen
                    this.angerNearbyEndermen(target);
                    cir.setReturnValue(target);
                }
            }
        }
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(doubleValue = 256))
    private double endermanRangeToTeleport(double constant){
        return 128;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityEnderman;setAIMoveSpeed(F)V"))
    private void teleportToNearestPlayerOnAggro(CallbackInfo ci){
        if(this.entityToAttack instanceof EntityPlayer){
            this.teleportToEntity(this.entityToAttack);
        }
    }

    @Inject(method = "onLivingUpdate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/src/EntityEnderman;entityMobOnLivingUpdate()V"))
    private void updateEnemyTeleport(CallbackInfo ci) {
        if (!this.worldObj.isRemote && this.isEntityAlive() && this.entityToAttack != null) {
            if (entityToAttack.getDistanceSqToEntity(this) > 6.0D && entityToAttack.getDistanceSqToEntity(this) < 900) {
                if (this.teleportDelay++ >= 120 && this.teleportEnemy()) {
                    this.teleportDelay = 0;
                }
            } else if (entityToAttack.getDistanceSqToEntity(this) < 256D) {
                this.teleportDelay = 0;
            }
        }
    }

    @Inject(method = "findPlayerToAttack", at = @At("TAIL"), cancellable = true)
    private void attackClosePlayers(CallbackInfoReturnable<Entity> cir){
        EntityPlayer target = null;
        if (this.worldObj.getDifficulty() == Difficulties.HOSTILE) {
            target = this.worldObj.getClosestVulnerablePlayerToEntity(this, 3.25);
        }
        EntityPlayer effectTarget = this.worldObj.getClosestVulnerablePlayerToEntity(this, 7);
        if(effectTarget != null && this.dimension != 1){
            EntityEnderman thisObj = (EntityEnderman)(Object)this;
            if(thisObj instanceof EntityRadioactiveEnderman){
                if (!effectTarget.isPotionActive(Potion.poison)) {
                    effectTarget.addPotionEffect(new PotionEffect(Potion.poison.id, 80, 0));
                }
            } else {
                effectTarget.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 60, 0));
                effectTarget.addPotionEffect(new PotionEffect(Potion.weakness.id, 60, 0));
                effectTarget.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 60, 0));
                effectTarget.addPotionEffect(new PotionEffect(Potion.blindness.id, 60, 0));
            }
            if (target != null) {
                this.patience--;
                if(this.patience <= 0) {
                    this.patience = 0;
                    this.angerNearbyEndermen(target);
                    cir.setReturnValue(target);
                }
            } else{
                this.patience = Math.min(++this.patience,30);
            }
        }
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/World;isDaytime()Z"))
    private boolean doNotLoseAggroDuringTheDay(World instance){
        return false;
    }
    @Unique
    protected boolean teleportEnemy() {
        if (this.entityToAttack == null) return false;
        Entity target = this.entityToAttack;

        // === tweakable constants ===
        double MAX_Y_DIFF    = 4.0D;  // allowed vertical shift when below 60
        final double MIN_DISTANCE  = 4.0D;  // minimum straight-line teleport
        final int    PARTICLE_COUNT = 128;

        // 1) direction from target → enderman eye-level
        Vec3 dir = this.worldObj.getWorldVec3Pool()
                .getVecFromPool(
                        this.posX - target.posX,
                        this.boundingBox.minY + (this.height/2.0F) - target.posY + target.getEyeHeight(),
                        this.posZ - target.posZ
                ).normalize();

        // 2) stash old pos
        double oldX = target.posX, oldY = target.posY, oldZ = target.posZ;

        // 3) compute candidate teleport
        target.posX = oldX + (this.rand.nextDouble() - 0.5D)*8.0D + dir.xCoord*0.8D;
        target.posY = oldY + (this.rand.nextInt(16) - 8)   + dir.yCoord*0.8D;
        target.posZ = oldZ + (this.rand.nextDouble() - 0.5D)*8.0D + dir.zCoord*0.8D;

        // 4) enforce minimum distance
        double dx = target.posX - oldX;
        double dy = target.posY - oldY;
        double dz = target.posZ - oldZ;
        double distSq = dx*dx + dy*dy + dz*dz;
        if (distSq < MIN_DISTANCE*MIN_DISTANCE) {
            // too short, skip teleport
            target.setPosition(oldX, oldY, oldZ);
            return false;
        }

        // 5) conditional Y-clamp
        //    only enforce if either old or new Y <= 60
        if (oldY <= 60.0D || target.posY <= 60.0D){
            if (Math.abs(target.posY - oldY) > MAX_Y_DIFF) {
                target.setPosition(oldX, oldY, oldZ);
                return false;
            }
        }

        int xb = MathHelper.floor_double(target.posX),
                yb = MathHelper.floor_double(target.posY),
                zb = MathHelper.floor_double(target.posZ);

        // 6) must be in loaded chunk
        if (!this.worldObj.blockExists(xb, yb, zb)) {
            target.setPosition(oldX, oldY, oldZ);
            return false;
        }

        // 7) ground-finding & clearance (unchanged)
        boolean canTeleport = false;
        while (!canTeleport && yb > 0) {
            int blockId = this.worldObj.getBlockId(xb, yb, zb);
            if (blockId != 0 && Block.blocksList[blockId].blockMaterial.blocksMovement()) {
                canTeleport = true;
                int clearance = MathHelper.ceiling_float_int(target.height);
                for (int i = 1; i <= clearance; i++) {
                    int aboveId = this.worldObj.getBlockId(xb, yb + i, zb);
                    if (aboveId != 0 && Block.blocksList[aboveId].blockMaterial.blocksMovement()) {
                        canTeleport = false;
                        target.posY -= clearance + 1;
                        yb        -= clearance + 1;
                        break;
                    }
                }
                if (canTeleport) {
                    target.posY++;
                    yb++;
                }
            } else {
                target.posY--;
                yb--;
            }
        }

        if (!canTeleport) {
            target.setPosition(oldX, oldY, oldZ);
            return false;
        }

        // 8) suffocation check
        target.setPosition(target.posX, target.posY, target.posZ);
        if (target.isEntityInsideOpaqueBlock()) {
            target.setPosition(oldX, oldY, oldZ);
            return false;
        }

        // 9) commit teleport
        if (target instanceof EntityPlayer) {
            ((EntityPlayer)target).setPositionAndUpdate(target.posX, target.posY, target.posZ);
        } else {
            target.setPosition(target.posX, target.posY, target.posZ);
        }

        // 10) particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double t  = (double)i/(PARTICLE_COUNT - 1);
            float  vx = (this.rand.nextFloat() - 0.5F)*0.2F;
            float  vy = (this.rand.nextFloat() - 0.5F)*0.2F;
            float  vz = (this.rand.nextFloat() - 0.5F)*0.2F;
            double px = oldX + dx*t + (this.rand.nextDouble() - 0.5D)*target.width*2.0D;
            double py = oldY + dy*t + this.rand.nextDouble()*target.height;
            double pz = oldZ + dz*t + (this.rand.nextDouble() - 0.5D)*target.width*2.0D;
            this.worldObj.spawnParticle("portal", px, py, pz, vx, vy, vz);
        }

        // 11) sounds
        this.worldObj.playSoundEffect(oldX, oldY, oldZ, "mob.endermen.portal", 1.0F, 1.0F);
        target.playSound   ("mob.endermen.portal", 1.0F, 1.0F);

        return true;
    }


}
