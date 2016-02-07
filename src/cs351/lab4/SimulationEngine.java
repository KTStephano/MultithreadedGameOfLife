package cs351.lab4;

import cs351.job.JobSystem;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationEngine
{
  private final ReentrantLock LOCK = new ReentrantLock();
  private byte[][] frontBuffer;
  private byte[][] backBuffer;
  private int worldWidth, worldHeight;
  private GridUpdateJob[] jobs;
  private boolean isStarted = false;
  private boolean isPaused = true;
  private boolean prevFrameFinished = true;
  private boolean needsToSwapBuffers = true;
  private int numThreads;
  private int numActiveThreads;
  private JobSystem jobSystem;

  public SimulationEngine(int worldWidth, int worldHeight)
  {
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;
    frontBuffer = new byte[worldWidth + 2][worldHeight + 2];
    backBuffer = new byte[worldWidth + 2][worldHeight + 2];
  }

  public void init(int numThreads)
  {
    LOCK.lock();
    try
    {
      if (isStarted) throw new RuntimeException("Engine was already started");
      System.out.println();
      System.out.println("Initializing engine ...");
      isStarted = true;
      this.numThreads = numThreads;
      jobs = new GridUpdateJob[numThreads];
      // @TODO: Make sure this is actually right
      int xOffset = worldWidth / numThreads;
      for (int i = 0; i < numThreads; i++)
      {
        int xStart = i * xOffset + 1;
        int xEnd = xStart + xOffset;
        // when worldWidth / numThreads doesn't divide evenly, this prevents it
        // from leaving dead cells at the end of the board
        if (i + 1 >= numThreads) xEnd = worldWidth + 1;
        jobs[i] = new GridUpdateJob(this, xStart, xEnd, 1, worldHeight + 1);
      }
      jobSystem = new JobSystem(numThreads);
      jobSystem.start();
      System.out.println("Engine initialized");
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public int getNumThreads()
  {
    LOCK.lock();
    try
    {
      if (!isStarted) throw new RuntimeException("Engine not started");
      return numThreads;
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public void togglePause(boolean value, boolean startNextFrame)
  {
    LOCK.lock();
    try
    {
      isPaused = value;
      if (!isPaused && startNextFrame) runFrame();
      else if (!isPaused) swapBuffers();
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

  public boolean previousFrameCompleted()
  {
    LOCK.lock();
    try
    {
      return prevFrameFinished;
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
      System.out.println("Engine shutdown");
      isStarted = false;
      jobSystem.stop(false);
    }
    finally
    {
      LOCK.unlock();
    }
  }

  public int getWorldWidth()
  {
    return worldWidth;
  }

  public int getWorldHeight()
  {
    return worldHeight;
  }

  public boolean isValid(int x, int y)
  {
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("SimulationEngine.lock() not called before isValid");
    return x >= 1 && x < getWorldWidth() + 1 && y >= 1 && y < getWorldHeight() + 1;
  }

  public void setAge(int x, int y, int age)
  {
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("Call SimulationEngine.lock() before calls to setAge");
    // x and y values start at 1 due to border but getWorldWidth/Height
    // functions report the size without the border
    x++;
    y++;
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to set");
    else if (!isPaused || !prevFrameFinished) return;
    frontBuffer[x][y] = (byte)age;
    backBuffer[x][y] = (byte)age;
  }

  public int getAge(int x, int y)
  {
    x++;
    y++;
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("Call SimulationEngine.lock() before calls to getAge");
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to get");
    return frontBuffer[x][y];
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
        needsToSwapBuffers = true;
        startNextFrame = !isPaused;
      }
    }
    finally
    {
      LOCK.unlock();
    }
    if (startNextFrame) runFrame();
    else swapBuffers();
  }

  public byte[][] getFrontBuffer()
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

  public void lock()
  {
    LOCK.lock();
  }

  public void unlock()
  {
    LOCK.unlock();
  }

  private void runFrame()
  {
    LOCK.lock();
    try
    {
      if (!isStarted || !prevFrameFinished) return;
      numActiveThreads = numThreads;
      swapBuffers();
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

  private void swapBuffers()
  {
    LOCK.lock();
    try
    {
      if (!isStarted || !needsToSwapBuffers) return;
      byte[][] swap = frontBuffer;
      frontBuffer = backBuffer;
      backBuffer = swap;
      needsToSwapBuffers = false;
    }
    finally
    {
      LOCK.unlock();
    }
  }
}
