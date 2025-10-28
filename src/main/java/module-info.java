import org.jspecify.annotations.NullMarked;

@NullMarked
module net.strokkur.campfireclaims {
  requires org.jspecify;
  requires org.bukkit;
  requires com.mojang.brigadier;
  requires net.strokkur.commands.common;
  requires net.strokkur.commands.paper;
  requires net.strokkur.config.annotations;
  requires org.apache.maven.resolver;
  requires it.unimi.dsi.fastutil;
  requires org.spongepowered.configurate;
  requires org.spongepowered.configurate.hocon;
  requires org.jetbrains.annotations;
  requires com.zaxxer.hikari;
  requires java.sql;
  requires org.mariadb.jdbc;
  requires com.github.benmanes.caffeine;
  requires net.strokkur.campfireclaims;
  requires org.slf4j;
}