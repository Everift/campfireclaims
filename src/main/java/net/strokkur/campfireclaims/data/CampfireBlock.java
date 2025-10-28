package net.strokkur.campfireclaims.data;

import org.bukkit.World;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public interface CampfireBlock {

  int id();

  int ownerId();

  Instant placementTime();

  int x();

  int y();

  int z();

  @Nullable
  World world();

  default int chunkX() {
    return x() << 16;
  }

  default int chunkZ() {
    return z() << 16;
  }

  int level();

  void level(int lvl);

  default void increaseLevel() {
    level(level() + 1);
  }
}
