package de.steve4u.mongobee.dao;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeEntry;
import org.bson.Document;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ChangeEntryIndexDaoTest {

  private static final String DB_NAME = "mongobeetest";
  private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private static final String CHANGEID_AUTHOR_INDEX_NAME = "changeId_1_author_1";

  private final ChangeEntryIndexDao dao = new ChangeEntryIndexDao(CHANGELOG_COLLECTION_NAME);

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  @Test
  void shouldCreateRequiredUniqueIndex() {
    // given
    MongoClient mongo = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongo.getDatabase(Mockito.anyString())).thenReturn(db);

    // when
    dao.createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION_NAME));

    // then
    Document createdIndex = findIndex(db);
    assertNotNull(createdIndex);
    assertTrue(dao.isUnique(createdIndex));
  }

  @Test
  @Disabled("Fongo has not implemented dropIndex for MongoCollection object (issue with mongo driver 3.x)")
  void shouldDropWrongIndex() {
    // init
    MongoClient mongo = mock(MongoClient.class);
    MongoDatabase db = MongoClients.create(settings).getDatabase(DB_NAME);
    when(mongo.getDatabase(Mockito.anyString())).thenReturn(db);

    MongoCollection<Document> collection = db.getCollection(CHANGELOG_COLLECTION_NAME);
    collection.createIndex(new Document()
        .append(ChangeEntry.KEY_CHANGEID, 1)
        .append(ChangeEntry.KEY_AUTHOR, 1));
    Document index = new Document("name", CHANGEID_AUTHOR_INDEX_NAME);

    // given
    Document createdIndex = findIndex(db);
    assertNotNull(createdIndex);
    assertFalse(dao.isUnique(createdIndex));

    // when
    dao.dropIndex(db.getCollection(CHANGELOG_COLLECTION_NAME), index);

    // then
    assertNull(findIndex(db));
  }

  private Document findIndex(MongoDatabase db) {

    for (Document index : db.getCollection(CHANGELOG_COLLECTION_NAME).listIndexes()) {
      String name = (String) index.get("name");
      if (ChangeEntryIndexDaoTest.CHANGEID_AUTHOR_INDEX_NAME.equals(name)) {
        return index;
      }
    }
    return null;
  }

}
