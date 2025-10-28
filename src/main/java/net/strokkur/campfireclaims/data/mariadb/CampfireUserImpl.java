package net.strokkur.campfireclaims.data.mariadb;

import net.strokkur.campfireclaims.data.CampfireBlock;
import net.strokkur.campfireclaims.data.CampfireUser;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

record CampfireUserImpl(
    int id,
    String username,
    UUID uuid,
    List<CampfireBlock> owned,
    List<CampfireBlock> trusted
) implements CampfireUser {

  @Override
  public @UnmodifiableView List<CampfireBlock> ownedBlocks() {
    return Collections.unmodifiableList(this.owned);
  }

  @Override
  public @UnmodifiableView List<CampfireBlock> trustedBlocks() {
    return Collections.unmodifiableList(this.trusted);
  }
}
