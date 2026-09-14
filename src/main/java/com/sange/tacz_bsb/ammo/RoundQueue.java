package com.sange.tacz_bsb.ammo;

import java.util.ArrayDeque;
import java.util.List;

/** Runs in firing order: upper bits store count, low two bits store the ammunition tier. */
public final class RoundQueue {
    private final ArrayDeque<Integer> runs = new ArrayDeque<>();
    private int size;
    public static int type(int run) { return run & 3; }
    public static int count(int run) { return run >>> 2; }
    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public int peek() {
        if (isEmpty()) throw new IllegalStateException("Empty ammunition queue");
        return type(runs.getFirst());
    }
    public void addLast(int tier, int count) {
        if (tier < 1 || tier > 3 || count < 0 || count > 1_000_000 - size)
            throw new IllegalArgumentException("Invalid ammunition tier or count");
        if (count == 0) return;
        int merged = count;
        if (!runs.isEmpty() && type(runs.getLast()) == tier) merged += count(runs.removeLast());
        runs.addLast((merged << 2) | tier);
        size += count;
    }
    public int removeFirst() {
        int tier = peek();
        int run = runs.removeFirst();
        if (count(run) > 1) runs.addFirst(run - 4);
        size--;
        return tier;
    }
    public RoundQueue take(int count) {
        if (count < 0 || count > size) throw new IllegalArgumentException("Not enough rounds");
        RoundQueue result = new RoundQueue();
        while (count > 0) {
            int run = runs.removeFirst();
            int moved = Math.min(count, count(run));
            result.addLast(type(run), moved);
            if (moved < count(run)) runs.addFirst(run - (moved << 2));
            count -= moved;
            size -= moved;
        }
        return result;
    }
    public void append(RoundQueue other) {
        if (other == this) throw new IllegalArgumentException("Cannot append queue to itself");
        for (int run : other.runs) addLast(type(run), count(run));
    }
    public void prepend(RoundQueue other) {
        RoundQueue merged = new RoundQueue();
        merged.append(other);
        merged.append(this);
        runs.clear();
        runs.addAll(merged.runs);
        size = merged.size;
    }
    public List<Integer> runs() { return List.copyOf(runs); }
    public int countTier(int tier) {
        return runs.stream().filter(n -> type(n) == tier).mapToInt(RoundQueue::count).sum();
    }
    public static RoundQueue fromRuns(List<Integer> encoded) {
        RoundQueue result = new RoundQueue();
        for (int n : encoded) {
            if (n <= 0 || count(n) == 0) throw new IllegalArgumentException("Invalid ammunition run");
            result.addLast(type(n), count(n));
        }
        return result;
    }
}
