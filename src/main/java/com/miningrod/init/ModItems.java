package com.miningrod.init;

import com.miningrod.MiningRodMod;
import com.miningrod.item.MiningRodItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, MiningRodMod.MOD_ID);

    public static final RegistryObject<MiningRodItem> MINING_ROD =
        ITEMS.register("mining_rod", () -> new MiningRodItem(
            new Item.Properties().durability(128).fireResistant()
        ));
}
