package cs351.lab4;

import java.util.Random;

public class World
{
  private final int WIDTH, HEIGHT;
  private final SimulationEngine ENGINE;
  private final Random RAND = new Random();

  public World(int width, int height, SimulationEngine engine)
  {
    WIDTH = width;
    HEIGHT = height;
    ENGINE = engine;
  }

  public int getWidth()
  {
    return WIDTH;
  }

  public int getHeight()
  {
    return HEIGHT;
  }

  public void initEngine()
  {
    for (int x = 0; x < WIDTH; x++)
    {
      for (int y = 0; y < HEIGHT; y++)
      {
        int num = RAND.nextInt(100);
        if (num > 50) ENGINE.set(x, y, true);
        else ENGINE.set(x, y, false);
      }
    }
    ENGINE.runFrame();
  }
}
