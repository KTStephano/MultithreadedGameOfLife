package cs351.lab4;

import cs351.job.JobSystem;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationEngine
{
  private final ReentrantLock LOCK = new ReentrantLock();
  private boolean[][] frontBuffer;
  private boolean[][] backBuffer;
  private GridUpdateJob[] jobs;
  private boolean isStarted = false;
  private boolean isPaused = true;
  private boolean prevFrameFinished = true;
  private int numThreads;
  private int numActiveThreads;
  private JobSystem jobSystem;

  public void init(int worldWidth, int worldHeight, int numThreads)
  {
    LOCK.lock();
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
      LOCK.unlock();
    }
  }

  public void togglePause(boolean value)
  {
    LOCK.lock();
    try
    {
      isPaused = value;
      if (!isPaused) runFrame();
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public boolean isPaused()
  {
    LOCK.lock();
    try
    {
      return isPaused;
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void shutdown()
  {
    LOCK.lock();
    try
    {
      if (!isStarted) throw new RuntimeException("Engine was not started");
      isStarted = false;
      jobSystem.stop(false);
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public boolean isValid(int x, int y)
  {
    LOCK.lock();
    try
    {
      return x >= 1 && x < frontBuffer.length && y >= 1 && y < frontBuffer[0].length;
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void set(int x, int y, boolean val)
  {
    x++;
    y++;
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to set");
    LOCK.lock();
    try
    {
      if (!isPaused || !prevFrameFinished) return;
      frontBuffer[x][y] = val;
      backBuffer[x][y] = val;
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public boolean get(int x, int y)
  {
    x++;
    y++;
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to get");
    LOCK.lock();
    try
    {
      if (!isPaused) return false;
      return frontBuffer[x][y];
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void notifyEngineOfThreadCompletion()
  {
    LOCK.lock();
    boolean startNextFrame = false;
    try
    {
      --numActiveThreads;
      if (numActiveThreads == 0)
      {
        prevFrameFinished = true;
        startNextFrame = !isPaused;
      }
    }
    finally
    {
      LOCK.unlock();
    }
    if (startNextFrame) runFrame();
  }

  public boolean[][] getFrontBuffer()
  {
    if (LOCK.tryLock())
    {
      try
      {
        return frontBuffer;
      }
      finally
      {
        LOCK.unlock();
      }
    }
    return null;
  }

  public boolean toggleRenderMode(boolean lock)
  {
    if (lock) return LOCK.tryLock();
    else LOCK.unlock();
    return true;
  }

  private void runFrame()
  {
    LOCK.lock();
    try
    {
      if (!isStarted || !prevFrameFinished) return;
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
      prevFrameFinished = false;
    }
    finally
    {
      LOCK.unlock();
    }
  }
}
