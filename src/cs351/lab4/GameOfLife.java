package cs351.lab4;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;

public class GameOfLife extends Application
{
  private int width = 700, height = 700;
  private final SimulationEngine ENGINE = new SimulationEngine();
  private GameUI userInterface;
  private int zoom = 5;

  private class MainGameLoop extends AnimationTimer
  {
    @Override
    public void handle(long now)
    {
      userInterface.update();
    }
  }

  @Override
  public void start(final Stage primaryStage)
  {
    ENGINE.init(10_000, 10_000, 8);
    new World(10_000, 10_000, ENGINE).initEngine();
    final String WINDOW_TITLE = "Game of Life : 10K x 10K";
    userInterface = new GameUI(ENGINE, WINDOW_TITLE, primaryStage, width, height, zoom);

    new MainGameLoop().start();
  }

  @Override
  public void stop()
  {
    ENGINE.shutdown();
  }

  public static void main(String[] args)
  {
    launch(args);
    //System.exit(0);
  }
}
