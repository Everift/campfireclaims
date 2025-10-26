import net.kyori.indra.licenser.spotless.HeaderFormat

plugins {
  id("java")
  alias(libs.plugins.spotless)
  alias(libs.plugins.blossom)
  alias(libs.plugins.run.paper)
}

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

spotless {
  java {
    removeUnusedImports()
    forbidWildcardImports()
  }
}

indraSpotlessLicenser {
  licenseHeaderFile(rootDir.resolve("HEADER"))
  headerFormat(HeaderFormat.starSlash())
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://eldonexus.de/repository/maven-releases/")
  maven("https://eldonexus.de/repository/maven-snapshots/")
}

dependencies {
  // this is a brigadier instance with a module-info.java
  compileOnly("net.strokkur:brigadier:1.4.0")

  compileOnly(libs.paper.api)
  compileOnly(libs.commands.annotations)
  annotationProcessor(libs.commands.processor)
}

tasks {
  runServer {
    minecraftVersion(libs.versions.minecraft.get())
  }
}