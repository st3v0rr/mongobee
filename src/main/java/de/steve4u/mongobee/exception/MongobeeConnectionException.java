package de.steve4u.mongobee.exception;

/**
 * Error while connection to MongoDB
 */
public class MongobeeConnectionException extends MongobeeException {

  public MongobeeConnectionException(String message, Exception baseException) {
    super(message, baseException);
  }
}
