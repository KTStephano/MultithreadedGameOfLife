package cs351.presets;

import cs351.lab4.SimulationEngine;

public class UpperRightCheckeredGrid implements Preset
{
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
      drawDiagonalDownwardFromStartX(engine, currDiagonal, currDiagonal);
      currDiagonal++;
      if (aliveCells >= ALIVE_CELLS_PER_BLOCK)
      {
        aliveCells = 0;
        currDiagonal++;
      }
    }
  }

  private void drawDiagonalDownwardFromStartX(SimulationEngine engine, int startX, int startY)
  {
    System.out.println(startX);
    while (startX < engine.getWorldWidth() && startY >= 0)
    {
      engine.setAge(startX, startY, 1);
      startX++;
      startY--;
    }
  }
}
