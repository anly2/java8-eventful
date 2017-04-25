package aanchev.eventful;

/**
 * A Functional Interface (like {@link java.util.function.Consumer Consumer})
 * that can potentially throw {@link VetoEventException} or {@link ConsumeEventException}.
 */
public interface Handler<DATA> {
	public void handle(DATA event) throws VetoEventException, ConsumeEventException;
	
	@SuppressWarnings("unchecked")
	public default boolean tryHandle(Object data) throws VetoEventException, ConsumeEventException {
		try {
			handle((DATA) data);
			return true;
		}
		catch (ClassCastException e) {
			return false;
		}
	}
}