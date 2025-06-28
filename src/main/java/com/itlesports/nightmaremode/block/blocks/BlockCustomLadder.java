package com.itlesports.nightmaremode.block.blocks;

import btw.block.blocks.LadderBlock;
import com.itlesports.nightmaremode.block.NMBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.IconRegister;

import java.util.Random;

public class BlockCustomLadder extends LadderBlock {
    private final double speedModifier;
    private final int dropItemID;
    public BlockCustomLadder(int iBlockID, double speedModifier) {
        super(iBlockID);
        this.setPicksEffectiveOn();
        this.setAxesEffectiveOn(false);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.speedModifier = 0.2 * speedModifier;
        this.dropItemID = iBlockID;
    }
    @Override
    public int idDropped(int iMetadata, Random rand, int iFortuneModifier) {
        return this.dropItemID;
    }

    public double getSpeedModifier() {
        return speedModifier;
    }
    @Override
    @Environment(value= EnvType.CLIENT)
    public void registerIcons(IconRegister register) {
        if (this.isStoneLadder()) {
            this.blockIcon = register.registerIcon("nmStoneLadder");
        } else{
            this.blockIcon = register.registerIcon("nmIronLadder");
        }
    }

    private boolean isStoneLadder(){
        return this.blockID == NMBlocks.stoneLadder.blockID;
    }
}
