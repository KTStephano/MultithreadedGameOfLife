package cs351.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class serves as an easy way to start up to 256 threads and
 * not have to worry about managing them. The ideal way to use this
 * class is to call start() once and then use it until either the program
 * is quitting or until it is not needed anymore in its current state.
 *
 * @author Justin Hall
 */
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

  /**
   * The number of worker threads becomes constant internally and cannot be changed.
   * This constructor does some initial work in allocating resources but does not
   * start the job system.
   *
   * @param numWorkerThreads number of worker threads from 1 to 256
   */
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

  /**
   * Creates a number of threads equal to NUM_WORKER_THREADS. After calling this function
   * the job system is ready to accept jobs.
   */
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

  /**
   * Terminates all existing threads.
   *
   * @param completeExistingJobs if this is true, threads will keep working until
   *                             all jobs in the job system's queue are completed -
   *                             if false they all quit as soon as possible
   */
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

  /**
   * Takes a single job object and tries to add it to the back buffer (threads can't
   * see this buffer). It does this by looking at the first element in the buffer
   * for the job's priority (which is usually the buffer with the fewest elements
   * because of the sorting done by the PriorityQueue). If it exists the job is added.
   * If not the job system will try to expand its buffer by a certain amount to not
   * only accept the current job but also future jobs.
   *
   * @param job job to be added to the back buffer
   */
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

  /**
   * Gets the top collection of jobs from the front buffer.
   *
   * @return collection of jobs from the front buffer
   */
  public Collection<Job> getJobs()
  {
    final JobGroup GROUP = JOBS.poll();
    return GROUP != null ? GROUP.getJobs() : null;
  }

  /**
   * This takes all jobs from the back buffer (that have jobs) and
   * adds them to the front buffer so that the threads can see them. At
   * the end of the loop it re-adds any buffers it took off of the back buffer
   * which had 0 jobs.
   */
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
   * Attempts to add NUM_WORKER_THREADS new buffers to the back buffer for the given priority
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
