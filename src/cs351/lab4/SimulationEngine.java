package cs351.lab4;

import cs351.job.JobSystem;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationEngine
{
  private final ReentrantLock MAIN_LOCK = new ReentrantLock();
  private final ReentrantLock RENDER_LOCK = new ReentrantLock();
  private boolean[][] frontBuffer;
  private boolean[][] backBuffer;
  private GridUpdateJob[] jobs;
  private boolean isStarted = false;
  private int numThreads;
  private int numActiveThreads;
  private JobSystem jobSystem;

  public void start(int worldWidth, int worldHeight, int numThreads)
  {
    MAIN_LOCK.lock();
    RENDER_LOCK.lock();
    try
    {
      if (isStarted) throw new RuntimeException("Engine was already started");
      isStarted = true;
      this.numThreads = numThreads;
      worldWidth += 2; // border
      worldHeight += 2; // border
      jobs = new GridUpdateJob[numThreads];
      // @TODO: Make sure this is actually right
      int xOffset = (worldWidth - 2) / numThreads;
      for (int i = 0; i < numThreads; i++)
      {
        int xStart = i * xOffset + 1;
        int xEnd = xStart + xOffset;
        jobs[i] = new GridUpdateJob(this, xStart, xEnd, 1, worldHeight - 1);
      }
      frontBuffer = new boolean[worldWidth][worldHeight];
      backBuffer = new boolean[worldWidth][worldHeight];
      jobSystem = new JobSystem(numThreads);
      jobSystem.start();
    }
    finally
    {
      MAIN_LOCK.unlock();
      RENDER_LOCK.unlock();
    }
  }

  public void stop()
  {
    MAIN_LOCK.lock();
    RENDER_LOCK.lock();
    try
    {
      if (!isStarted) throw new RuntimeException("Engine was not started");
      isStarted = false;
      jobSystem.stop(false);
    }
    finally
    {
      MAIN_LOCK.unlock();
      RENDER_LOCK.unlock();
    }
  }

  public void runFrame()
  {
    MAIN_LOCK.lock();
    RENDER_LOCK.lock();
    try
    {
      numActiveThreads = numThreads;
      boolean[][] swap = frontBuffer;
      frontBuffer = backBuffer;
      backBuffer = swap;
      for (GridUpdateJob job : jobs)
      {
        job.initFrame(frontBuffer, backBuffer);
        jobSystem.submitJob(job);
      }
      jobSystem.dispatchJobs();
    }
    finally
    {
      MAIN_LOCK.unlock();
      RENDER_LOCK.unlock();
    }
  }

  public boolean isValid(int x, int y)
  {
    MAIN_LOCK.lock();
    try
    {
      return x >= 1 && x < frontBuffer.length && y >= 1 && y < frontBuffer[0].length;
    }
    finally
    {
      MAIN_LOCK.unlock();
    }
  }

  public void set(int x, int y, boolean val)
  {
    x++;
    y++;
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to set");
    MAIN_LOCK.lock();
    try
    {
      backBuffer[x][y] = val;
    }
    finally
    {
      MAIN_LOCK.unlock();
    }
  }

  public void notifyEngineOfThreadCompletion()
  {
    MAIN_LOCK.lock();
    boolean startNextFrame = false;
    try
    {
      --numActiveThreads;
      if (numActiveThreads == 0) startNextFrame = true;
    }
    finally
    {
      MAIN_LOCK.unlock();
    }
    if (startNextFrame) runFrame();
  }

  public boolean[][] getFrontBuffer()
  {
    if (RENDER_LOCK.tryLock())
    {
      try
      {
        return frontBuffer;
      }
      finally
      {
        RENDER_LOCK.unlock();
      }
    }
    return null;
  }

  public boolean toggleRenderMode(boolean lock)
  {
    if (lock) return RENDER_LOCK.tryLock();
    else RENDER_LOCK.unlock();
    return true;
  }
}
