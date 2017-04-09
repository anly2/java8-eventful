# java8-eventful
A Java Interface that makes use of defaults and lambda to provide an Event Bus/System functionality in a convenient way.

## Example
```java
Eventful<Exception> eventful;
// get an instance...
// can be as easy as:
eventful = new Eventful.Default<Exception>(){}; //this is an interface, hence the anonymous implementing class

eventful.on("event", (ArrayIndexOutOfBoundsException e) -> { throw new Eventful.VetoEventException(); }); //stops subsequent handlers from being called; fire() returns false
eventful.on("event", (IOException e) -> { throw new Eventful.ConsumeEventException(); }); //stops subsequent handlers from being called; fire() returns true
eventful.on("event", e -> System.err.println(e.getMessage())); //simplest use; the "lambda object" is returned for future reference
eventful.on("event", (InterruptedException e) -> e.printStackTrace()); //simple use, but only called with InterruptedExceptions - with other types it is silently ignored
eventful.on("event", (RuntimeException e) -> { //unwrap and propagate
	if (e.getCause() != null)
		eventful.fire("event", (Exception) e.getCause()); //trigger another event
});
```
