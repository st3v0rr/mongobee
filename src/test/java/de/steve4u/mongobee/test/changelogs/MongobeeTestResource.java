package de.steve4u.mongobee.test.changelogs;

import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeLog;
import de.steve4u.mongobee.changeset.ChangeSet;

@ChangeLog(order = "1")
public class MongobeeTestResource {

  @ChangeSet(author = "testuser", id = "test1", order = "01")
  public void testChangeSet() {

    System.out.println("invoked 1");

  }

  @ChangeSet(author = "testuser", id = "test2", order = "02")
  public void testChangeSet2() {

    System.out.println("invoked 2");

  }

  @ChangeSet(author = "testuser", id = "test3", order = "03")
  public void testChangeSet3(MongoDatabase db) {

    System.out.println("invoked 3 with db=" + db.toString());

  }

  @ChangeSet(author = "testuser", id = "test4", order = "04")
  public void testChangeSet5(MongoDatabase mongoDatabase) {

    System.out.println("invoked 4 with mongoDatabase=" + mongoDatabase.toString());

  }

}
