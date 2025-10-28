package net.strokkur.campfireclaims.data.mariadb;

import net.strokkur.campfireclaims.data.CampfireBlock;
import net.strokkur.campfireclaims.data.CampfireUser;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

final class CampfireBlockImpl implements CampfireBlock {
  private final int id;
  private final int ownerId;
  private final Instant placementTime;
  private final int x;
  private final int y;
  private final int z;
  private final @Nullable World world;
  private int level;

  CampfireBlockImpl(int id, int ownerId, Instant placementTime, int x, int y, int z, @Nullable World world, int level) {
    this.id = id;
    this.ownerId = ownerId;
    this.placementTime = placementTime;
    this.x = x;
    this.y = y;
    this.z = z;
    this.world = world;
    this.level = level;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public int ownerId() {
    return ownerId;
  }

  @Override
  public Instant placementTime() {
    return placementTime;
  }

  @Override
  public int x() {
    return x;
  }

  @Override
  public int y() {
    return y;
  }

  @Override
  public int z() {
    return z;
  }

  @Override
  public @Nullable World world() {
    return world;
  }

  @Override
  public int level() {
    return level;
  }

  @Override
  public void level(final int lvl) {
    this.level = lvl;
  }
}
