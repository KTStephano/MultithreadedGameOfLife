package cs351.job;

public abstract class Job
{
  private final int PRIORITY;
  private final int DEFAULT_PRIORITY = 5;

  public Job()
  {
    PRIORITY = DEFAULT_PRIORITY;
  }

  public Job(int priority)
  {
    PRIORITY = priority;
  }

  public abstract void run(int threadID);

  public int getPriority()
  {
    return PRIORITY;
  }
}
