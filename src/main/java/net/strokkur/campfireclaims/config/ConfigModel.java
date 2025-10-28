package net.strokkur.campfireclaims.config;

import net.strokkur.config.ConfigFormat;
import net.strokkur.config.GenerateConfig;
import org.jspecify.annotations.NullUnmarked;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigFormat(ConfigFormat.Format.HOCON)
@GenerateConfig
@ConfigSerializable
@NullUnmarked
class ConfigModel {

  @Comment("The database connection information. This is required for the plugin to run.")
  Database database;

  @ConfigSerializable
  static class Database {
    String ip = "127.0.0.1";
    int port = 3306;
    String database = "mariadb";
    String username = "username";
    String password = "password";
  }
}