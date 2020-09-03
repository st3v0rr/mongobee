package de.steve4u.mongobee.dao;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;

class LockDaoTest {

  private static final String DB_NAME = "mongobeetest";
  private static final String LOCK_COLLECTION_NAME = "mongobeelock";

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  @Test
  void shouldGetLockWhenNotPreviouslyHeld() {

    // given
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(db);
    dao.releaseLock(db);

    // when
    boolean hasLock = dao.acquireLock(db);

    // then
    assertTrue(hasLock);
  }

  @Test
  void shouldNotGetLockWhenPreviouslyHeld() {

    // given
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(db);

    // when
    dao.acquireLock(db);
    boolean hasLock = dao.acquireLock(db);
    // then
    assertFalse(hasLock);

  }

  @Test
  void shouldGetLockWhenPreviouslyHeldAndReleased() {

    // given
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(db);

    // when
    dao.acquireLock(db);
    dao.releaseLock(db);
    boolean hasLock = dao.acquireLock(db);
    // then
    assertTrue(hasLock);

  }

  @Test
  void releaseLockShouldBeIdempotent() {
    // given
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);

    dao.intitializeLock(db);

    // when
    dao.releaseLock(db);
    dao.releaseLock(db);
    boolean hasLock = dao.acquireLock(db);
    // then
    assertTrue(hasLock);

  }

  @Test
  void whenLockNotHeldCheckReturnsFalse() {

    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(db);

    dao.releaseLock(db);

    assertFalse(dao.isLockHeld(db));

  }

  @Test
  void whenLockHeldCheckReturnsTrue() {

    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    LockDao dao = new LockDao(LOCK_COLLECTION_NAME);
    dao.intitializeLock(db);

    dao.acquireLock(db);

    assertTrue(dao.isLockHeld(db));

  }
}
