package net.strokkur.campfireclaims.data;

import org.bukkit.Location;

import java.sql.SQLException;
import java.util.UUID;

public interface CampfireRepository {

  CampfireUser loadUser(UUID uuid, String username) throws SQLException;

  CampfireUser getUser(UUID uuid) throws SQLException;

  CampfireBlock createBlock(Location blockPosition, CampfireUser owner);

  void removeBlock(CampfireBlock block) throws SQLException;

  void saveBlock(CampfireBlock block) throws SQLException;
}
