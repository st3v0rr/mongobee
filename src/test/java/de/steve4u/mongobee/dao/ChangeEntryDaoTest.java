package de.steve4u.mongobee.dao;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.exception.MongobeeConfigurationException;
import de.steve4u.mongobee.exception.MongobeeLockException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class ChangeEntryDaoTest {

  private static final String DB_NAME = "mongobeedaotest";
  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private static final String LOCK_COLLECTION_NAME = "mongobeelock";
  private static final boolean WAIT_FOR_LOCK = false;
  private static final long CHANGE_LOG_LOCK_WAIT_TIME = 5L;
  private static final long CHANGE_LOG_LOCK_POLL_RATE = 10L;
  private static final boolean THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK = false;

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  @Test
  void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {

    // given
    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);

    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(null);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(MongoCollection.class));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(any(MongoCollection.class), any(Document.class));
  }

  @Test
  void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(true);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(0))
        .createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION_NAME));
    // and not
    verify(indexDaoMock, times(0))
        .dropIndex(db.getCollection(CHANGELOG_COLLECTION_NAME), new Document());
  }

  @Test
  void shouldRecreateChangeIdAuthorIndexIfFoundNotUnique()
      throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(false);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).dropIndex(any(MongoCollection.class), any(Document.class));
    // and
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(MongoCollection.class));
  }

  @Test
  void shouldInitiateLock() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    dao.setIndexDao(indexDaoMock);

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    // then
    verify(lockDao).intitializeLock(db);

  }

  @Test
  void shouldGetLockWhenLockDaoGetsLock() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    LockDao lockDao = mock(LockDao.class);
    when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(true);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    boolean hasLock = dao.acquireProcessLock();

    // then
    assertTrue(hasLock);
  }

  @Test
  void shouldWaitForLockIfWaitForLockIsTrue() throws Exception {
    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME, true,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    LockDao lockDao = mock(LockDao.class);
    when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(false, true);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    boolean hasLock = dao.acquireProcessLock();

    // then
    verify(lockDao, times(2)).acquireLock(any(MongoDatabase.class));
    assertTrue(hasLock);
  }

  @Test
  void shouldThrowLockExceptionIfThrowExceptionIsTrue() throws Exception {
    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE, true);

    LockDao lockDao = mock(LockDao.class);
    when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(false);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when/then
    assertThrows(MongobeeLockException.class, () -> {
      boolean hasLock = dao.acquireProcessLock();
      assertFalse(hasLock);
    });
  }

  @Test
  void shouldReleaseLockFromLockDao() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    dao.releaseProcessLock();

    // then
    verify(lockDao).releaseLock(any(MongoDatabase.class));
  }

  @Test
  void shouldCheckLockHeldFromFromLockDao() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME,
        WAIT_FOR_LOCK,
        CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE,
        THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    when(lockDao.isLockHeld(db)).thenReturn(true);

    boolean lockHeld = dao.isProccessLockHeld();

    // then
    assertTrue(lockHeld);
  }

}
