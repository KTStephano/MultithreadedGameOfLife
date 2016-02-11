package cs351.presets;

import cs351.lab4.SimulationEngine;

/**
 * Sets the engine to have a full grid except for its borders.
 *
 * @author Justin Hall
 */
public class FullGrid implements Preset
{
  /**
   * Runs through every cell and sets it to alive (except borders).
   *
   * @param engine SimulationEngine object to use.
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    int width = engine.getWorldWidth();
    int height = engine.getWorldHeight();
    for (int x = 0; x < width; x++)
    {
      for (int y = 0; y < height; y++)
      {
        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) engine.setAge(x, y, 1);
        else engine.setAge(x, y, 0);
      }
    }
  }
}
