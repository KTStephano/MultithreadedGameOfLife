package cs351.presets;

import cs351.lab4.SimulationEngine;

public class GliderGun implements Preset
{

  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {

  }

  private void spawnGlider(SimulationEngine engine, int startX, int startY)
  {
    engine.setAge(startX, startY, 1);
    engine.setAge(startX + 1, startY + 1, 1);
    for (int x = startX + 1; x < startX - 1; --x)
    {
      engine.setAge(x, startY + 1, 1);
    }
  }
}
