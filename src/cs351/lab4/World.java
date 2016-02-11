package cs351.lab4;

import cs351.presets.Preset;

/**
 * The World object can accept any instance of Preset. When initEngine is called
 * the given Preset is used to set the starting state of the board.
 *
 * @author Justin Hall
 */
public class World
{
  private final SimulationEngine ENGINE;
  private final Preset PRESET;
  private String id;

  /**
   * World constructor.
   *
   * @param id id that is used when toString() is called (such as for UI elements)
   * @param preset Preset to use to initialize the engine
   * @param engine SimulationEngine object to use
   */
  public World(String id, Preset preset, SimulationEngine engine)
  {
    ENGINE = engine;
    this.id = id;
    PRESET = preset;
  }

  /**
   * Makes this class usable in print statements and things like UI elements.
   *
   * @return id given to this object in the constructor
   */
  @Override
  public String toString()
  {
    return id;
  }

  /**
   * Locks the given engine and uses its preset to reinitialize it. The given engine
   * should be paused before this function is called.
   */
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
  }
}
