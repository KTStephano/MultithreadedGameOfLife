package cs351.job;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

public class WorkerThread extends Thread
{
  private final ReentrantLock LOCK;
  private final int ID;
  private final JobSystem JOB_SYSTEM;
  private volatile boolean isRunning = true;
  private volatile boolean completeExistingJobsBeforeTerminating = false;

  public WorkerThread(int id, JobSystem jobSystem)
  {
    LOCK = new ReentrantLock();
    ID = id;
    JOB_SYSTEM = jobSystem;
  }

  @Override
  public void run()
  {
    boolean isRunning = true;
    System.out.println("Worker Thread #" + ID + " has started");
    Collection<Job> jobs = null;
    while (isRunning)
    {
      LOCK.lock();
      try
      {
        if (completeExistingJobsBeforeTerminating && jobs == null) isRunning = false;
        else isRunning = this.isRunning;
      }
      finally
      {
        LOCK.unlock();
      }
      // if there are jobs, execute them but otherwise signal that this is a good
      // time to pause this thread
      jobs = JOB_SYSTEM.getJobs();
      if (jobs != null) for (Job job : jobs) job.run(ID);
      else yield();
    }
    System.out.println("Worker Thread #" + ID + " has finished");
  }

  public void terminate(boolean completeExistingJobs)
  {
    LOCK.lock();
    try
    {
      if (completeExistingJobs) completeExistingJobsBeforeTerminating = true;
      else isRunning = false;
    }
    finally
    {
      LOCK.unlock();
    }
  }
}
