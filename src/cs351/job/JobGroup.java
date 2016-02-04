package cs351.job;

import java.util.Collection;

public class JobGroup
{
  private final int PRIORITY;
  private final Collection<Job> JOBS;

  public JobGroup(int priority, Collection<Job> jobs)
  {
    PRIORITY = priority;
    JOBS = jobs;
  }

  public Collection<Job> getJobs()
  {
    return JOBS;
  }

  public int getPriority()
  {
    return PRIORITY;
  }
}
