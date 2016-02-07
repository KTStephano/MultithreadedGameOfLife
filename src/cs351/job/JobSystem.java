package cs351.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class JobSystem
{
  private final ReentrantLock LOCK;
  private final int NUM_WORKER_THREADS;
  private final WorkerThread[] WORKER_THREADS;
  // outer Integer = priority ; inner Integer = number of threads
  private final HashMap<Integer, PriorityQueue<LinkedList<Job>>> JOB_BUFFER;
  // these are the jobs visible to the worker threads
  private final PriorityBlockingQueue<JobGroup> JOBS;
  // used for better load balancing - prevents one thread from receiving a massive number of
  // jobs at one time that could have been better distributed over the active threads
  private final int MAX_JOBS_PER_GROUP = 100;
  private boolean isStarted = false;

  public JobSystem(int numWorkerThreads)
  {
    final int MIN_THREADS = 1;
    final int MAX_THREADS = 256;
    if (numWorkerThreads < MIN_THREADS) numWorkerThreads = MIN_THREADS;
    else if (numWorkerThreads > MAX_THREADS) numWorkerThreads = MAX_THREADS;
    LOCK = new ReentrantLock();
    NUM_WORKER_THREADS = numWorkerThreads;
    WORKER_THREADS = new WorkerThread[numWorkerThreads];
    JOB_BUFFER = new HashMap<>();
    JOBS = new PriorityBlockingQueue<>(10, (o1, o2) -> o2.getPriority() - o1.getPriority() );
  }

  public void start()
  {
    LOCK.lock();
    try
    {
      if (isStarted) throw new RuntimeException("Job System was already started");
      isStarted = true;
      for (int i = 0; i < NUM_WORKER_THREADS; i++)
      {
        WORKER_THREADS[i] = new WorkerThread(i + 1, this);
        WORKER_THREADS[i].start();
      }
      System.out.println("Job system started with " + NUM_WORKER_THREADS + " threads");
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void stop(boolean completeExistingJobs)
  {
    LOCK.lock();
    try
    {
      if (!isStarted) throw new RuntimeException("Job System was not started");
      isStarted = false;
      for (int i = 0; i < NUM_WORKER_THREADS; i++) WORKER_THREADS[i].terminate(completeExistingJobs);
      System.out.println("Job system shutdown");
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void submitJob(Job job)
  {
    final int PRIORITY = job.getPriority();
    LOCK.lock();
    try
    {
      if (!isStarted) throw new RuntimeException("Job System was not running - cannot submit job");
      if (!JOB_BUFFER.containsKey(PRIORITY)) addJobListsToBuffer(PRIORITY);
      else if (JOB_BUFFER.get(PRIORITY).size() == 0) addJobListsToBuffer(PRIORITY);
      PriorityQueue<LinkedList<Job>> buffers = JOB_BUFFER.get(PRIORITY);
      // if a match was found go ahead and add it, but otherwise grow the cs351.job buffer
      // and call submitJob again
      if (buffers.peek().size() >= MAX_JOBS_PER_GROUP)
      {
        addJobListsToBuffer(PRIORITY);
        submitJob(job);
        return;
      }
      // remove, add, and reinsert to force the PriorityQueue to figure out its new order
      LinkedList<Job> buffer = buffers.poll();
      buffer.add(job);
      buffers.add(buffer);
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public Collection<Job> getJobs()
  {
    final JobGroup GROUP = JOBS.poll();
    return GROUP != null ? GROUP.getJobs() : null;
  }

  public void dispatchJobs()
  {
    LOCK.lock();
    try
    {
      LinkedList<LinkedList<Job>> buffersToReAdd = new LinkedList<>();
      // iterate over each priority level and deconstruct its buffer so that at the end
      // no active buffers remain
      for (Map.Entry<Integer, PriorityQueue<LinkedList<Job>>> outer : JOB_BUFFER.entrySet())
      {
        while (outer.getValue().size() > 0)
        {
          LinkedList<Job> list = outer.getValue().poll();
          if (list.size() == 0) buffersToReAdd.add(list);
          else JOBS.add(new JobGroup(outer.getKey(), list));
        }
        outer.getValue().addAll(buffersToReAdd);
        buffersToReAdd.clear();
      }
    }
    finally
    {
      LOCK.unlock();
    }
  }

  /**
   * Attempts to add NUM_WORKER_THREADS new buffers to the general JOB_BUFFER for the given priority
   *
   * @param priority priority to add the new buffers at
   */
  private void addJobListsToBuffer(int priority)
  {
    LOCK.lock();
    try
    {
      if (!JOB_BUFFER.containsKey(priority)) JOB_BUFFER.put(priority, new PriorityQueue<>(MAX_JOBS_PER_GROUP, (o1, o2) -> o1.size() - o2.size()));
      PriorityQueue<LinkedList<Job>> list = JOB_BUFFER.get(priority);
      for (int i = 0; i < NUM_WORKER_THREADS; i++) list.add(new LinkedList<>());
    }
    finally
    {
      LOCK.unlock();
    }
  }
}
