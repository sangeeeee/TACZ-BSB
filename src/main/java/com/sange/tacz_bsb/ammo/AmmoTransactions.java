package com.sange.tacz_bsb.ammo;

import com.sange.tacz_bsb.BsbConfig;
import com.sange.tacz_bsb.BsbContent;
import com.sange.tacz_bsb.item.TieredAmmoItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class AmmoTransactions {
    private AmmoTransactions() {}
    public static boolean active(ModernKineticGunScriptAPI api) {
        return api.getShooter() != null && !api.getShooter().level().isClientSide && api.getGunIndex() != null;
    }
    public static ResourceLocation ammoId(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun == null ? null : TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                .map(index -> index.getGunData().getAmmoId()).orElse(null);
    }
    public static int tier(ItemStack stack) {
        return stack.getItem() instanceof TieredAmmoItem ammo ? ammo.tier() : 1;
    }
    public static int boxTier(ItemStack stack) { return stack.getOrDefault(BsbContent.BOX_TIER.get(), 1); }
    public static ItemStack stack(ResourceLocation id, int tier, int count) {
        if (tier == 1) return AmmoItemBuilder.create().setId(id).setCount(count).build();
        if (tier != 2 && tier != 3) throw new IllegalArgumentException("Invalid ammunition tier");
        TieredAmmoItem item = tier == 3 ? BsbContent.PRECISE_AMMO.get() : BsbContent.IMPROVED_AMMO.get();
        ItemStack stack = new ItemStack(item, count);
        item.setAmmoId(stack, id);
        return stack;
    }
    public static int extract(IItemHandler inventory, ItemStack stack, int requested) {
        IGun gun = IGun.getIGunOrNull(stack);
        AmmoLedger state = AmmoState.read(stack, gun);
        RoundQueue extracted = new RoundQueue();
        int left = Math.max(0, requested);
        for (int slot = 0; slot < inventory.getSlots() && left > 0; slot++) {
            ItemStack candidate = inventory.getStackInSlot(slot);
            if (candidate.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(stack, candidate)) {
                ItemStack obtained = inventory.extractItem(slot, left, false);
                if (!obtained.isEmpty()) {
                    extracted.addLast(tier(obtained), obtained.getCount());
                    left -= obtained.getCount();
                }
            } else if (candidate.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(stack, candidate)) {
                int count = Math.min(Math.max(box.getAmmoCount(candidate), 0), left);
                if (count == 0) continue;
                int quality = boxTier(candidate);
                if (!box.isCreative(candidate) && !box.isAllTypeCreative(candidate)) {
                    box.setAmmoCount(candidate, box.getAmmoCount(candidate) - count);
                    if (box.getAmmoCount(candidate) == 0) {
                        box.setAmmoId(candidate, com.tacz.guns.api.DefaultAssets.EMPTY_AMMO_ID);
                        candidate.remove(BsbContent.BOX_TIER.get());
                    }
                }
                extracted.addLast(quality, count);
                left -= count;
            }
        }
        state.reserve.append(extracted);
        AmmoState.write(stack, gun, state);
        return extracted.size();
    }
    public static int consume(ModernKineticGunScriptAPI api, int amount) {
        if (amount <= 0) return 0;
        ItemStack stack = api.getItemStack();
        AbstractGunItem gun = api.getAbstractGunItem();
        if (api.useInventoryAmmo() && !api.isReloadingNeedConsumeAmmo()) {
            AmmoLedger state = AmmoState.read(stack, gun);
            state.reserve.addLast(1, amount);
            AmmoState.write(stack, gun, state);
            return amount;
        }
        if (gun.useDummyAmmo(stack)) {
            int obtained = gun.findAndExtractDummyAmmo(stack, amount);
            AmmoLedger state = AmmoState.read(stack, gun);
            state.reserve.addLast(1, obtained);
            AmmoState.write(stack, gun, state);
            return obtained;
        }
        IItemHandler inventory = api.getShooter().getCapability(Capabilities.ItemHandler.ENTITY, null);
        return inventory == null ? 0 : gun.findAndExtractInventoryAmmo(inventory, stack, amount);
    }
    private static boolean free(ModernKineticGunScriptAPI api) {
        return !api.isReloadingNeedConsumeAmmo() || api.getGunIndex().getGunData().getReloadData().isInfinite();
    }
    public static int put(ModernKineticGunScriptAPI api, int amount) {
        if (amount <= 0) return 0;
        ItemStack stack = api.getItemStack();
        AbstractGunItem gun = api.getAbstractGunItem();
        AmmoLedger state = AmmoState.read(stack, gun);
        boolean fuel = api.getGunIndex().getGunData().getReloadData().getType() == FeedType.FUEL;
        if (fuel && !state.reserve.isEmpty()) {
            int count = Math.min(amount, Math.max(0, api.getMaxAmmoCount() - state.magazine.size()));
            if (count == 0) return amount;
            int type = state.reserve.removeFirst();
            state.magazine.addLast(type, count);
            AmmoState.write(stack, gun, state);
            return amount - count;
        }
        int excess = state.load(amount, api.getMaxAmmoCount(),
                BsbConfig.fifo(api.getGunIndex().getGunData().getAmmoId().toString()), free(api));
        AmmoState.write(stack, gun, state);
        return excess;
    }
    public static int remove(ModernKineticGunScriptAPI api, int count) {
        ItemStack stack = api.getItemStack();
        AmmoLedger state = AmmoState.read(stack, api.getAbstractGunItem());
        int removed = state.removeForTransfer(count);
        AmmoState.write(stack, api.getAbstractGunItem(), state);
        return removed;
    }
    public static void chamber(ModernKineticGunScriptAPI api, boolean present) {
        ItemStack stack = api.getItemStack();
        AmmoLedger state = AmmoState.read(stack, api.getAbstractGunItem());
        if (api.getBolt() != Bolt.OPEN_BOLT) state.chamber(present, free(api));
        AmmoState.write(stack, api.getAbstractGunItem(), state);
    }
    /** 0 = dry fire, 1 = ordinary, 2 = improved, 3 = precise. Called once per consumed cartridge. */
    public static int fire(ModernKineticGunScriptAPI api) {
        ItemStack stack = api.getItemStack();
        AbstractGunItem gun = api.getAbstractGunItem();
        Bolt bolt = api.getBolt();
        AmmoLedger state = AmmoState.read(stack, gun);
        if (api.useInventoryAmmo()) {
            if (bolt == Bolt.MANUAL_ACTION) {
                int fired = state.fire(false, true);
                AmmoState.write(stack, gun, state);
                return fired;
            }
            // Chamber fires first; a newly extracted round becomes the next chambered round.
            int oldChamber = state.chamber;
            int got = consume(api, 1);
            state = AmmoState.read(stack, gun);
            int obtained = got > 0 ? state.reserve.removeFirst() : 0;
            int fired = oldChamber != 0 && bolt != Bolt.OPEN_BOLT ? oldChamber : obtained;
            state.chamber = oldChamber != 0 && bolt != Bolt.OPEN_BOLT ? obtained : 0;
            AmmoState.write(stack, gun, state);
            return fired;
        }
        int fired = state.fire(bolt == Bolt.OPEN_BOLT, bolt == Bolt.MANUAL_ACTION);
        AmmoState.write(stack, gun, state);
        return fired;
    }
    public static int peek(ModernKineticGunScriptAPI api) {
        AmmoLedger state = AmmoState.read(api.getItemStack(), api.getAbstractGunItem());
        int next = state.nextRound(api.getBolt() == Bolt.OPEN_BOLT, api.getBolt() == Bolt.MANUAL_ACTION);
        if (next != 0) return next;
        return 1; // Unbacked creative/infinite ammunition is ordinary.
    }
    public static void returnPending(LivingEntity owner, ItemStack stack) {
        if (owner.level().isClientSide || !(stack.getItem() instanceof AbstractGunItem gun) || ammoId(stack) == null) return;
        AmmoLedger state = AmmoState.read(stack, gun);
        RoundQueue refund = state.reserve.take(state.reserve.size());
        refund.append(state.transfer.take(state.transfer.size()));
        if (refund.isEmpty()) return;
        AmmoState.write(stack, gun, state);
        refund(owner, stack, refund, false);
    }
    public static void unload(LivingEntity owner, ItemStack stack, boolean includeChamber) {
        if (owner.level().isClientSide || !(stack.getItem() instanceof AbstractGunItem gun)) return;
        ResourceLocation id = ammoId(stack);
        if (id == null || (gun.useInventoryAmmo(stack) && !includeChamber)) return;
        AmmoLedger state = AmmoState.read(stack, gun);
        RoundQueue rounds = state.magazine.take(state.magazine.size());
        if (includeChamber && state.chamber != 0) {
            rounds.addLast(state.chamber, 1);
            state.chamber = 0;
        }
        RoundQueue pending = state.reserve.take(state.reserve.size());
        pending.append(state.transfer.take(state.transfer.size()));
        AmmoState.write(stack, gun, state);
        boolean fuel = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).orElseThrow()
                .getGunData().getReloadData().getType() == FeedType.FUEL;
        refund(owner, stack, pending, false);
        if (!fuel) refund(owner, stack, rounds, true);
    }
    private static void refund(LivingEntity owner, ItemStack gunStack, RoundQueue rounds, boolean loaded) {
        IGun gun = IGun.getIGunOrNull(gunStack);
        if (loaded && TimelessAPI.getCommonGunIndex(gun.getGunId(gunStack)).orElseThrow()
                .getGunData().getReloadData().isInfinite()) return;
        if (gun.useDummyAmmo(gunStack)) {
            gun.addDummyAmmoAmount(gunStack, rounds.size());
            return;
        }
        if (owner instanceof Player player && player.isCreative()) return;
        ResourceLocation id = ammoId(gunStack);
        for (int run : rounds.runs()) {
            int left = RoundQueue.count(run);
            while (left > 0) {
                ItemStack item = stack(id, RoundQueue.type(run), 1);
                int count = Math.min(left, item.getMaxStackSize());
                item.setCount(count);
                if (owner instanceof Player player) ItemHandlerHelper.giveItemToPlayer(player, item);
                else owner.spawnAtLocation(item);
                left -= count;
            }
        }
    }
}

