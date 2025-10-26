import org.jspecify.annotations.NullMarked;

@NullMarked
module net.strokkur.campfireclaims {
  requires org.jspecify;
  requires org.bukkit;
  requires com.mojang.brigadier;
  requires net.strokkur.commands.common;
  requires net.strokkur.commands.paper;
}