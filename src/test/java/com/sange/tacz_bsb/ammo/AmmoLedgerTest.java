package com.sange.tacz_bsb.ammo;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class AmmoLedgerTest {
    @Test void chamberFiresBeforeMixedMagazine() {
        var state = AmmoLedger.ordinary(1, true);
        state.reserve.addLast(true, 3);
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
        state.reserve.addLast(true, 2);
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
        state.reserve.addLast(true, 2);
        assertEquals(28, state.load(30, 30, false, false));
        assertEquals(2, state.total());
        assertEquals(1, state.removeForTransfer(1));
        state.chamber(true, false);
        state.chamber(true, false); // Idempotent.
        assertEquals(2, state.total());
        assertEquals(2, state.preciseCount());
    }
    @Test void encodingPreservesOrderAndBounds() {
        var q = new RoundQueue();
        q.addLast(false, 4); q.addLast(false, 5); q.addLast(true, 3);
        assertEquals(java.util.List.of(-9, 3), q.runs());
        var copy = RoundQueue.fromRuns(q.runs());
        while (!q.isEmpty()) assertEquals(q.removeFirst(), copy.removeFirst());
        assertThrows(IllegalArgumentException.class, () -> RoundQueue.fromRuns(java.util.List.of(Integer.MIN_VALUE)));
    }
    @Test void randomizedTransactionsConserveBothQualities() {
        for (int seed = 0; seed < 100; seed++) {
            Random random = new Random(seed);
            var state = AmmoLedger.ordinary(0, false);
            int ordinary = 0, precise = 0;
            for (int step = 0; step < 1000; step++) {
                switch (random.nextInt(6)) {
                    case 0 -> {
                        boolean quality = random.nextBoolean();
                        int n = random.nextInt(8);
                        state.reserve.addLast(quality, n);
                        if (quality) precise += n; else ordinary += n;
                    }
                    case 1 -> state.load(random.nextInt(35), 30, random.nextBoolean(), false);
                    case 2 -> {
                        int fired = state.fire(false, random.nextBoolean());
                        if (fired == 2) precise--; else if (fired == 1) ordinary--;
                    }
                    case 3 -> { state.removeForTransfer(random.nextInt(3)); state.chamber(true, false); }
                    case 4 -> {
                        RoundQueue returned = state.magazine.take(random.nextInt(state.magazine.size() + 1));
                        precise -= returned.preciseCount();
                        ordinary -= returned.size() - returned.preciseCount();
                    }
                    case 5 -> state.chamber(false, false);
                }
                assertEquals(precise, state.preciseCount(), "precise, seed=" + seed + " step=" + step);
                assertEquals(ordinary + precise, state.total(), "total, seed=" + seed + " step=" + step);
            }
        }
    }
}

