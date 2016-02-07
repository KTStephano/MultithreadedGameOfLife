package cs351.presets;

import cs351.lab4.SimulationEngine;

public class FullGrid implements Preset
{
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
