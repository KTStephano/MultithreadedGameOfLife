package cs351.job;

import java.util.Collection;

/**
 * This class is only ever used by the job system internally. There is
 * no way to get a JobGroup or pass a JobGroup from/to the job system.
 *
 * @author Justin Hall
 */
final public class JobGroup
{
  private final int PRIORITY;
  private final Collection<Job> JOBS;

  /**
   * Takes the priority of the entire collection of jobs and the collection
   * itself.
   *
   * @param priority priority of the jobs
   * @param jobs collection of jobs
   */
  public JobGroup(int priority, Collection<Job> jobs)
  {
    PRIORITY = priority;
    JOBS = jobs;
  }

  /**
   * Used by the job system to get the stored jobs.
   *
   * @return collection of jobs
   */
  public Collection<Job> getJobs()
  {
    return JOBS;
  }

  /**
   * Used by the job system to get the priority for an entire group of jobs.
   *
   * @return priority for this job group
   */
  public int getPriority()
  {
    return PRIORITY;
  }
}
