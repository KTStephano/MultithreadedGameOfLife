package cs351.job;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used internally by the job system and is not meant to be
 * extended. The job system does not provide a way to give it a thread
 * object or to get any of its thread objects.
 *
 * @author Justin Hall
 */
final public class WorkerThread extends Thread
{
  private final ReentrantLock LOCK;
  private final int ID;
  private final JobSystem JOB_SYSTEM;
  private volatile boolean isRunning = true;
  private volatile boolean completeExistingJobsBeforeTerminating = false;

  /**
   * Main constructor.
   *
   * @param id integer id for the thread (passed to Job's run function)
   * @param jobSystem reference back to the job system for callbacks
   */
  public WorkerThread(int id, JobSystem jobSystem)
  {
    LOCK = new ReentrantLock();
    ID = id;
    JOB_SYSTEM = jobSystem;
  }

  /**
   * Once this is called, the worker thread continues looping and asks the job
   * system if there are any jobs for it to do. If not, it sleeps for 1 millisecond.
   *
   * When this thread is told to terminate, its isRunning flag is set to false and it
   * exits after it is done working on the current block of jobs.
   */
  @Override
  public void run()
  {
    boolean isRunning = true;
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
      else
      {
        try
        {
          Thread.sleep(1);
        }
        catch (InterruptedException e)
        {

        }
      }
    }
  }

  /**
   * If completeExistingJobs is set to true then the thread will continue working until
   * there are no jobs left in the job system's internal queue. If it is false it will exit
   * immediately after the batch it is working on is finished.
   *
   * @param completeExistingJobs true if it should complete existing jobs in the queue and false if not
   */
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
