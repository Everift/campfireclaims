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
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class CampfireClaimsPlugin extends JavaPlugin {
  @Override
  public void onEnable() {
    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS.newHandler(
        event -> ClaimCommandBrigadier.register(event.registrar(), this)
    ));
  }
}
