package cs351.presets;

import cs351.lab4.SimulationEngine;

/**
 * Initializes the dancing border preset. Meant to be given to a World object.
 *
 * @author Justin Hall
 */
public class DancingBorder implements Preset
{
  /**
   * For each y value, drawRow is called and the startX variable
   * is decremented.
   *
   * @param engine SimulationEngine object to use
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    int startX = 0;
    new BlankGrid().setInitialEngineState(engine);
    for (int y = 0; y < engine.getWorldHeight(); y++)
    {
      drawRow(engine, startX, y);
      --startX;
    }
  }

  /**
   * Draws a row across the board of cells where each block has 8
   * alive cells and is 2 cells away from the previous block.
   *
   * @param engine SimulationEngine object to use
   * @param startX x-value to start with (incremented each loop)
   * @param y y-value that remains constant
   */
  private void drawRow(SimulationEngine engine, int startX, int y)
  {
    final int ALIVE_CELLS_PER_ROW = 8;
    final int SPACING_BETWEEN_BLOCKS = 2;
    int aliveCellsSet = 0;

    for (int x = startX; x < engine.getWorldWidth(); x++)
    {
      ++aliveCellsSet;
      if (aliveCellsSet >= ALIVE_CELLS_PER_ROW)
      {
        x += SPACING_BETWEEN_BLOCKS;
        aliveCellsSet = 0;
      }
      if (!engine.isValid(x, y) || x >= engine.getWorldWidth()) continue;
      engine.setAge(x, y, 1);
    }
  }
}
