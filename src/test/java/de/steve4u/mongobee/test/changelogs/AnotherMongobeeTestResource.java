package de.steve4u.mongobee.test.changelogs;

import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeLog;
import de.steve4u.mongobee.changeset.ChangeSet;

@ChangeLog(order = "2")
public class AnotherMongobeeTestResource {

  @ChangeSet(author = "testuser", id = "Btest1", order = "01")
  public void testChangeSet() {
    System.out.println("invoked B1");
  }

  @ChangeSet(author = "testuser", id = "Btest2", order = "02")
  public void testChangeSet2() {
    System.out.println("invoked B2");
  }

  @ChangeSet(author = "testuser", id = "Btest3", order = "03")
  public void testChangeSet3(MongoDatabase db) {
    System.out.println("invoked B3 with db=" + db.toString());
  }

  @ChangeSet(author = "testuser", id = "Btest4", order = "04")
  public void testChangeSet6(MongoDatabase mongoDatabase) {
    System.out.println("invoked B4 with db=" + mongoDatabase.toString());
  }

}
