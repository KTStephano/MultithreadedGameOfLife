package cs351.lab4;

import cs351.presets.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Main class for the simulation. It starts up the SimulationEngine and the GameUI
 * and then serves as the main loop that calls the GameUI's update function.
 *
 * @author Justin Hall
 */
public class GameOfLife extends Application
{
  private final int GRID_SIZE = 10_000;
  private final SimulationEngine ENGINE = new SimulationEngine(GRID_SIZE, GRID_SIZE);
  private GameUI userInterface;

  /**
   * Class representing the UI loop - each frame the GameUI's update function
   * is called which takes care of input management/canvas resizing/rendering.
   *
   * @author Justin Hall
   */
  private class MainUILoop extends AnimationTimer
  {
    @Override
    public void handle(long now)
    {
      userInterface.update();
    }
  }

  /**
   * Starts the SimulationEngine with NUM_THREADS number of threads. It then instantiates the
   * World objects which represent some method for setting the initial state of the simulation
   * and passes them to the UI to show to the user for selection.
   *
   * @param primaryStage Stage object which is given to the GameUI
   */
  @Override
  public void start(final Stage primaryStage)
  {
    final int WIDTH = 700, HEIGHT = 700, ZOOM = 5;
    final int NUM_THREADS = 8;
    ENGINE.init(NUM_THREADS);
    World[] worlds = { new World("Random", new RandomGrid(), ENGINE), new World("Blank", new BlankGrid(), ENGINE),
                       new World("Full (Minus Edges)", new FullGrid(), ENGINE), new World("Dancing Border", new DancingBorder(), ENGINE),
                       new World("Upper-Right Checkered", new UpperRightCheckeredGrid(), ENGINE), new World("Glider Gun", new GliderGun(), ENGINE) };
    worlds[0].initEngine();
    final String WINDOW_TITLE = "Game of Life : 10K x 10K";
    userInterface = new GameUI(ENGINE, WINDOW_TITLE, FXCollections.observableArrayList(worlds), primaryStage, WIDTH, HEIGHT, ZOOM);
    userInterface.setRenderingColorsBasedOnAge(calculateAgeColors());

    new MainUILoop().start();
  }

  /**
   * Shuts down the SimulationEngine. This also stops the job system so that all active
   * worker threads can terminate.
   */
  @Override
  public void stop()
  {
    ENGINE.shutdown();
  }

  /**
   * Small function for calculating the colors of the different ages with the youngest
   * being the darkest color and the oldest being the lightest color.
   *
   * @return array with all age colors from 0-10 (0 being the color for dead cells)
   */
  private Color[] calculateAgeColors()
  {
    Color[] colors = new Color[11];
    Color deadColor = Color.WHITE;//new Color(1.0, 0.7, 0.7, 1.0);
    colors[0] = deadColor;
    double redYoungest = 0.0, greenYoungest = 0.0, blueYoungest = 0.0;
    double offset = 0.1;
    for (int i = 0; i < colors.length - 1; i++)
    {
      colors[i + 1] = new Color(redYoungest + i * offset, greenYoungest + i * offset, blueYoungest + i * offset, 1.0);
    }
    return colors;
  }

  /**
   * Entry.
   *
   * @param args command line arguments
   */
  public static void main(String[] args)
  {
    launch(args);
  }
}
