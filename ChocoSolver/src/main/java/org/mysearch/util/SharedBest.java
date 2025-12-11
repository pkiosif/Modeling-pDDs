package org.mysearch.util;

public final class SharedBest {
    private final java.util.concurrent.atomic.AtomicInteger best = new java.util.concurrent.atomic.AtomicInteger(0);
    public int get() { return best.get(); }
    /** Monotone raise; returns true if it increased */
    public boolean raiseTo(int v) {
        int x;
        while ((x = best.get()) < v) {
            if (best.compareAndSet(x, v)) return true;
        }
        return false;
    }
}

