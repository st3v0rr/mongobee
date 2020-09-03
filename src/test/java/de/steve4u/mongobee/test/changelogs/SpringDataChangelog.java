package de.steve4u.mongobee.test.changelogs;

import de.steve4u.mongobee.changeset.ChangeLog;
import de.steve4u.mongobee.changeset.ChangeSet;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeLog
public class SpringDataChangelog {

  @ChangeSet(author = "abelski", id = "spring_test4", order = "04")
  public void testChangeSet(MongoTemplate mongoTemplate) {
    System.out.println("invoked  with mongoTemplate=" + mongoTemplate.toString());
    System.out.println("invoked  with mongoTemplate=" + mongoTemplate.getCollectionNames());
  }
}
