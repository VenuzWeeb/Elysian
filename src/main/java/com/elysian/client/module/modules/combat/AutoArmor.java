package com.elysian.client.module.modules.combat;

import com.elysian.client.module.ModuleType;
import com.elysian.client.property.NumberProperty;
import com.elysian.client.property.Property;
import com.elysian.client.module.ToggleableModule;
import com.elysian.client.util.FriendUtil;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


public class AutoArmor extends ToggleableModule {

    private final NumberProperty<Integer> Delay = new NumberProperty<Integer>(5, 0, 50, "Delay");
    private final NumberProperty<Integer> PlayerRange = new NumberProperty<Integer>(5, 0, 50, "Player Range");
    private final NumberProperty<Integer> CrystalRange = new NumberProperty<Integer>(5, 0, 50, "Crystal Range");
    private final NumberProperty<Integer> BootPercent = new NumberProperty<Integer>(5, 0, 50, "Boot Percent");
    private final NumberProperty<Integer> ChestPercent = new NumberProperty<Integer>(5, 0, 50, "Chest Percent");
    private final Property<Boolean> SmartMod = new Property<Boolean>(true, "Smart Mode", "Smart Mode");
    private final Property<Boolean> EquipArmour = new Property<Boolean>(true, "Equip Armour", "Equip Armour");

    private int delay_count;

    public AutoArmor() {
        super("AutoArmor", new String[] {"AutoArmor"}, "AutoArmor", ModuleType.COMBAT);
        this.offerProperties(Delay, PlayerRange, CrystalRange, BootPercent, ChestPercent, SmartMod, EquipArmour, this.keybind);
    }
    @Override
    protected void onEnable() {
        delay_count = 0;
    }

    public void update() {

        if (mc.player.ticksExisted % 2 == 0) return;

        boolean flag = false;

        if (delay_count < Delay.getValue()) {
            delay_count++;
            return;
        }
        delay_count = 0;

        if (SmartMod.getValue()) {
            if (!(is_crystal_in_range(CrystalRange.getValue()) || is_player_in_range(PlayerRange.getValue()))) flag = true;
        }

        if (flag) {
            if (mc.gameSettings.keyBindUseItem.isKeyDown() && mc.player.getHeldItemMainhand().getItem() == Items.EXPERIENCE_BOTTLE) {
                take_off();
            }
            return;
        }

        if (!EquipArmour.getValue()) return;

        if (mc.currentScreen instanceof GuiContainer && !(mc.currentScreen instanceof InventoryEffectRenderer)) return;

        int[] bestArmorSlots = new int[4];
        int[] bestArmorValues = new int[4];

        // initialize with currently equipped armor
        for(int armorType = 0; armorType < 4; armorType++)
        {
            ItemStack oldArmor = mc.player.inventory.armorItemInSlot(armorType);

            if(oldArmor.getItem() instanceof ItemArmor)
                bestArmorValues[armorType] =
                        ((ItemArmor)oldArmor.getItem()).damageReduceAmount;

            bestArmorSlots[armorType] = -1;
        }

        // search inventory for better armor
        for(int slot = 0; slot < 36; slot++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);

            if (stack.getCount() > 1)
                continue;

            if(!(stack.getItem() instanceof ItemArmor))
                continue;

            ItemArmor armor = (ItemArmor)stack.getItem();
            int armorType = armor.armorType.ordinal() - 2;

            if (armorType == 2 && mc.player.inventory.armorItemInSlot(armorType).getItem().equals(Items.ELYTRA)) continue;

            int armorValue = armor.damageReduceAmount;

            if(armorValue > bestArmorValues[armorType])
            {
                bestArmorSlots[armorType] = slot;
                bestArmorValues[armorType] = armorValue;
            }
        }

        // equip better armor
        for(int armorType = 0; armorType < 4; armorType++)
        {
            // check if better armor was found
            int slot = bestArmorSlots[armorType];
            if(slot == -1)
                continue;

            // check if armor can be swapped
            // needs 1 free slot where it can put the old armor
            ItemStack oldArmor = mc.player.inventory.armorItemInSlot(armorType);
            if(oldArmor != ItemStack.EMPTY || mc.player.inventory.getFirstEmptyStack() != -1)
            {
                // hotbar fix
                if(slot < 9)
                    slot += 36;

                // swap armor
                mc.playerController.windowClick(0, 8 - armorType, 0,
                        ClickType.QUICK_MOVE, mc.player);
                mc.playerController.windowClick(0, slot, 0,
                        ClickType.QUICK_MOVE, mc.player);

                break;
            }
        }

    }

    public boolean is_player_in_range(int range) {
        for (EntityPlayer player : mc.world.playerEntities.stream().filter(entityPlayer -> !FriendUtil.isFriend(entityPlayer.getName())).collect(Collectors.toList())) {
            if (player == mc.player) continue;
            if (mc.player.getDistance(player) < range) {
                return true;
            }
        }
        return false;
    }

    public boolean is_crystal_in_range(int range) {
        for (Entity c : mc.world.loadedEntityList.stream().filter(entity -> entity instanceof EntityEnderCrystal).collect(Collectors.toList())) {
            if (mc.player.getDistance(c) < range) {
                return true;
            }
        }
        return false;
    }

    public void take_off() {
        if (!is_space()) return;

        for (final Map.Entry<Integer, ItemStack> armorSlot : get_armour().entrySet()) {
            final ItemStack stack = armorSlot.getValue();
            if (is_healed(stack)) {
                mc.playerController.windowClick(0, armorSlot.getKey(), 0, ClickType.QUICK_MOVE, mc.player);
                return;
            }
        }

    }

    public boolean is_space() {
        for (final Map.Entry<Integer, ItemStack> invSlot : get_inv().entrySet()) {
            final ItemStack stack = invSlot.getValue();
            if (stack.getItem() == Items.AIR) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, ItemStack> get_inv() {
        return get_inv_slots(9, 44);
    }

    private static Map<Integer, ItemStack> get_armour() {
        return get_inv_slots(5, 8);
    }

    private static Map<Integer, ItemStack> get_inv_slots(int current, final int last) {
        final Map<Integer, ItemStack> fullInventorySlots = new HashMap<>();
        while (current <= last) {
            fullInventorySlots.put(current, mc.player.inventoryContainer.getInventory().get(current));
            current++;
        }
        return fullInventorySlots;
    }

    public boolean is_healed(ItemStack item) {
        if (item.getItem() == Items.DIAMOND_BOOTS || item.getItem() == Items.DIAMOND_HELMET) {
            double max_dam = item.getMaxDamage();
            double dam_left = item.getMaxDamage() - item.getItemDamage();
            double percent = (dam_left / max_dam) * 100;
            return percent >= BootPercent.getValue();
        } else {
            double max_dam = item.getMaxDamage();
            double dam_left = item.getMaxDamage() - item.getItemDamage();
            double percent = (dam_left / max_dam) * 100;
            return percent >= ChestPercent.getValue();
        }}
}
