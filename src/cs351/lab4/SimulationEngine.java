package cs351.lab4;

import cs351.job.JobSystem;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is used to manage the current state of the simulation and create
 * the next step in the simulation. Starts up the job system so that multithreading
 * can be used.
 *
 * @author Justin Hall
 */
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

  /**
   * Creates the front abd back buffers with the given width/height
   * values (with +2 for border padding).
   *
   * @param worldWidth width of the grid in pixels
   * @param worldHeight height of the grid in pixels
   */
  public SimulationEngine(int worldWidth, int worldHeight)
  {
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;
    frontBuffer = new byte[worldWidth + 2][worldHeight + 2];
    backBuffer = new byte[worldWidth + 2][worldHeight + 2];
  }

  /**
   * This function performs initial setup so that the engine is ready to begin
   * the simulation.
   *
   * @param numThreads number of threads to ask the job system to create
   */
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

  /**
   * Gets the number of threads the engine is currently using.
   *
   * @return number of threads
   */
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

  /**
   * When this is called the engine will either wake back up or stop active
   * execution. startNextFrame is a flag that lets the engine be unpaused
   * without starting the next frame (forces a buffer swap without continuing
   * execution).
   *
   * @param value true if paused and false if resumed
   * @param startNextFrame if true the next frame is run (which spawns future frames),
   *                       but if false it just swaps the buffers
   */
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

  /**
   * Checks if the engine is paused.
   *
   * @return true if paused and false if not
   */
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

  /**
   * If the previous frame it was working on is done, this will return true.
   *
   * @return true if yes and false if not
   */
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

  /**
   * Shuts down the job system so that the program can exit cleanly.
   */
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

  /**
   * Returns the world width.
   *
   * @return world width in pixels (minus the border padding)
   */
  public int getWorldWidth()
  {
    return worldWidth;
  }

  /**
   * Returns the world height.
   *
   * @return world height in pixels (minus the border padding)
   */
  public int getWorldHeight()
  {
    return worldHeight;
  }

  /**
   * Checks if the given (x, y) pair is valid.
   *
   * @param x x-value
   * @param y y-value
   * @return true if valid and false if not
   * @throws IllegalStateException if SimulationEngine.lock() is not called before this
   */
  public boolean isValid(int x, int y) throws IllegalStateException
  {
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("SimulationEngine.lock() not called before isValid");
    return x >= 1 && x < getWorldWidth() + 1 && y >= 1 && y < getWorldHeight() + 1;
  }

  /**
   * Sets the age of the cell at the given (x, y) location.
   *
   * @param x x-location
   * @param y-location
   * @param age age of the cell in generations (0 - 10)
   * @throws RuntimeException if SimulationEngine.lock() is not called before this or if the (x, y) pair is invalid
   */
  public void setAge(int x, int y, int age) throws RuntimeException
  {
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("Call SimulationEngine.lock() before calls to setAge");
    // x and y values start at 1 due to border but getWorldWidth/Height
    // functions report the size without the border
    x++;
    y++;
    if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to setAge");
    else if (!isPaused || !prevFrameFinished) return;
    frontBuffer[x][y] = (byte)age;
    backBuffer[x][y] = (byte)age;
  }

  /**
   * Gets the age of the cell at the given (x, y) coordinates.
   *
   * @param x x-location
   * @param y y-location
   * @return age of the cell
   * @throws RuntimeException if SimulationEngine.lock() is not called before this or if the given (x, y) pair is invalid
   */
  public int getAge(int x, int y) throws RuntimeException
  {
    x++;
    y++;
    if (!LOCK.isHeldByCurrentThread()) throw new IllegalStateException("Call SimulationEngine.lock() before calls to getAge");
    else if (!isValid(x, y)) throw new RuntimeException("Invalid (x, y) coordinates to getAge");
    return frontBuffer[x][y];
  }

  /**
   * This is called by the jobs when they are finished - when the number of working
   * threads hits 0 the engine knows it needs to start the next frame.
   */
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

  /**
   * Locks the engine for synchronization (lock is maintained until unlock() is called).
   */
  public void lock()
  {
    LOCK.lock();
  }

  /**
   * Unlocks the engine.
   */
  public void unlock()
  {
    LOCK.unlock();
  }

  /**
   * Starts the next frame by swapping the buffers, re-adding the jobs with updated
   * buffers, and then dispatching the jobs.
   */
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

  /**
   * Swaps the front and back buffers with each other.
   */
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
