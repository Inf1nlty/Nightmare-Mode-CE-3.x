package com.itlesports.nightmaremode.item;

import net.minecraft.src.Item;

public class NMItem extends Item {
    public NMItem(int id) {
        super(id);
    }

    public String getModId() {
        return "nightmare_mode";
    }
}