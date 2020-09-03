package de.steve4u.mongobee.exception;

public class MongobeeException extends Exception {

  public MongobeeException(String message) {
    super(message);
  }

  public MongobeeException(String message, Throwable cause) {
    super(message, cause);
  }
}
