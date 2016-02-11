package cs351.presets;

import cs351.lab4.SimulationEngine;
import java.util.Random;

/**
 * Sets the initial grid state to be randomized such that each cell
 * has approximately a 50% chance of being alive.
 *
 * @author Justin Hall
 */
public class RandomGrid implements Preset
{
  private final Random RAND = new Random();

  /**
   * At each cell, calls nextInt(100) and if that value is > 50, the cell
   * is set to alive.
   *
   * @param engine SimulationEngine object to use
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    for (int x = 0; x < engine.getWorldWidth(); x++)
    {
      for (int y = 0; y < engine.getWorldHeight(); y++)
      {
        if (RAND.nextInt(100) > 50) engine.setAge(x, y, 1);
        else engine.setAge(x, y, 0);
      }
    }
  }
}
