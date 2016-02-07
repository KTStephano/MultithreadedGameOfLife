package cs351.presets;

import cs351.lab4.SimulationEngine;

import java.util.Random;

public class RandomGrid implements Preset
{
  private final Random RAND = new Random();

  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    for (int x = 0; x < engine.getWorldWidth(); x++)
    {
      for (int y = 0; y < engine.getWorldHeight(); y++)
      {
        if (RAND.nextInt(100) > 50) engine.setAge(x, y, (byte)1);
        else engine.setAge(x, y, (byte)0);
      }
    }
  }
}
