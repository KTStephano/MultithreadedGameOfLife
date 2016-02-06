package cs351.lab4;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.LinkedList;

public class GameUI
{
  private int width, height;
  private int zoom;
  private int viewXOffset = 1, viewYOffset = 1;
  private int prevX, prevY;
  private int standardButtonWidth = 0, standardButtonHeight = 50;
  private final Stage STAGE;
  private Canvas Canvas;
  private final HBox BUTTON_ROW_HORIZONTAL = new HBox();
  private final SimulationEngine ENGINE;
  private LinkedList<Button> buttons;
  private boolean needsUpdate = true;

  public GameUI(SimulationEngine engine, String title, Stage stage, int width, int height, int zoom)
  {
    ENGINE = engine;
    this.width = width;
    this.height = height;
    this.zoom = zoom;
    buttons = new LinkedList<>();

    STAGE = stage;
    STAGE.setTitle(title);
    STAGE.setWidth(this.width);
    STAGE.setHeight(this.height);

    BorderPane layout = new BorderPane();
    Group root = new Group();
    Canvas = new Canvas(width, height);
    layout.setCenter(Canvas);
    layout.setTop(BUTTON_ROW_HORIZONTAL);
    root.getChildren().add(layout);
    STAGE.setScene(new Scene(root, Color.BLACK));
    initButtons();

    Canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mouseClicked);
    Canvas.addEventHandler(ScrollEvent.SCROLL, this::mouseScroll);
    Canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);

    stage.show();
  }

  public Button addButton(String text)
  {
    final int pixelMultiplier = 5;
    Button button = new Button(text);
    buttons.add(button);
    if (text.length() * pixelMultiplier > standardButtonWidth) standardButtonWidth = text.length() * pixelMultiplier;
    adjustButtonsToScreenWidth();
    HBox.setHgrow(button, Priority.ALWAYS);
    button.setMaxWidth(Double.MAX_VALUE);
    BUTTON_ROW_HORIZONTAL.getChildren().add(button);
    return button;
  }

  public void update()
  {
    adjustWindowDimensions();
    render(Canvas.getGraphicsContext2D());
    setNeedsUpdate(false);
  }

  private void adjustButtonsToScreenWidth()
  {
    while (standardButtonWidth * buttons.size() > STAGE.getWidth() && standardButtonWidth > 1) --standardButtonWidth;
    for (Button button : buttons) button.setPrefWidth(standardButtonWidth);
  }

  private void adjustWindowDimensions()
  {
    if (width != (int)STAGE.getWidth() || height != (int)STAGE.getHeight())
    {
      width = (int) STAGE.getWidth();
      height = (int) STAGE.getHeight();
      Canvas.setWidth(width);
      Canvas.setHeight(height);
      setNeedsUpdate(true);
    }
  }

  private void render(GraphicsContext context)
  {
    if (ENGINE.isPaused() && !needsUpdate) return;
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
      int totalWidth = viewXOffset + numCellsX < grid.length - 1 ? viewXOffset + numCellsX : grid.length - 1;
      int totalHeight = viewYOffset + numCellsY < grid[0].length - 1 ? viewYOffset + numCellsY : grid[0].length - 1;
      for (int x = viewXOffset; x < totalWidth; x++)
      {
        for (int y = viewYOffset; y < totalHeight; y++)
        {
          int currX = (x - viewXOffset) * zoom - 1;
          int currY = (y - viewYOffset) * zoom - 1;
          if (grid[x][y]) context.fillRect(currX, currY, zoom, zoom);
          else if (zoom > 5)
          {
            context.fillRect(currX, currY, zoom, zoom);
            context.setFill(Color.WHITE);
            context.fillRect(currX + 2, currY + 2, zoom - 2, zoom -2);
            context.setFill(Color.BLACK);
          }
        }
      }
    }
    finally
    {
      ENGINE.toggleRenderMode(false);
    }
  }

  private void initButtons()
  {
    final Button play = addButton("Play");
    final Button pause = addButton("Pause");
    pause.setDisable(true);
    final Button next = addButton("Next");
    addButton("Reset");

    play.setOnAction((e) ->
    {
      play.setDisable(true);
      pause.setDisable(false);
      next.setDisable(true);
      ENGINE.togglePause(false);
    });
    pause.setOnAction((e) ->
    {
      pause.setDisable(true);
      play.setDisable(false);
      next.setDisable(false);
      ENGINE.togglePause(true);
    });
    next.setOnAction((e) ->
    {
      ENGINE.togglePause(false);
      ENGINE.togglePause(true);
      setNeedsUpdate(true);
    });
  }

  private void mouseClicked(MouseEvent e)
  {
    prevX = (int)e.getX();
    prevY = (int)e.getY();
    setNeedsUpdate(true);
    System.out.println(prevX + " " + prevY);
    ENGINE.set(prevX/zoom, prevY/zoom, !ENGINE.get(prevX/zoom, prevY/zoom));
  }

  /**
   * Calls mouseClicked while the user is dragging the mouse.
   *
   * @param e the generated Mouse from the mouse drag
   */
  private void mouseDragged(MouseEvent e)
  {
    viewXOffset += prevX - (int)e.getX();
    viewYOffset += prevY - (int)e.getY();
    if (viewXOffset < 1) viewXOffset = 1;
    if (viewYOffset < 1) viewYOffset = 1;
    prevX = (int)e.getX();
    prevY = (int)e.getY();
    setNeedsUpdate(true);
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
    setNeedsUpdate(true);
  }

  private void setNeedsUpdate(boolean val)
  {
    needsUpdate = val;
  }
}