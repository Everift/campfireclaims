package net.strokkur.campfireclaims.data.mariadb;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import net.strokkur.campfireclaims.config.Config;
import net.strokkur.campfireclaims.data.CampfireBlock;
import net.strokkur.campfireclaims.data.CampfireRepository;
import net.strokkur.campfireclaims.data.CampfireUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;
import org.mariadb.jdbc.MariaDbDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class MariaDBCampfireRepository implements CampfireRepository {
  private static final Logger LOGGER = LoggerFactory.getLogger("CampfireClaims/MariaDBCampfireRepository");

  private final HikariPool pool;
  private final Cache<UUID, CampfireUser> userCache = Caffeine.newBuilder()
      .expireAfterAccess(10, TimeUnit.DAYS)
      .removalListener((key, value, reason) -> {
        try {
          if (value instanceof CampfireUser user) {
            saveUser(user);
          }
        } catch (SQLException sqlException) {
          LOGGER.error("Failed to save for uuid '{}'", key, sqlException);
        }
      })
      .build();
  private final Map<Integer, CampfireBlock> loadedBlocks = new WeakHashMap<>();
  private final BiMap<Integer, World> loadedWorlds = HashBiMap.create();

  public MariaDBCampfireRepository(final Config config) throws SQLException {
    final MariaDbDataSource dataSource = new MariaDbDataSource();
    dataSource.setUser(config.database().username());
    dataSource.setPassword(config.database().password());
    dataSource.setUrl("jdbc:mariadb://%s:%s/%s".formatted(
        config.database().ip(),
        config.database().port(),
        config.database().database()
    ));

    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDataSource(dataSource);
    this.pool = new HikariPool(hikariConfig);

    // Init tables
    try (final Connection conn = this.pool.getConnection()) {
      final Statement statement = conn.createStatement();

      statement.addBatch("""
          CREATE TABLE IF NOT EXISTS campfireclaims_users(
              user_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
              user_name VARCHAR(16) NOT NULL,
              user_uuid UUID NOT NULL UNIQUE
          );""");
      statement.addBatch("""
          CREATE TABLE IF NOT EXISTS campfireclaims_blocks(
              block_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
              owner_id INT NOT NULL,
              placement_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              world_id INT NOT NULL,
              pos_x INT NOT NULL,
              pos_y INT NOT NULL,
              pos_z INT NOT NULL,
              claim_level INT NOT NULL,
              active BIT NOT NULL DEFAULT TRUE
          );""");
      statement.addBatch("""
          CREATE TABLE IF NOT EXISTS campfireclaims_worlds(
              world_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
              world_uuid UUID NOT NULL
          );""");
      statement.addBatch("""
              CREATE TABLE IF NOT EXISTS campfireclaims_trusted(
              trust_id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
              user_id INT NOT NULL,
              block_id INT NOT NULL,
              active BIT NOT NULL DEFAULT TRUE
          );""");
      statement.executeBatch();
    }
  }

  @Override
  public CampfireUser loadUser(final UUID uuid, final String username) throws SQLException {
    final CampfireUser cached = userCache.getIfPresent(uuid);
    if (cached != null) {
      return cached;
    }

    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement prepared = conn.prepareStatement("SELECT (user_id, user_name) FROM campfireclaims_users WHERE user_uuid = ?");
      prepared.setObject(1, uuid);
      final ResultSet userSet = prepared.executeQuery();

      if (userSet.next()) {
        final int userId = userSet.getInt("user_id");
        final String userName = userSet.getString("user_name");
        return createAndCache(conn, userId, userName, uuid);
      } else {
        final PreparedStatement insert = conn.prepareStatement(
            "INSERT INTO campfireclaims_users (user_name, user_uuid) VALUES (?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS
        );
        insert.setString(1, username);
        insert.setObject(2, uuid);
        insert.executeUpdate();

        final ResultSet result = insert.getResultSet();
        final int userId = result.getInt("user_id");
        return createAndCache(conn, userId, username, uuid);
      }
    }
  }

  @Override
  public CampfireUser getUser(final UUID uuid) throws SQLException {
    final CampfireUser cached = userCache.getIfPresent(uuid);
    if (cached != null) {
      return cached;
    }

    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement prepared = conn.prepareStatement("SELECT (user_id, user_name) FROM campfireclaims_users WHERE user_uuid = ?");
      prepared.setObject(1, uuid);
      final ResultSet userSet = prepared.executeQuery();

      if (userSet.next()) {
        final int userId = userSet.getInt("user_id");
        final String userName = userSet.getString("user_name");
        return createAndCache(conn, userId, userName, uuid);
      } else {
        throw new IllegalStateException("No mapping found for for uuid '" + uuid + "'. Use #loadUser instead if you expect the entry to not exist.");
      }
    }
  }

  private CampfireUser createAndCache(final Connection conn, final int userId, final String userName,
                                      final UUID userUuid) throws SQLException {
    final List<CampfireBlock> ownedBlocks = getOwnedBlocks(conn, userId);
    final List<CampfireBlock> trustedBlocks = getTrustedBlocks(conn, userId);

    final CampfireUser user = new CampfireUserImpl(
        userId,
        userName,
        userUuid,
        ownedBlocks,
        trustedBlocks
    );
    this.userCache.put(userUuid, user);
    return user;
  }

  private List<CampfireBlock> getOwnedBlocks(final Connection conn, final int user) throws SQLException {
    final PreparedStatement prepared = conn.prepareStatement("SELECT (block_id) FROM campfireclaims_trusted WHERE user_id = ? AND active = TRUE");
    return getCampfireBlocksFromSet(conn, user, prepared);
  }

  private List<CampfireBlock> getTrustedBlocks(final Connection conn, final int user) throws SQLException {
    final PreparedStatement prepared = conn.prepareStatement("SELECT (block_id) FROM campfireclaims_blocks WHERE owner_id = ? AND active = TRUE");
    return getCampfireBlocksFromSet(conn, user, prepared);
  }

  private List<CampfireBlock> getCampfireBlocksFromSet(final Connection conn, final int user, final PreparedStatement prepared) throws SQLException {
    prepared.setInt(1, user);

    final ResultSet set = prepared.executeQuery();
    final List<CampfireBlock> out = new LinkedList<>();
    while (set.next()) {
      out.add(getCampfireBlock(conn, set.getInt("block_id"), user));
    }
    return out;
  }

  private CampfireBlock getCampfireBlock(final Connection conn, final int blockId, final int owner) throws SQLException {
    final CampfireBlock cached = this.loadedBlocks.get(blockId);
    if (cached != null) {
      return cached;
    }

    final PreparedStatement prepared = conn.prepareStatement("""
        SELECT (placement_timestamp, world_id, pos_x, pos_y, pos_z, claim_level)
        FROM campfireclaims_blocks
        WHERE block_id = $1""");
    prepared.setInt(1, blockId);

    final ResultSet set = prepared.executeQuery();
    if (set.next()) {
      final CampfireBlock block = new CampfireBlockImpl(
          blockId,
          owner,
          set.getTimestamp("placement_timestamp").toInstant(),
          set.getInt("pos_x"),
          set.getInt("pos_y"),
          set.getInt("pos_z"),
          getWorldFromId(conn, set.getInt("world_id")),
          set.getInt("claim_level")
      );
      this.loadedBlocks.put(block.id(), block);
      return block;
    } else {
      throw new SQLException("No block with blockId of '" + blockId + "' found.");
    }
  }

  private @Nullable World getWorldFromId(final Connection conn, final int worldId) throws SQLException {
    if (this.loadedWorlds.containsKey(worldId)) {
      return this.loadedWorlds.get(worldId);
    }

    final PreparedStatement prepared = conn.prepareStatement("SELECT (world_uuid) FROM campfireclaims_worlds WHERE world_id = ?");
    prepared.setInt(1, worldId);
    final ResultSet set = prepared.executeQuery();
    if (set.next()) {
      final UUID uuid = set.getObject("world_uuid", UUID.class);
      final World world = Bukkit.getWorld(uuid);
      this.loadedWorlds.put(worldId, world);
      return world;
    } else {
      return null;
    }
  }

  private void saveUser(final CampfireUser user) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement updateUsers = conn.prepareStatement("UPDATE campfireclaims_users SET user_username = ? WHERE user_id = ?;");
      updateUsers.setString(1, user.username());
      updateUsers.setInt(2, user.id());
      updateUsers.executeUpdate();
    }
  }

  @Override
  public CampfireBlock createBlock(final Location blockPosition, final CampfireUser owner) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final int worldId = getOrInsertWorld(conn, blockPosition.getWorld());

      final PreparedStatement prepared = conn.prepareStatement("""
              INSERT INTO campfireclaims_blocks (owner_id, world_id, pos_x, pos_y, pos_z, claim_level)
              VALUES (?, ?, ?, ?, ?, ?);""",
          PreparedStatement.RETURN_GENERATED_KEYS
      );
      prepared.setInt(1, owner.id());
      prepared.setInt(2, worldId);
      prepared.setInt(3, blockPosition.getBlockX());
      prepared.setInt(4, blockPosition.getBlockY());
      prepared.setInt(5, blockPosition.getBlockZ());
      prepared.setInt(6, 1);

      prepared.executeUpdate();
      final ResultSet insertResult = prepared.getResultSet();
      if (insertResult.next()) {
        final CampfireBlock out = new CampfireBlockImpl(
            insertResult.getInt("block_id"),
            owner.id(),
            insertResult.getTimestamp("placement_timestamp").toInstant(),
            blockPosition.getBlockX(),
            blockPosition.getBlockY(),
            blockPosition.getBlockZ(),
            blockPosition.getWorld(),
            1
        );
        ((CampfireUserImpl) owner).owned().add(out);
        return out;
      }

      throw new SQLException("Failed to create new campfire block for (%s x=%s y=%s z=%s)".formatted(
          blockPosition.getWorld().getName(), blockPosition.getBlockX(), blockPosition.getBlockY(), blockPosition.getBlockZ()
      ));
    }
  }

  private int getOrInsertWorld(final Connection conn, final World world) throws SQLException {
    final Integer cached = this.loadedWorlds.inverse().get(world);
    if (cached != null) {
      return cached;
    }

    final PreparedStatement preparedStatement = conn.prepareStatement("SELECT (world_id) FROM campfireclaims_worlds WHERE world_uuid = ?;");
    preparedStatement.setObject(1, world.getUID());
    final ResultSet set = preparedStatement.executeQuery();
    if (set.next()) {
      return set.getInt("world_id");
    }

    final PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO campfireclaims_world (world_uuid) VALUES (?);", PreparedStatement.RETURN_GENERATED_KEYS);
    insertStatement.setObject(1, world.getUID());
    insertStatement.executeUpdate();
    final ResultSet insertSet = insertStatement.getResultSet();
    if (!insertSet.next()) {
      throw new IllegalStateException("Failed to insert world '" + world.getUID() + "'");
    }
    return insertSet.getInt("world_id");
  }

  @Override
  public void saveBlock(final CampfireBlock block) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement prepared = conn.prepareStatement("UPDATE campfireclaims_blocks SET claim_level = ? WHERE block_id = ?;");
      prepared.setInt(1, block.level());
      prepared.setInt(2, block.id());
      prepared.executeUpdate();
    }
  }

  @Override
  public void removeBlock(final CampfireUser owner, final CampfireBlock block) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement blocksStatement = conn.prepareStatement("UPDATE campfireclaims_blocks SET active = FALSE WHERE block_id = ?;");
      blocksStatement.setInt(1, block.id());
      blocksStatement.executeUpdate();
      ((CampfireUserImpl) owner).owned().remove(block);

      final PreparedStatement trustedStatement = conn.prepareStatement("UPDATE campfireclaims_trusted SET active = FALSE WHERE block_id = ?;");
      trustedStatement.setInt(1, block.id());
      trustedStatement.executeUpdate();
      for (final CampfireUser user : this.userCache.asMap().values()) {
        ((CampfireUserImpl) user).trusted().removeIf(b -> b.id() == block.id());
      }
    }
  }

  @Override
  public void addTrusted(final CampfireBlock block, final CampfireUser user) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement insertStatement = conn.prepareStatement("""
          INSERT INTO campfireclaims_trusted (user_id, block_id)
          VALUES (?, ?)""");
      insertStatement.setInt(1, user.id());
      insertStatement.setInt(2, block.id());
      insertStatement.executeUpdate();
      ((CampfireUserImpl) user).trusted().add(block);
    }
  }

  @Override
  public void removeTrusted(final CampfireBlock block, final CampfireUser user) throws SQLException {
    try (final Connection conn = this.pool.getConnection()) {
      final PreparedStatement statement = conn.prepareStatement("UPDATE campfireclaims_trusted SET active = FALSE WHERE block_id = ?;");
      statement.setInt(1, block.id());
      ((CampfireUserImpl) user).trusted().removeIf(b -> b.id() == block.id());
    }
  }
}
