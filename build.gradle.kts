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
    leadingTabsToSpaces(2)
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
  maven("https://eldonexus.de/repository/maven-public/")
}

dependencies {
  // this is a brigadier instance with a module-info.java
  compileOnly("net.strokkur:brigadier:1.4.0")
  compileOnly(libs.paper.api)

  implementation(libs.mariadb.client)
  implementation(libs.caffeine)
  implementation(libs.hikaricp)
  implementation(libs.configurate.hocon)

  compileOnly(libs.commands.annotations)
  annotationProcessor(libs.commands.processor)

  compileOnly(libs.config.annotations)
  annotationProcessor(libs.config.processor)
}

sourceSets.main {
  blossom.javaSources {
    property("mariadb", libs.versions.mariadb)
    property("configurate", libs.versions.configurate)
    property("hikaricp", libs.versions.hikaricp)
    property("caffeine", libs.versions.caffeine)
  }
}

tasks {
  runServer {
    minecraftVersion(libs.versions.minecraft.get())
    jvmArgs("-Xmx2G", "-Xms2G", "-Dcom.mojang.eula.agree=true")
    downloadPlugins {
      modrinth("luckperms", "v5.5.17-bukkit")
    }
  }

  processResources {
    val version = project.version;
    filesMatching("paper-plugin.yml") {
      expand("version" to version)
    }
  }
}