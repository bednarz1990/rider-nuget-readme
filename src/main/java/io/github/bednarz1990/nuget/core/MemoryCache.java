package io.github.bednarz1990.nuget.core;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Project-local bounded LRU. Errors are never inserted; missing readmes expire too. */
public final class MemoryCache<K, V> {
    private record Entry<V>(V value, long expires) {}
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>(16, .75f, true);
    private final int capacity;
    private final long ttl;
    private final Clock clock;
    public MemoryCache(int capacity, Duration ttl, Clock clock) {
        this.capacity = capacity; this.ttl = ttl.toMillis(); this.clock = clock;
    }
    public synchronized V get(K key) {
        Entry<V> entry = entries.get(key);
        if (entry == null) return null;
        if (clock.millis() >= entry.expires()) { entries.remove(key); return null; }
        return entry.value();
    }
    public synchronized void put(K key, V value) {
        entries.put(key, new Entry<>(value, clock.millis() + ttl));
        while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
    }
    public synchronized void clear() { entries.clear(); }
}
