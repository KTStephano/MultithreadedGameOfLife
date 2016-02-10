package cs351.lab4;

import cs351.presets.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameOfLife extends Application
{
  private final int GRID_SIZE = 10_000;
  private final SimulationEngine ENGINE = new SimulationEngine(GRID_SIZE, GRID_SIZE);
  private GameUI userInterface;

  private final long[] frameTimes = new long[100];
  private int frameTimeIndex = 0 ;
  private boolean arrayFilled = false ;

  private class MainGameLoop extends AnimationTimer
  {
    @Override
    public void handle(long now)
    {
      userInterface.update();

      long oldFrameTime = frameTimes[frameTimeIndex] ;
      frameTimes[frameTimeIndex] = now ;
      frameTimeIndex = (frameTimeIndex + 1) % frameTimes.length ;
      if (frameTimeIndex == 0) {
        arrayFilled = true ;
      }
      if (arrayFilled) {
        long elapsedNanos = now - oldFrameTime ;
        long elapsedNanosPerFrame = elapsedNanos / frameTimes.length ;
        double frameRate = 1_000_000_000.0 / elapsedNanosPerFrame ;
        //System.out.println((String.format("Current frame rate: %.3f", frameRate)));
      }
    }
  }

  @Override
  public void start(final Stage primaryStage)
  {
    final int WIDTH = 700, HEIGHT = 700, ZOOM = 5;
    ENGINE.init(Runtime.getRuntime().availableProcessors());
    World[] worlds = { new World("Random", new RandomGrid(), ENGINE), new World("Blank", new BlankGrid(), ENGINE),
                       new World("Full (Minus Edges)", new FullGrid(), ENGINE), new World("Dancing Border", new DancingBorder(), ENGINE),
                       new World("Upper-Right Checkered", new UpperRightCheckeredGrid(), ENGINE)};
    worlds[0].initEngine();
    final String WINDOW_TITLE = "Game of Life : 10K x 10K";
    userInterface = new GameUI(ENGINE, WINDOW_TITLE, FXCollections.observableArrayList(worlds), primaryStage, WIDTH, HEIGHT, ZOOM);
    userInterface.setRenderingColorsBasedOnAge(calculateAgeColors());

    new MainGameLoop().start();
  }

  @Override
  public void stop()
  {
    ENGINE.shutdown();
  }

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

  public static void main(String[] args)
  {
    launch(args);
  }
}
