package de.steve4u.mongobee.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.steve4u.mongobee.changeset.ChangeEntry;
import de.steve4u.mongobee.exception.MongobeeChangeSetException;
import de.steve4u.mongobee.test.changelogs.AnotherMongobeeTestResource;
import de.steve4u.mongobee.test.changelogs.MongobeeTestResource;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChangeServiceTest {

  @Test
  void shouldFindChangeLogClasses() {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    // when
    List<Class<?>> foundClasses = service.fetchChangeLogs();
    // then
    assertTrue(foundClasses != null && foundClasses.size() > 0);
  }

  @Test
  void shouldFindChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 4);
  }

  @Test
  void shouldFindAnotherChangeSetMethods() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(AnotherMongobeeTestResource.class);

    // then
    assertTrue(foundMethods != null && foundMethods.size() == 4);
  }


  @Test
  void shouldFindIsRunAlwaysMethod() throws MongobeeChangeSetException {
    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when
    List<Method> foundMethods = service.fetchChangeSets(AnotherMongobeeTestResource.class);
    // then
    for (Method foundMethod : foundMethods) {
      if (foundMethod.getName().equals("testChangeSetWithAlways")) {
        assertTrue(service.isRunAlwaysChangeSet(foundMethod));
      } else {
        assertFalse(service.isRunAlwaysChangeSet(foundMethod));
      }
    }
  }

  @Test
  void shouldCreateEntry() throws MongobeeChangeSetException {

    // given
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);
    List<Method> foundMethods = service.fetchChangeSets(MongobeeTestResource.class);

    for (Method foundMethod : foundMethods) {

      // when
      ChangeEntry entry = service.createChangeEntry(foundMethod);

      // then
      assertEquals("testuser", entry.getAuthor());
      assertEquals(MongobeeTestResource.class.getName(), entry.getChangeLogClass());
      assertNotNull(entry.getTimestamp());
      assertNotNull(entry.getChangeId());
      assertNotNull(entry.getChangeSetMethodName());
    }
  }

  @Test
  void shouldFailOnDuplicatedChangeSets() {
    // given
    String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
    ChangeService service = new ChangeService(scanPackage);

    // when/then
    assertThrows(MongobeeChangeSetException.class,
        () -> service.fetchChangeSets(ChangeLogWithDuplicate.class));
  }

}
