package cs351.presets;

import cs351.lab4.SimulationEngine;

public class DancingBorder implements Preset
{
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

  private void drawRow(SimulationEngine engine, int startX, int y)
  {
    final int ALIVE_CELLS_PER_ROW = 8;
    final int SPACING_BETWEEN_ALIVE_CELLS = 1;
    final int SPACING_BETWEEN_BLOCKS = 2;
    int aliveCellsSet = 0;

    for (int x = startX; x < engine.getWorldWidth(); x += SPACING_BETWEEN_ALIVE_CELLS)
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
