package org.geysermc.geyser.entity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.concurrent.atomic.AtomicInteger;

public record GeyserEntityIdentifier(NbtMap nbt) {
    private static final AtomicInteger RUNTIME_ID_ALLOCATOR = new AtomicInteger(100000);

    public boolean hasSpawnEgg() {
        return this.nbt.getBoolean("hasspawnegg");
    }

    @NonNull
    public String identifier() {
        return this.nbt.getString("id");
    }

    public boolean isSummonable() {
        return this.nbt.getBoolean("summonable");
    }

    public static class Builder {
        private final NbtMapBuilder nbt = NbtMap.builder();

        public Builder spawnEgg(boolean spawnEgg) {
            this.nbt.putBoolean("hasspawnegg", spawnEgg);
            return this;
        }

        public Builder identifier(String identifier) {
            this.nbt.putString("id", identifier);
            return this;
        }

        public Builder summonable(boolean summonable) {
            this.nbt.putBoolean("summonable", summonable);
            return this;
        }

        public GeyserEntityIdentifier build() {
            // Vanilla registry information
            this.nbt.putString("bid", "");
            this.nbt.putInt("rid", RUNTIME_ID_ALLOCATOR.getAndIncrement());
            this.nbt.putBoolean("experimental", false);

            return new GeyserEntityIdentifier(this.nbt.build());
        }
    }
}
