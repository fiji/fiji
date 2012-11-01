/** 
 * Time the execution of any block of code.
 * <p> 
 * Copied from <a href=http://www.javapractices.com/topic/TopicAction.do?Id=85>javapractice.com</a>
 */

public final class Stopwatch {

  /**
  * An example of the use of this class to
  * time the execution of String manipulation code.
  */
  public static void main (String... arguments) {
    Stopwatch stopwatch = new Stopwatch();

    stopwatch.start();

    //do stuff
    StringBuilder messageOne = new StringBuilder();
    int numIterations = 5000;
    for(int idx=0; idx < numIterations; ++idx){
      messageOne.append("blah");
    }

    stopwatch.stop();
    //Note that there is no need to call a method to get the duration,
    //since toString is automatic here
    System.out.println("The reading for StringBuilder is: " + stopwatch);

    //reuse the same stopwatch to measure an alternative implementation
    //Note that there is no need to call a reset method.
    stopwatch.start();

    //do stuff again
    String messageTwo = null;
    for(int idx=0; idx < numIterations; ++idx){
      messageTwo = messageTwo + "blah";
    }

    stopwatch.stop();
    //perform a numeric comparsion
    if ( stopwatch.toValue() > 5 ) {
      System.out.println("The reading is high: " + stopwatch);
    }
    else {
      System.out.println("The reading is low: " + stopwatch);
    }
  }

  /**
  * Start the stopwatch.
  *
  * @throws IllegalStateException if the stopwatch is already running.
  */
  public void start(){
    if ( fIsRunning ) {
      throw new IllegalStateException("Must stop before calling start again.");
    }
    //reset both start and stop
    fStart = System.currentTimeMillis();
    fStop = 0;
    fIsRunning = true;
    fHasBeenUsedOnce = true;
  }

  /**
  * Stop the stopwatch.
  *
  * @throws IllegalStateException if the stopwatch is not already running.
  */
  public void stop() {
    if ( !fIsRunning ) {
      throw new IllegalStateException("Cannot stop if not currently running.");
    }
    fStop = System.currentTimeMillis();
    fIsRunning = false;
  }

  /**
  * Express the "reading" on the stopwatch.
  *
  * @throws IllegalStateException if the Stopwatch has never been used,
  * or if the stopwatch is still running.
  */
  public String toString() {
    validateIsReadable();
    StringBuilder result = new StringBuilder();
    result.append(fStop - fStart);
    result.append(" ms");
    return result.toString();
  }

  /**
  * Express the "reading" on the stopwatch as a numeric type.
  *
  * @throws IllegalStateException if the Stopwatch has never been used,
  * or if the stopwatch is still running.
  */
  public long toValue() {
    validateIsReadable();
    return fStop - fStart;
  }

  // PRIVATE ////
  private long fStart;
  private long fStop;

  private boolean fIsRunning;
  private boolean fHasBeenUsedOnce;

  /**
  * Throws IllegalStateException if the watch has never been started,
  * or if the watch is still running.
  */
  private void validateIsReadable() {
    if ( fIsRunning ) {
      String message = "Cannot read a stopwatch which is still running.";
      throw new IllegalStateException(message);
    }
    if ( !fHasBeenUsedOnce ) {
      String message = "Cannot read a stopwatch which has never been started.";
      throw new IllegalStateException(message);
    }
  }
}
