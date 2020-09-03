package de.steve4u.mongobee;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeEntry;
import de.steve4u.mongobee.resources.EnvironmentMock;
import de.steve4u.mongobee.test.changelogs.AnotherMongobeeTestResource;
import de.steve4u.mongobee.test.profiles.def.UnProfiledChangeLog;
import de.steve4u.mongobee.test.profiles.dev.ProfiledDevChangeLog;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongobeeProfileTest {

  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  public static final int CHANGELOG_COUNT = 10;

  private final Mongobee runner = new Mongobee();

  private MongoDatabase fakeMongoDatabase;

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  @BeforeEach
  void init() throws Exception {
    fakeMongoDatabase = MongoClients.create(settings).getDatabase("mongobeeprofiletest");

    runner.setDbName("mongobeeprofiletest");
    runner.setEnabled(true);
  }

  @Test
  void shouldRunDevProfileAndNonAnnotated() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev", "test"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);  //  no-@Profile  should not match

    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);  //  @Profile("dev")  should not match

    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(0, change3);  //  @Profile("default")  should not match
  }

  @Test
  void shouldRunUnprofiledChangeLog() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("test"));
    runner.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

    long change2 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev2")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change2);

    long change3 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev3")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change3);  //  @Profile("dev")  should not match

    long change4 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev4")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(0, change4);  //  @Profile("pro")  should not match

    long change5 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Pdev5")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change5);  //  @Profile("!pro")  should match
  }

  @Test
  void shouldNotRunAnyChangeSet() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("foobar"));
    runner.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document());
    assertEquals(0, changes);
  }

  @Test
  void shouldRunChangeSetsWhenNoEnv() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @Test
  void shouldRunAllChangeSets() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock("dev"));
    runner.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long changes = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document());
    assertEquals(CHANGELOG_COUNT, changes);
  }

  @AfterEach
  public void cleanUp() {
    runner.setMongoTemplate(null);
    fakeMongoDatabase.drop();
  }

}
