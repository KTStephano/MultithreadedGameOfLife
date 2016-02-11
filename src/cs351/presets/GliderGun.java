package cs351.presets;

import cs351.lab4.SimulationEngine;

/**
 * Creates diagonals of gliders that stop at the main diagonal
 * of the grid. They move from upper left to lower right.
 *
 * @author Justin Hall
 */
public class GliderGun implements Preset
{
  private final int DIAGONAL_OFFSET = 5;

  /**
   * Runs across the diagonal, calling spawnGlider at each step.
   *
   * @param engine Simulation engine object to use
   */
  @Override
  public void setInitialEngineState(SimulationEngine engine)
  {
    int currDiagonalX = engine.getWorldWidth() - 3;
    int currDiagonalY = 0;
    new BlankGrid().setInitialEngineState(engine);

    while (currDiagonalX >= DIAGONAL_OFFSET && currDiagonalY < engine.getWorldHeight() - DIAGONAL_OFFSET)
    {
      spawnGliders(engine, currDiagonalX, currDiagonalY);
      currDiagonalX -= DIAGONAL_OFFSET;
      currDiagonalY += DIAGONAL_OFFSET;
    }
  }

  /**
   * Given a startX and startY, it walks in a diagonal path and adds
   * gliders at each step until it reaches DIAGONAL_OFFSET in the x/y directions.
   *
   * @param engine SimulationEngine object to use
   * @param startX x-value to start with
   * @param startY y-value to start with
   */
  private void spawnGliders(SimulationEngine engine, int startX, int startY)
  {
    while (startX >= DIAGONAL_OFFSET && startY >= DIAGONAL_OFFSET)
    {
      engine.setAge(startX, startY, 1);
      engine.setAge(startX + 1, startY + 1, 1);
      for (int x = startX + 1; x > startX - 2; --x)
      {
        engine.setAge(x, startY + 2, 1);
      }
      startX -= DIAGONAL_OFFSET;
      startY -= DIAGONAL_OFFSET;
    }
  }
}
