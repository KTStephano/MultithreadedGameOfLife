package cs351.lab4;

import cs351.job.Job;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the only job class that the SimulationEngine uses. It updates
 * its section of the board one time and then lets the engine know it's
 * done.
 *
 * @author Justin Hall
 */
public class GridUpdateJob extends Job
{
  private final ReentrantLock LOCK = new ReentrantLock();
  private int START_X, END_X;
  private int START_Y, END_Y;
  private byte[][] frontBuffer; // never writes to this (guaranteed)
  private byte[][] backBuffer;
  private final int[] MAP_X = { -1, -1, -1, 0, 0, 1, 1, 1 };
  private final int[] MAP_Y = { -1, 0, 1, -1, 1, -1, 0, 1 };
  private final SimulationEngine ENGINE;

  /**
   * Creates the update job with a set of constant parameters.
   *
   * @param engine SimulationEngine object for callbacks
   * @param startX the x-value it should start with for updating (inclusive)
   * @param endX the x-value it should stop with for updating (exclusive)
   * @param startY the y-value it should start with for updating (inclusive)
   * @param endY the y-value it should stop with for updating (exclusive)
   */
  public GridUpdateJob(SimulationEngine engine, int startX, int endX, int startY, int endY)
  {
    ENGINE = engine;
    START_X = startX;
    END_X = endX;
    START_Y = startY;
    END_Y = endY;
  }

  /**
   * Runs through its section of the board exactly once, writing the value for
   * each cell to the backBuffer and reading from the frontBuffer.
   *
   * @param threadID integer id for the thread the job is being executed on
   */
  @Override
  public void run(int threadID)
  {
    final byte MAX_CELL_AGE = 10;
    LOCK.lock();
    try
    {
      for (int x = START_X; x < END_X; ++x)
      {
        for (int y = START_Y; y < END_Y; ++y)
        {
          int alive = 0;
          for (int i = 0; i < MAP_X.length; i++)
          {
            if (frontBuffer[x + MAP_X[i]][y + MAP_Y[i]] > 0)
            {
              alive++;
              if (alive > 3) break;
            }
          }
          if (frontBuffer[x][y] > 0 && alive >= 2 && alive <= 3)
          {
            ++backBuffer[x][y];
            if (backBuffer[x][y] > MAX_CELL_AGE) --backBuffer[x][y];
          }
          else if (alive == 3) backBuffer[x][y] = 1;
          else backBuffer[x][y] = 0;
        }
      }
    }
    finally
    {
      LOCK.unlock();
    }
    ENGINE.notifyEngineOfThreadCompletion();
  }

  /**
   * This is called to let the job know which reference to use
   * for the front and back buffers.
   *
   * @param frontBuffer front grid to read from (previous frame)
   * @param backBuffer back grid to write to (current frame)
   */
  public void initFrame(byte[][] frontBuffer, byte[][] backBuffer)
  {
    LOCK.lock();
    try
    {
      this.frontBuffer = frontBuffer;
      this.backBuffer = backBuffer;
    }
    finally
    {
      LOCK.unlock();
    }
  }
}
