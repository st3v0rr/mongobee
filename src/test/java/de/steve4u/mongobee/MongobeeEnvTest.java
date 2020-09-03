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
import de.steve4u.mongobee.test.changelogs.EnvironmentDependentTestResource;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MongobeeEnvTest {

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
    fakeMongoDatabase = MongoClients.create(settings).getDatabase("mongobeeenvtest");

    runner.setDbName("mongobeeenvtest");
    runner.setEnabled(true);
  }

  @Test
  void shouldRunChangesetWithEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(new EnvironmentMock());
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Envtest1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

  }

  @Test
  void shouldRunChangesetWithNullEnvironment() throws Exception {
    // given
    runner.setSpringEnvironment(null);
    runner.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());

    // when
    runner.execute();

    // then
    long change1 = fakeMongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
        .countDocuments(new Document()
            .append(ChangeEntry.KEY_CHANGEID, "Envtest1")
            .append(ChangeEntry.KEY_AUTHOR, "testuser"));
    assertEquals(1, change1);

  }

  @AfterEach
  public void cleanUp() {
    runner.setMongoTemplate(null);
    fakeMongoDatabase.drop();
  }

}
