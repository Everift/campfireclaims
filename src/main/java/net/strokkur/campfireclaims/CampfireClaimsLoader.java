package net.strokkur.campfireclaims;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public class CampfireClaimsLoader implements PluginLoader {
  @Override
  public void classloader(final PluginClasspathBuilder builder) {
    final MavenLibraryResolver resolver = new MavenLibraryResolver();
    resolver.addRepository(new RemoteRepository.Builder("paper", "default", "https://repo.papermc.io/repository/maven-public/").build());
    resolver.addRepository(new RemoteRepository.Builder("google-mirror", "default", "https://maven-central.storage-download.googleapis.com/maven2").build());
    resolver.addDependency(new Dependency(new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:" + BuildConstants.VERSION_MARIADB), null));
    resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:" + BuildConstants.VERSION_HIKARICP), null));
    resolver.addDependency(new Dependency(new DefaultArtifact("com.github.ben-manes.caffeine:caffeine:" + BuildConstants.VERSION_CAFFEINE), null));
    resolver.addDependency(new Dependency(new DefaultArtifact("org.spongepowered:configurate-hocon:" + BuildConstants.VERSION_CONFIGURATE), null));
    builder.addLibrary(resolver);
  }
}
