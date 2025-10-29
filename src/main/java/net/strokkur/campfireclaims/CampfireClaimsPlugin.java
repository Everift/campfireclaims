/*
 * campfireclaims  a simple claims plugin with campfires!
 * Copyright (C) 2025  Strokkur24
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.strokkur.campfireclaims;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.strokkur.campfireclaims.commands.ClaimCommandBrigadier;
import net.strokkur.campfireclaims.config.Config;
import net.strokkur.campfireclaims.config.ConfigImpl;
import net.strokkur.campfireclaims.data.CampfireRepository;
import net.strokkur.campfireclaims.data.mariadb.MariaDBCampfireRepository;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.sql.SQLException;

@SuppressWarnings("UnstableApiUsage")
public class CampfireClaimsPlugin extends JavaPlugin {
  private boolean shouldDisable = false;
  private @Nullable Config config = null;
  private @Nullable CampfireRepository repository = null;

  @Override
  public void onLoad() {
    try {
      this.config = new ConfigImpl();
      this.config.reload(this);
      this.repository = new MariaDBCampfireRepository(this.config);
    } catch (IOException | SQLException exception) {
      this.getSLF4JLogger().error("Failed to load: ", exception);
      shouldDisable = true;
    }
  }

  @Override
  public void onEnable() {
    if (shouldDisable) {
      this.getServer().getPluginManager().disablePlugin(this);
      return;
    }

    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(
        event -> ClaimCommandBrigadier.register(event.registrar(), this)
    ));
  }
}
