package com.sange.tacz_bsb.ammo;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class AmmoLedgerTest {
    @Test void nextRoundUsesTheWeaponFeedMechanism() {
        var state = AmmoLedger.ordinary(0, true);
        state.magazine.addLast(2, 1);
        assertEquals(2, state.nextRound(true, false), "RPG must ignore the ordinary barrel marker");
        assertEquals(1, state.nextRound(false, false), "closed bolt still fires the chamber first");
        assertEquals(1, state.nextRound(false, true), "manual action still uses the chamber");
        state.chamber = 0;
        assertEquals(0, state.nextRound(false, true), "manual action requires chambering");
        assertEquals(2, state.nextRound(false, false));
        state.magazine.removeFirst();
        state.chamber = 1;
        assertEquals(0, state.nextRound(true, false), "barrel flag must not make an empty RPG look loaded");
    }
    @Test void chamberFiresBeforeMixedMagazine() {
        var state = AmmoLedger.ordinary(1, true);
        state.reserve.addLast(2, 3);
        assertEquals(0, state.load(3, 30, false, false));
        assertEquals(1, state.fire(false, false));
        assertEquals(2, state.chamber);
        assertEquals(2, state.fire(false, false));
        assertEquals(2, state.fire(false, false));
        assertEquals(2, state.fire(false, false));
        assertEquals(1, state.fire(false, false));
        assertEquals(0, state.fire(false, false));
    }
    @Test void tubeAppendsAndManualActionRequiresChambering() {
        var state = AmmoLedger.ordinary(2, false);
        state.reserve.addLast(2, 2);
        state.load(2, 6, true, false);
        assertEquals(0, state.fire(false, true));
        for (int expected : new int[]{1, 1, 2, 2}) {
            state.removeForTransfer(1);
            state.chamber(true, false);
            assertEquals(expected, state.fire(false, true));
        }
        assertEquals(0, state.total());
    }
    @Test void insufficientInventoryCannotCreateRounds() {
        var state = AmmoLedger.ordinary(0, false);
        state.reserve.addLast(2, 2);
        assertEquals(28, state.load(30, 30, false, false));
        assertEquals(2, state.total());
        assertEquals(1, state.removeForTransfer(1));
        state.chamber(true, false);
        state.chamber(true, false); // Idempotent.
        assertEquals(2, state.total());
        assertEquals(2, state.countTier(2));
    }
    @Test void encodingPreservesOrderAndBounds() {
        var q = new RoundQueue();
        q.addLast(1, 4); q.addLast(1, 5); q.addLast(2, 3);
        assertEquals(java.util.List.of(37, 14), q.runs());
        var copy = RoundQueue.fromRuns(q.runs());
        while (!q.isEmpty()) assertEquals(q.removeFirst(), copy.removeFirst());
        assertThrows(IllegalArgumentException.class, () -> RoundQueue.fromRuns(java.util.List.of(Integer.MIN_VALUE)));
    }
    @Test void randomizedTransactionsConserveAllThreeTiers() {
        for (int seed = 0; seed < 100; seed++) {
            Random random = new Random(seed);
            var state = AmmoLedger.ordinary(0, false);
            int[] expected = new int[4];
            for (int step = 0; step < 1000; step++) {
                switch (random.nextInt(7)) {
                    case 0 -> {
                        int tier = 1 + random.nextInt(3), n = random.nextInt(8);
                        state.reserve.addLast(tier, n);
                        expected[tier] += n;
                    }
                    case 1 -> state.load(random.nextInt(35), 30, random.nextBoolean(), false);
                    case 2 -> {
                        int fired = state.fire(false, random.nextBoolean());
                        if (fired > 0) expected[fired]--;
                    }
                    case 3 -> { state.removeForTransfer(random.nextInt(3)); state.chamber(true, false); }
                    case 4 -> {
                        RoundQueue returned = state.magazine.take(random.nextInt(state.magazine.size() + 1));
                        for (int tier = 1; tier <= 3; tier++) expected[tier] -= returned.countTier(tier);
                    }
                    case 5 -> state.chamber(false, false);
                    case 6 -> {
                        // Persist and decode each queue, including pending reload/bolt transfers.
                        state = new AmmoLedger(RoundQueue.fromRuns(state.magazine.runs()),
                                RoundQueue.fromRuns(state.reserve.runs()), RoundQueue.fromRuns(state.transfer.runs()), state.chamber);
                    }
                }
                for (int tier = 1; tier <= 3; tier++)
                    assertEquals(expected[tier], state.countTier(tier), "tier=" + tier + " seed=" + seed + " step=" + step);
                assertEquals(expected[1] + expected[2] + expected[3], state.total());
            }
        }
    }
    @Test void allThreeTiersRetainExactChamberAndMagazineOrder() {
        var state = AmmoLedger.ordinary(1, true);
        state.reserve.addLast(3, 2);
        state.reserve.addLast(2, 1);
        state.reserve.addLast(1, 1);
        state.reserve.addLast(3, 1);
        state.load(5, 30, false, false);
        for (int tier : new int[]{1, 3, 3, 2, 1, 3, 1}) assertEquals(tier, state.fire(false, false));
        assertEquals(0, state.total());
    }
}
