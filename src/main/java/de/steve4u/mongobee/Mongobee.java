package de.steve4u.mongobee;

import static com.mongodb.ServerAddress.defaultHost;
import static com.mongodb.ServerAddress.defaultPort;
import static org.springframework.util.StringUtils.hasText;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.steve4u.mongobee.changeset.ChangeEntry;
import de.steve4u.mongobee.dao.ChangeEntryDao;
import de.steve4u.mongobee.exception.MongobeeChangeSetException;
import de.steve4u.mongobee.exception.MongobeeConfigurationException;
import de.steve4u.mongobee.exception.MongobeeConnectionException;
import de.steve4u.mongobee.exception.MongobeeException;
import de.steve4u.mongobee.utils.ChangeService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Mongobee runner
 */
public class Mongobee implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(Mongobee.class);

  private static final String DEFAULT_CHANGELOG_COLLECTION_NAME = "dbchangelog";
  private static final String DEFAULT_LOCK_COLLECTION_NAME = "mongobeelock";
  private static final boolean DEFAULT_WAIT_FOR_LOCK = false;
  private static final long DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME = 5L;
  private static final long DEFAULT_CHANGE_LOG_LOCK_POLL_RATE = 10L;
  private static final boolean DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK = false;

  private final ChangeEntryDao dao;

  private boolean enabled = true;
  private String changeLogsScanPackage;
  private final MongoClient mongoClient;
  private String dbName;
  private Environment springEnvironment;

  private MongoTemplate mongoTemplate;

  private static final ConnectionString connString = new ConnectionString(
      "mongodb://" + defaultHost() + ":" + defaultPort() + "/");
  private static final MongoClientSettings settings = MongoClientSettings.builder()
      .applyConnectionString(connString)
      .retryWrites(true)
      .build();

  public Mongobee() {
    this(MongoClients.create(settings));
  }

  /**
   * <p>Constructor takes db.mongodb.MongoClient object as a parameter.
   * </p><p>For more details about <tt>MongoClient</tt> please see com.mongodb.MongoClient docs
   * </p>
   *
   * @param mongoClient database connection client
   * @see MongoClient
   */
  public Mongobee(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
    this.dao = new ChangeEntryDao(DEFAULT_CHANGELOG_COLLECTION_NAME, DEFAULT_LOCK_COLLECTION_NAME,
        DEFAULT_WAIT_FOR_LOCK,
        DEFAULT_CHANGE_LOG_LOCK_WAIT_TIME, DEFAULT_CHANGE_LOG_LOCK_POLL_RATE,
        DEFAULT_THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
  }

  /**
   * For Spring users: executing mongobee after bean is created in the Spring context
   *
   * @throws Exception exception
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    execute();
  }

  /**
   * Executing migration
   *
   * @throws MongobeeException exception
   */
  public void execute() throws MongobeeException {
    if (!isEnabled()) {
      logger.info("Mongobee is disabled. Exiting.");
      return;
    }

    validateConfig();

    dao.connectMongoDb(this.mongoClient, dbName);

    if (!dao.acquireProcessLock()) {
      logger.info("Mongobee did not acquire process lock. Exiting.");
      return;
    }

    logger.info("Mongobee acquired process lock, starting the data migration sequence..");

    try {
      executeMigration();
    } finally {
      logger.info("Mongobee is releasing process lock.");
      dao.releaseProcessLock();
    }

    logger.info("Mongobee has finished his job.");
  }

  private void executeMigration() throws MongobeeException {

    ChangeService service = new ChangeService(changeLogsScanPackage, springEnvironment);

    for (Class<?> changelogClass : service.fetchChangeLogs()) {

      Object changelogInstance = null;
      try {
        changelogInstance = changelogClass.getConstructor().newInstance();
        List<Method> changesetMethods = service.fetchChangeSets(changelogInstance.getClass());

        for (Method changesetMethod : changesetMethods) {
          ChangeEntry changeEntry = service.createChangeEntry(changesetMethod);

          executeMigrationForChangeEntry(service, changelogInstance, changesetMethod, changeEntry);
        }
      } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
        throw new MongobeeException(e.getMessage(), e);
      } catch (InvocationTargetException e) {
        Throwable targetException = e.getTargetException();
        throw new MongobeeException(targetException.getMessage(), e);
      }
    }
  }

  private void executeMigrationForChangeEntry(ChangeService service, Object changelogInstance,
      Method changesetMethod, ChangeEntry changeEntry)
      throws MongobeeConnectionException, IllegalAccessException, InvocationTargetException {
    try {
      if (dao.isNewChange(changeEntry)) {
        executeChangeSetMethod(changesetMethod, changelogInstance, dao.getMongoDatabase());
        dao.save(changeEntry);
        logger.info("{} applied", changeEntry);
      } else if (service.isRunAlwaysChangeSet(changesetMethod)) {
        executeChangeSetMethod(changesetMethod, changelogInstance, dao.getMongoDatabase());
        logger.info("{} reapplied", changeEntry);
      } else {
        logger.info("{} passed over", changeEntry);
      }
    } catch (MongobeeChangeSetException e) {
      logger.error(e.getMessage());
    }
  }

  private Object executeChangeSetMethod(Method changeSetMethod, Object changeLogInstance,
      MongoDatabase mongoDatabase)
      throws IllegalAccessException, InvocationTargetException, MongobeeChangeSetException {
    if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)) {
      logger.debug("method with MongoTemplate argument");

      return changeSetMethod.invoke(changeLogInstance,
          mongoTemplate != null ? mongoTemplate : new MongoTemplate(mongoClient, dbName));
    } else if (changeSetMethod.getParameterTypes().length == 2
        && changeSetMethod.getParameterTypes()[0].equals(MongoTemplate.class)
        && changeSetMethod.getParameterTypes()[1].equals(Environment.class)) {
      logger.debug("method with MongoTemplate and environment arguments");

      return changeSetMethod.invoke(changeLogInstance,
          mongoTemplate != null ? mongoTemplate : new MongoTemplate(mongoClient, dbName),
          springEnvironment);
    } else if (changeSetMethod.getParameterTypes().length == 1
        && changeSetMethod.getParameterTypes()[0].equals(MongoDatabase.class)) {
      logger.debug("method with DB argument");

      return changeSetMethod.invoke(changeLogInstance, mongoDatabase);
    } else if (changeSetMethod.getParameterTypes().length == 0) {
      logger.debug("method with no params");

      return changeSetMethod.invoke(changeLogInstance);
    } else {
      throw new MongobeeChangeSetException("ChangeSet method " + changeSetMethod.getName() +
          " has wrong arguments list. Please see docs for more info!");
    }
  }

  private void validateConfig() throws MongobeeConfigurationException {
    if (!hasText(dbName)) {
      throw new MongobeeConfigurationException(
          "DB name is not set. It should be defined in MongoDB URI or via setter");
    }
    if (!hasText(changeLogsScanPackage)) {
      throw new MongobeeConfigurationException(
          "Scan package for changelogs is not set: use appropriate setter");
    }
  }

  /**
   * @return true if an execution is in progress, in any process.
   * @throws MongobeeConnectionException exception
   */
  public boolean isExecutionInProgress() throws MongobeeConnectionException {
    return dao.isProccessLockHeld();
  }

  /**
   * Used DB name should be set here or via MongoDB URI (in a constructor)
   *
   * @param dbName database name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setDbName(String dbName) {
    this.dbName = dbName;
    return this;
  }

  /**
   * Package name where @ChangeLog-annotated classes are kept.
   *
   * @param changeLogsScanPackage package where your changelogs are
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangeLogsScanPackage(String changeLogsScanPackage) {
    this.changeLogsScanPackage = changeLogsScanPackage;
    return this;
  }

  /**
   * @return true if Mongobee runner is enabled and able to run, otherwise false
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Feature which enables/disables Mongobee runner execution
   *
   * @param enabled MOngobee will run only if this option is set to true
   * @return Mongobee object for fluent interface
   */
  public Mongobee setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Feature which enables/disables waiting for lock if it's already obtained
   *
   * @param waitForLock Mongobee will be waiting for lock if it's already obtained if this option is
   *                    set to true
   * @return Mongobee object for fluent interface
   */
  public Mongobee setWaitForLock(boolean waitForLock) {
    this.dao.setWaitForLock(waitForLock);
    return this;
  }

  /**
   * Waiting time for acquiring lock if waitForLock is true
   *
   * @param changeLogLockWaitTime Waiting time in minutes for acquiring lock
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangeLogLockWaitTime(long changeLogLockWaitTime) {
    this.dao.setChangeLogLockWaitTime(changeLogLockWaitTime);
    return this;
  }

  /**
   * Poll rate for acquiring lock if waitForLock is true
   *
   * @param changeLogLockPollRate Poll rate in seconds for acquiring lock
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangeLogLockPollRate(long changeLogLockPollRate) {
    this.dao.setChangeLogLockPollRate(changeLogLockPollRate);
    return this;
  }

  /**
   * Feature which enables/disables throwing MongobeeLockException if Mongobee can not obtain lock
   *
   * @param throwExceptionIfCannotObtainLock Mongobee will throw MongobeeLockException if lock can
   *                                         not be obtained
   * @return Mongobee object for fluent interface
   */
  public Mongobee setThrowExceptionIfCannotObtainLock(boolean throwExceptionIfCannotObtainLock) {
    this.dao.setThrowExceptionIfCannotObtainLock(throwExceptionIfCannotObtainLock);
    return this;
  }

  /**
   * Set Environment object for Spring Profiles (@Profile) integration
   *
   * @param environment org.springframework.core.env.Environment object to inject
   * @return Mongobee object for fluent interface
   */
  public Mongobee setSpringEnvironment(Environment environment) {
    this.springEnvironment = environment;
    return this;
  }

  /**
   * Sets pre-configured {@link MongoTemplate} instance to use by the Mongobee
   *
   * @param mongoTemplate instance of the {@link MongoTemplate}
   * @return Mongobee object for fluent interface
   */
  public Mongobee setMongoTemplate(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    return this;
  }

  /**
   * Overwrites a default mongobee changelog collection hardcoded in DEFAULT_CHANGELOG_COLLECTION_NAME.
   * <p>
   * CAUTION! Use this method carefully - when changing the name on a existing system, your
   * changelogs will be executed again on your MongoDB instance
   *
   * @param changelogCollectionName a new changelog collection name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setChangelogCollectionName(String changelogCollectionName) {
    this.dao.setChangelogCollectionName(changelogCollectionName);
    return this;
  }

  /**
   * Overwrites a default mongobee lock collection hardcoded in DEFAULT_LOCK_COLLECTION_NAME
   *
   * @param lockCollectionName a new lock collection name
   * @return Mongobee object for fluent interface
   */
  public Mongobee setLockCollectionName(String lockCollectionName) {
    this.dao.setLockCollectionName(lockCollectionName);
    return this;
  }

  /**
   * Closes the Mongo instance used by Mongobee. This will close either the connection Mongobee was
   * initiated with or that which was internally created.
   */
  public void close() {
    dao.close();
  }
}
