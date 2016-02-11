package cs351.presets;

import cs351.lab4.SimulationEngine;

/**
 * This preset is to be given to a World object. Initializes a blank grid.
 *
 * @author Justin Hall
 */
public class BlankGrid implements Preset
{
  /**
   * Runs through each cell and sets it to 0 for the given engine.
   * @param engine SimulationEngine object to use
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    for (int x = 0; x < engine.getWorldWidth(); x++)
    {
      for (int y = 0; y < engine.getWorldHeight(); y++)
      {
        engine.setAge(x, y, 0);
      }
    }
  }
}
