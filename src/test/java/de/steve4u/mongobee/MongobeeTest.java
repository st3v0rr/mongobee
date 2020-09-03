package de.steve4u.mongobee;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeEntry;
import de.steve4u.mongobee.exception.MongobeeConfigurationException;
import de.steve4u.mongobee.test.changelogs.MongobeeTestResource;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongobeeTest {

  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private final Mongobee runner = new Mongobee();

  private MongoDatabase fakeMongoDatabase;

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  @BeforeEach
  void init() {
    fakeMongoDatabase = MongoClients.create(settings).getDatabase("mongobeetest");

    runner.setDbName("mongobeetest");
    runner.setEnabled(true);
    runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
  }

  @Test
  void shouldThrowAnExceptionIfNoDbNameSet() {
    // given
    Mongobee failingRunner = new Mongobee();
    failingRunner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());

    // when/then
    assertThrows(MongobeeConfigurationException.class, failingRunner::execute);
  }

  @Test
  void shouldExecuteAllChangeSets() throws Exception {
    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "test1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);
    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "test2")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);
    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "test3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change3);
    long change4 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "test4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change4);

    long changeAll = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(9, changeAll);
  }

  @AfterEach
  public void cleanUp() {
    fakeMongoDatabase.drop();
  }

}
