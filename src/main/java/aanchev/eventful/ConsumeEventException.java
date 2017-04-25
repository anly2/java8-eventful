package aanchev.eventful;

/**
 * An exception that allows a handler to stop the invocation of subsequent handlers for that event.
 * This is very similar to {@link VetoEventException},
 * except the triggering call to {@link Eventful#fire(String, Object)} will return with <code>true</code>,
 * indicating a successful, albeit premature completion.
 */
public class ConsumeEventException extends Exception {
	private static final long serialVersionUID = 1L;
}