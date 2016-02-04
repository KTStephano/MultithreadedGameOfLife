package cs351.lab4;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameOfLife extends Application
{
  private int width = 700, height = 700;
  private GraphicsContext context;
  private final SimulationEngine ENGINE = new SimulationEngine();
  private int zoom = 5;
  private int xOffset = 0, yOffset = 0;
  private int prevX, prevY;

  private class MainGameLoop extends AnimationTimer
  {
    @Override
    public void handle(long now) {
      context.setFill(Color.WHITE);
      context.fillRect(0, 0, width, height);
      context.setFill(Color.BLACK);
      if (!ENGINE.toggleRenderMode(true)) return;
      try
      {
        boolean[][] grid = ENGINE.getFrontBuffer();
        if (grid == null) return;
        int numCellsX = width / zoom;
        int numCellsY = height / zoom;
        int totalWidth = xOffset + numCellsX < grid.length - 1 ? xOffset + numCellsX : grid.length - 1;
        int totalHeight = yOffset + numCellsY < grid[0].length - 1 ? yOffset + numCellsY : grid[0].length - 1;
        for (int x = xOffset; x < totalWidth; x++)
        {
          for (int y = yOffset; y < totalHeight; y++)
          {
            int currX = (x - xOffset) * zoom - 1;
            int currY = (y - yOffset) * zoom - 1;
            if (zoom > 5)
            {
              context.fillRect(currX, currY, zoom, zoom);
              context.setFill(Color.WHITE);
              context.fillRect(currX + 2, currY + 2, zoom - 2, zoom -2);
              context.setFill(Color.BLACK);
            }
            if (grid[x][y]) context.fillRect(currX, currY, zoom, zoom);
          }
        }
      }
      finally
      {
        ENGINE.toggleRenderMode(false);
      }
    }
  }

  @Override
  public void start(final Stage primaryStage)
  {
    ENGINE.start(10_000, 10_000, 8);
    new World(10_000, 10_000, ENGINE).initEngine();
    final String WINDOW_TITLE = "Game of Life : 10K x 10K";
    primaryStage.setTitle(WINDOW_TITLE);
    Group root = new Group();
    Canvas canvas = new Canvas(width, height);
    root.getChildren().add(canvas);
    context = canvas.getGraphicsContext2D();

    Scene scene = new Scene(root, Color.BLACK);
    primaryStage.setScene(scene);
    primaryStage.show();

    canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mouseClicked);
    canvas.addEventHandler(ScrollEvent.SCROLL, this::mouseScroll);
    canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);
    canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);

    new MainGameLoop().start();
  }

  private void mouseClicked(MouseEvent e)
  {
    prevX = (int)e.getX();
    prevY = (int)e.getY();
  }

  /**
   * Calls mouseClicked while the user is dragging the mouse.
   *
   * @param e the generated Mouse from the mouse drag
   */
  private void mouseDragged(MouseEvent e)
  {
    xOffset += prevX - (int)e.getX();
    yOffset += prevY - (int)e.getY();
    if (xOffset < 1) xOffset = 1;
    if (yOffset < 1) yOffset = 1;
    prevX = (int)e.getX();
    prevY = (int)e.getY();
  }

  private void mouseScroll(ScrollEvent e)
  {
    final int MAX_ZOOM = 50;
    final int MIN_ZOOM = 1;
    int scrollAmnt = (int)(e.getDeltaY() / e.getDeltaY());
    if (e.getDeltaY() < 0) scrollAmnt *= -1;
    zoom += scrollAmnt;
    if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;
    else if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
  }

  /**
   * Resets the two booleans regardless of which was was actually true.
   *
   * @param e the generated Mouse from the button release
   */
  private void mouseReleased(MouseEvent e)
  {
  }

  public static void main(String[] args)
  {
    launch(args);
    System.exit(1);
  }
}
