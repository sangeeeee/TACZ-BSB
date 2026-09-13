package com.sange.tacz_bsb.ammo;

import java.util.ArrayDeque;
import java.util.List;

/** Run-length encoded rounds in firing order. Positive runs are precise. */
public final class RoundQueue {
    private final ArrayDeque<Integer> runs = new ArrayDeque<>();
    private int size;

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }
    public boolean peek() {
        if (isEmpty()) throw new IllegalStateException("Empty ammunition queue");
        return runs.getFirst() > 0;
    }
    public void addLast(boolean precise, int count) {
        if (count < 0 || count > 1_000_000 - size) throw new IllegalArgumentException("Invalid ammunition count");
        if (count == 0) return;
        int run = precise ? count : -count;
        if (!runs.isEmpty() && (runs.getLast() > 0) == precise) run += runs.removeLast();
        runs.addLast(run);
        size += count;
    }
    public boolean removeFirst() {
        boolean precise = peek();
        int run = runs.removeFirst();
        if (Math.abs(run) > 1) runs.addFirst(run + (precise ? -1 : 1));
        size--;
        return precise;
    }
    public RoundQueue take(int count) {
        if (count < 0 || count > size) throw new IllegalArgumentException("Not enough rounds");
        RoundQueue result = new RoundQueue();
        while (count > 0) {
            int run = runs.removeFirst();
            int moved = Math.min(count, Math.abs(run));
            result.addLast(run > 0, moved);
            if (moved < Math.abs(run)) runs.addFirst(run > 0 ? run - moved : run + moved);
            count -= moved;
            size -= moved;
        }
        return result;
    }
    public void append(RoundQueue other) {
        for (int run : other.runs) addLast(run > 0, Math.abs(run));
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
    public int preciseCount() { return runs.stream().filter(n -> n > 0).mapToInt(Integer::intValue).sum(); }
    public static RoundQueue fromRuns(List<Integer> encoded) {
        RoundQueue result = new RoundQueue();
        for (int n : encoded) {
            if (n == 0 || n == Integer.MIN_VALUE) throw new IllegalArgumentException("Invalid run");
            result.addLast(n > 0, Math.abs(n));
        }
        return result;
    }
}

