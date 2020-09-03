package de.steve4u.mongobee.utils;

import de.steve4u.mongobee.changeset.ChangeLog;
import de.steve4u.mongobee.changeset.ChangeSet;

@ChangeLog
public class ChangeLogWithDuplicate {

  @ChangeSet(author = "testuser", id = "Btest1", order = "01")
  public void testChangeSet() {
    System.out.println("invoked B1");
  }

  @ChangeSet(author = "testuser", id = "Btest2", order = "02")
  public void testChangeSet2() {
    System.out.println("invoked B2");
  }

  @ChangeSet(author = "testuser", id = "Btest2", order = "03")
  public void testChangeSet3() {
    System.out.println("invoked B3");
  }
}
