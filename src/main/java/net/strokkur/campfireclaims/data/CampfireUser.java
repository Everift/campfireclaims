package net.strokkur.campfireclaims.data;

import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.UUID;

public interface CampfireUser {

  int id();

  String username();

  UUID uuid();

  @UnmodifiableView
  List<CampfireBlock> ownedBlocks();

  @UnmodifiableView
  List<CampfireBlock> trustedBlocks();
}
