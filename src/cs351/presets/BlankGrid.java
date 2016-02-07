package cs351.presets;

import cs351.lab4.SimulationEngine;

public class BlankGrid implements Preset
{
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
