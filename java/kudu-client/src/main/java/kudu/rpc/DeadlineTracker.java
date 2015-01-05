// Copyright (c) 2014, Cloudera, inc.
// Confidential Cloudera Information: Covered by NDA.
package kudu.rpc;

import com.google.common.base.Stopwatch;

/**
 * This is a wrapper class around Stopwatch so that we can also track a deadline.
 * The watch starts as soon as this object is created with a deadline of 0,
 * meaning that there's no deadline.
 * The deadline has been reached once the stopwatch's elapsed time is equal or greater than the
 * provided deadline.
 */
public class DeadlineTracker {
  private final Stopwatch stopwatch;
  private long deadline = 0;

  /**
   * Creates a new tracker, which starts the stopwatch right now.
   */
  public DeadlineTracker() {
    this(new Stopwatch());
  }

  /**
   * Creates a new tracker, using the specified stopwatch, and starts is right now.
   * The stopwatch is reset if it was already running.
   * @param stopwatch Specific Stopwatch to use
   */
  public DeadlineTracker(Stopwatch stopwatch) {
    if (stopwatch.isRunning()) {
      stopwatch.reset();
    }
    this.stopwatch = stopwatch.start();
  }

  /**
   * Check if we're already past the deadline.
   * @return true if we're past the deadline, otherwise false. Also returns false if no deadline
   * was specified
   */
  public boolean timedOut() {
    if (!hasDeadline()) {
      return false;
    }
    return deadline - stopwatch.elapsedMillis() <= 0;
  }

  /**
   * Get the number of milliseconds before the deadline is reached.
   * Special semantic: this method is used to pass down the remaning deadline to the RPCs,
   * for which a deadline of 0 means no deadline and it also won't accept a negative deadline.
   * Thus, if deadline - stopwatch.elapsedMillis(), then 1 will be returned.
   * @return the remaining millis before the deadline is reached, or 1 if the remaining time is
   * lesser or equal to 0, or Long.MAX_VALUE if no deadline was specified (in which case it
   * should never be called).
   * @throws IllegalStateException if this method is called and no deadline was set
   */
  public long getMillisBeforeDeadline() {
    if (!hasDeadline()) {
      throw new IllegalStateException("This tracker doesn't have a deadline set so it cannot " +
          "answer getMillisBeforeDeadline()");
    }
    long millisBeforeDeadline = deadline - stopwatch.elapsedMillis();
    millisBeforeDeadline = millisBeforeDeadline <= 0 ? 1 : millisBeforeDeadline;
    return millisBeforeDeadline;
  }

  public long getElapsedMillis() {
    return this.stopwatch.elapsedMillis();
  }

  /**
   * Tells if a non-zero deadline was set.
   * @return true if the deadline is greater than 0, false otherwise.
   */
  public boolean hasDeadline() {
    return deadline != 0;
  }

  /**
   * Utility method to check if sleeping for a specified amount of time would put us past the
   * deadline.
   * @param plannedSleepTime number of milliseconds for a planned sleep
   * @return if the planned sleeps goes past the deadline.
   */
  public boolean wouldSleepingTimeout(long plannedSleepTime) {
    if (!hasDeadline()) {
      return false;
    }
    return getMillisBeforeDeadline() - plannedSleepTime <= 0;
  }

  /**
   * Sets the deadline to 0 (no deadline) and restarts the stopwatch from scratch.
   */
  public void reset() {
    deadline = 0;
    stopwatch.reset();
    stopwatch.start();
  }

  /**
   * Get the deadline
   * @return the current deadline
   */
  public long getDeadline() {
    return deadline;
  }

  /**
   * Set a new deadline for this tracker. It cannot be smaller than 0,
   * and if it is 0 then it means that there is no deadline (which is the default behavior).
   * This method won't call reset().
   * @param deadline a number of milliseconds greater or equal to 0
   * @throws IllegalArgumentException if the deadline is lesser than 0
   */
  public void setDeadline(long deadline) {
    if (deadline < 0) {
      throw new IllegalArgumentException("The deadline must be greater or equal to 0, " +
          "the passed value is " + deadline);
    }
    this.deadline = deadline;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer("DeadlineTracker(timeout=");
    buf.append(deadline);
    buf.append(", elapsed=").append(stopwatch.elapsedMillis());
    buf.append(")");
    return buf.toString();
  }
}
