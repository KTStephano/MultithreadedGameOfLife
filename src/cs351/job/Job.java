package cs351.job;

/**
 * The abstract Job class is the only way of getting something executed
 * on the threads managed by the JobSystem class. It is meant to be
 * extended and then passed to a JobSystem instance by using its
 * .submitJob() function.
 *
 * @author Justin Hall
 */
public abstract class Job
{
  private final int PRIORITY;

  /**
   * Default constructor - sets priority to 5
   */
  public Job()
  {
    PRIORITY = 5;
  }

  /**
   * This function is called exactly once by one of the job system
   * threads.
   *
   * @param threadID integer id for the thread the job is being executed on
   */
  public abstract void run(int threadID);

  /**
   * Gets the priority for this job (lower being higher priority).
   *
   * @return job's priority
   */
  public int getPriority()
  {
    return PRIORITY;
  }
}
