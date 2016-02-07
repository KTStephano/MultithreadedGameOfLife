package cs351.lab4;

import cs351.presets.Preset;
import java.util.Random;

public class World
{
  private final SimulationEngine ENGINE;
  private final Preset PRESET;
  private String id;

  public World(String id, Preset preset, SimulationEngine engine)
  {
    ENGINE = engine;
    this.id = id;
    PRESET = preset;
  }

  @Override
  public String toString()
  {
    return id;
  }

  public void initEngine()
  {
    ENGINE.lock();
    try
    {
      PRESET.setInitialEngineState(ENGINE);
    }
    finally
    {
      ENGINE.unlock();
    }
    ENGINE.togglePause(false, false);
    ENGINE.togglePause(true, false);
  }
}
