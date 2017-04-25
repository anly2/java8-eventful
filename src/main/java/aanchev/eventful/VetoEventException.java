package aanchev.eventful;

/**
 * An exception that allows a handler to stop the invocation of subsequent handlers for that event.
 * This is very similar to {@link ConsumeEventException},
 * except the triggering call to {@link Eventful#fire(String, Object)} will return with <code>false</code>,
 * indicating a failure.
 * For more specific error indications, custom runtime exceptions can be thrown.
 */
public class VetoEventException extends Exception {
	private static final long serialVersionUID = 1L;
}