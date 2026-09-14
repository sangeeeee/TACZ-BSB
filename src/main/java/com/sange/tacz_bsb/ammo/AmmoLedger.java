package com.sange.tacz_bsb.ammo;

/** Mutable transaction state; never expose this object as an ItemStack component. */
public final class AmmoLedger {
    public final RoundQueue magazine;
    public final RoundQueue reserve;
    public final RoundQueue transfer;
    /** 0 = empty, 1 = normal, 2 = precise. */
    public int chamber;

    public AmmoLedger(RoundQueue magazine, RoundQueue reserve, RoundQueue transfer, int chamber) {
        if (chamber < 0 || chamber > 2) throw new IllegalArgumentException("Invalid chamber");
        this.magazine = magazine;
        this.reserve = reserve;
        this.transfer = transfer;
        this.chamber = chamber;
    }
    public static AmmoLedger ordinary(int count, boolean chamber) {
        RoundQueue rounds = new RoundQueue();
        rounds.addLast(false, count);
        return new AmmoLedger(rounds, new RoundQueue(), new RoundQueue(), chamber ? 1 : 0);
    }
    public int total() { return magazine.size() + reserve.size() + transfer.size() + (chamber == 0 ? 0 : 1); }
    public int preciseCount() {
        return magazine.preciseCount() + reserve.preciseCount() + transfer.preciseCount() + (chamber == 2 ? 1 : 0);
    }
    public int load(int requested, int capacity, boolean fifo, boolean free) {
        int accepted = Math.min(Math.max(0, requested), Math.max(0, capacity - magazine.size()));
        if (!free) accepted = Math.min(accepted, reserve.size());
        int backed = Math.min(accepted, reserve.size());
        RoundQueue batch = reserve.take(backed);
        batch.addLast(false, accepted - backed);
        if (fifo) magazine.append(batch); else magazine.prepend(batch);
        return requested - accepted;
    }
    public int removeForTransfer(int requested) {
        int count = Math.min(Math.max(0, requested), magazine.size());
        transfer.append(magazine.take(count));
        return count;
    }
    public void chamber(boolean present, boolean free) {
        if (!present) {
            if (chamber != 0) transfer.addLast(chamber == 2, 1);
            chamber = 0;
        } else if (chamber == 0) {
            if (!transfer.isEmpty()) chamber = transfer.removeFirst() ? 2 : 1;
            else if (!reserve.isEmpty()) chamber = reserve.removeFirst() ? 2 : 1;
            else if (!magazine.isEmpty()) chamber = magazine.removeFirst() ? 2 : 1;
            else if (free) chamber = 1;
        }
    }
    /** Shared by the HUD and non-consuming shots; open-bolt guns ignore the barrel marker. */
    public int nextRound(boolean openBolt, boolean manual) {
        if (!openBolt && chamber != 0) return chamber;
        if (manual || magazine.isEmpty()) return 0;
        return magazine.peek() ? 2 : 1;
    }
    /** Returns fired type, zero for dry fire. */
    public int fire(boolean openBolt, boolean manual) {
        if (manual) {
            int fired = chamber;
            chamber = 0;
            return fired;
        }
        int fired;
        if (!openBolt && chamber != 0) {
            fired = chamber;
            chamber = 0;
        } else {
            if (magazine.isEmpty()) return 0;
            fired = magazine.removeFirst() ? 2 : 1;
        }
        if (!openBolt && !magazine.isEmpty()) chamber = magazine.removeFirst() ? 2 : 1;
        return fired;
    }
}

