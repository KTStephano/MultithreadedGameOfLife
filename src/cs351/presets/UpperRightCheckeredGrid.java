package cs351.presets;

import cs351.lab4.SimulationEngine;

/**
 * Creates the checkerboard-like starting state for the engine.
 *
 * @author Justin Hall
 */
public class UpperRightCheckeredGrid implements Preset
{

  /**
   * Walks along the diagonal starting from x=0, y=0 until x=engine.getWorldWidth(),
   * y=engine.getWorldWidth() and calls drawDiagonalUpwardFromStartX.
   *
   * @param engine SimulationEngine object to use.
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    new BlankGrid().setInitialEngineState(engine);
    int currDiagonal = 0;
    final int ALIVE_CELLS_PER_BLOCK = 8;
    int aliveCells = 0;
    while (currDiagonal < engine.getWorldWidth())
    {
      aliveCells++;
      drawDiagonalUpwardFromStartX(engine, currDiagonal, currDiagonal);
      currDiagonal++;
      if (aliveCells >= ALIVE_CELLS_PER_BLOCK)
      {
        aliveCells = 0;
        // skip ahead by an extra one along the diagonal
        currDiagonal++;
      }
    }
  }

  /**
   * From the given startX, startY, it will draw a diagonal line upwards until it
   * reaches the bounds of the grid.
   *
   * @param engine SimulationEngine object to use
   * @param startX x-value to start with
   * @param startY y-value to start with
   */
  private void drawDiagonalUpwardFromStartX(SimulationEngine engine, int startX, int startY)
  {
    while (startX < engine.getWorldWidth() && startY >= 0)
    {
      engine.setAge(startX, startY, 1);
      startX++;
      startY--;
    }
  }
}
