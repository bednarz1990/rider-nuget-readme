package io.github.bednarz1990.nuget.core;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** Cancellation saves work; the generation check also prevents stale EDT callbacks. */
public final class LatestRequest {
    private final AtomicLong generation = new AtomicLong();
    private Future<?> active;
    public synchronized long next() {
        generation.incrementAndGet();
        if (active != null) active.cancel(true);
        active = null;
        return generation.get();
    }
    public synchronized void attach(long ticket, Future<?> future) {
        if (current(ticket)) active = future; else future.cancel(true);
    }
    public boolean current(long ticket) { return ticket == generation.get(); }
}
