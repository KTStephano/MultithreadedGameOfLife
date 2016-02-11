package cs351.lab4;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * This class represents the UI for the game. It performs input handling,
 * canvas resizing and rendering.
 *
 * @author Justin Hall
 */
public class GameUI
{
  private int windowWidth, windowHeight;
  private int canvasWidth, canvasHeight;
  private int zoom;
  private final int MIN_VIEW_OFFSET = 0;
  private int viewXOffset = MIN_VIEW_OFFSET, viewYOffset = MIN_VIEW_OFFSET;
  private int prevX, prevY;
  private final int STANDARD_BUTTON_SPACING = 10;
  private final Stage STAGE;
  private final Stage SETTINGS_STAGE = new Stage();
  private Canvas canvas;
  private final HBox BUTTON_ROW_HORIZONTAL = new HBox();
  private final SimulationEngine ENGINE;
  private final ObservableList<World> PRESETS;
  private Color[] availableColors = null;
  private volatile boolean needsUpdate = true;
  private boolean mouseDragged = false;
  // holds all of the presets after the UI is initialized
  private final ListView<World> PRESET_LIST = new ListView<>();

  /**
   * Takes a set of initial parameters to use to set up the UI.
   *
   * @param engine SimulationEngine object for callbacks
   * @param title title to use for the main window
   * @param presets list of presets (World objects) to use to populate the preset panel
   * @param stage Stage object representing the main window
   * @param width width to use for the window
   * @param height height to use for the window
   * @param zoom starting zoom (lower values being further away)
   */
  public GameUI(SimulationEngine engine, String title, ObservableList<World> presets, Stage stage, int width, int height, int zoom)
  {
    ENGINE = engine;
    PRESETS = presets;
    this.zoom = zoom;

    STAGE = stage;
    STAGE.setTitle(title);
    STAGE.setWidth(width);
    STAGE.setHeight(height);
    STAGE.setWidth(width);
    STAGE.setHeight(height);
    STAGE.setOnCloseRequest((e) -> signalClose());

    BUTTON_ROW_HORIZONTAL.setSpacing(STANDARD_BUTTON_SPACING);
    BorderPane layout = new BorderPane();
    Group root = new Group();
    canvas = new Canvas(width, height);
    layout.setCenter(canvas);
    layout.setTop(BUTTON_ROW_HORIZONTAL);
    root.getChildren().add(layout);
    STAGE.setScene(new Scene(root, width, height, Color.WHITE));
    initButtons();

    canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressedDown);
    canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::mousePressedAndReleased);
    canvas.addEventHandler(ScrollEvent.SCROLL, this::mouseScroll);
    canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);

    adjustWindowDimensions();
    adjustViewOffsetsToZoom();
    stage.show();
  }

  /**
   * This is the main entry point for this class and should be called as much as
   * possible (up to screen refresh rate) for the UI to have a responsive feel.
   */
  public void update()
  {
    // if prevFrameComplete is true at the start of this, the engine has already
    // swapped its buffers so that the renderer will see the latest version and it
    // can stop updating after this rendering call
    boolean prevFrameComplete = ENGINE.previousFrameCompleted();
    adjustWindowDimensions();
    adjustViewOffsetsToZoom();
    render(canvas.getGraphicsContext2D());
    setNeedsUpdate(!prevFrameComplete);
  }

  /**
   * Makes sure that the main window and the settings window are close so that
   * the program can exit cleanly.
   */
  public void signalClose()
  {
    SETTINGS_STAGE.close();
    STAGE.close();
  }

  /**
   * This should be called before the first call to update.
   *
   * @param colors array of 11 elements representing the different colors to use
   *               for the different ages with element 0 being for dead cells
   *               and element 10 being for the oldest of cells
   */
  public void setRenderingColorsBasedOnAge(Color[] colors)
  {
    availableColors = colors;
  }

  /**
   * If the windowWidth/windowHeight variables are out of sync with the main window,
   * a resize event has occurred. This function then recalculates the canvas width and height
   * and sets the needsUpdate flag so that the renderer will update the screen at least once afterwards.
   */
  private void adjustWindowDimensions()
  {
    if (windowWidth != (int)STAGE.getWidth() || windowHeight != (int)STAGE.getHeight())
    {
      // Hard-coded insets: was not able to get a better solution working in time. They
      // are used to calculate the canvas width/height.
      int xStart = 8;
      int xEnd = 8;

      int yStart = 31;
      int yEnd = 31;

      windowWidth = (int) STAGE.getWidth();
      windowHeight = (int) STAGE.getHeight();
      canvasWidth = windowWidth - (xStart + xEnd);
      canvasHeight = windowHeight - (yStart + yEnd);
      canvas.setWidth(canvasWidth);
      canvas.setHeight(canvasHeight);
      setNeedsUpdate(true);
    }
  }

  /**
   * Takes a GraphicsContext object and tries to render the current state
   * of the simulation. If the zoom is 5 and up, grid lines are drawn. If not
   * it just draws the cells with their current age/death color.
   *
   * @param context GraphicsContext object to use for draw calls
   */
  private void render(GraphicsContext context)
  {
    if (ENGINE.isPaused() && !needsUpdate) return;
    else if (availableColors == null) return;
    context.setFill(Color.WHITE);
    context.fillRect(0, 0, windowWidth, windowHeight);
    context.setFill(Color.BLACK);
    ENGINE.lock();
    try
    {
      int numCellsX = windowWidth / zoom;
      int numCellsY = windowHeight / zoom;
      int totalWidth = viewXOffset + numCellsX < ENGINE.getWorldWidth() ? viewXOffset + numCellsX : ENGINE.getWorldWidth();
      int totalHeight = viewYOffset + numCellsY < ENGINE.getWorldHeight() ? viewYOffset + numCellsY : ENGINE.getWorldHeight();
      for (int x = viewXOffset; x < totalWidth; x++)
      {
        for (int y = viewYOffset; y < totalHeight; y++)
        {
          int currX = (x - viewXOffset) * zoom - 1;
          int currY = (y - viewYOffset) * zoom - 1;
          int age = ENGINE.getAge(x, y);
          if (zoom >= 5)
          {
            context.setFill(Color.GRAY);
            context.fillRect(currX, currY, zoom, zoom);
            context.setFill(availableColors[age]);
            context.fillRect(currX + 1, currY + 1, zoom - 1, zoom - 1);
          }
          else if (age > 0)
          {
            context.setFill(availableColors[age]);
            context.fillRect(currX, currY, zoom, zoom);
          }
        }
      }
    }
    finally
    {
      ENGINE.unlock();
    }
  }

  /**
   * Sets up the play, pause, next, reset and settings buttons and then initializes
   * the settings panel by calling initSettings().
   */
  private void initButtons()
  {
    final Button PLAY = addButton("Play");
    final Button PAUSE = addButton("Pause");
    PAUSE.setDisable(true);
    final Button NEXT = addButton("Next");
    final Button RESET = addButton("Reset");
    final Button SETTINGS = addButton("Settings");
    initSettings(SETTINGS);

    PLAY.setOnAction((e) ->
    {
      SETTINGS_STAGE.hide();
      PLAY.setDisable(true);
      PAUSE.setDisable(false);
      NEXT.setDisable(true);
      RESET.setDisable(true);
      SETTINGS.setDisable(true);
      ENGINE.togglePause(false, true);
    });
    PAUSE.setOnAction((e) ->
    {
      PAUSE.setDisable(true);
      PLAY.setDisable(false);
      NEXT.setDisable(false);
      RESET.setDisable(false);
      SETTINGS.setDisable(false);
      ENGINE.togglePause(true, false);
    });
    NEXT.setOnAction((e) ->
    {
      SETTINGS_STAGE.hide();
      ENGINE.togglePause(false, true);
      ENGINE.togglePause(true, false);
      setNeedsUpdate(true);
    });
    RESET.setOnAction((e) ->
    {
      SETTINGS_STAGE.hide();
      PRESET_LIST.getSelectionModel().getSelectedItem().initEngine();
      setNeedsUpdate(true);
    });
  }

  /**
   * Creates the settings window and its labels/button/preset list so the
   * user can change the settings for the simulation.
   *
   * @param settings settings button
   */
  private void initSettings(Button settings)
  {
    final int WIDTH = 250;
    final int HEIGHT = 265;
    SETTINGS_STAGE.setTitle("Settings");
    SETTINGS_STAGE.setWidth(WIDTH);
    SETTINGS_STAGE.setHeight(HEIGHT);
    SETTINGS_STAGE.setResizable(false);

    final Label THREAD_LABEL = new Label("Threads ");
    final TextField THREAD_TEXT = new TextField();
    PRESET_LIST.setItems(PRESETS);
    PRESET_LIST.getSelectionModel().select(0); // set the default selection to the first element
    HBox threadRow = new HBox();
    VBox settingsCol = new VBox();
    settingsCol.setSpacing(STANDARD_BUTTON_SPACING);
    threadRow.getChildren().addAll(THREAD_LABEL, THREAD_TEXT);
    settingsCol.getChildren().addAll(threadRow, new Label("Presets"), PRESET_LIST);

    final Button apply = new Button("Apply");
    HBox applyButton = new HBox();
    HBox.setHgrow(apply, Priority.ALWAYS);
    apply.setMaxWidth(SETTINGS_STAGE.getWidth() - 6);
    applyButton.getChildren().addAll(apply);

    BorderPane layout = new BorderPane();
    layout.setPrefWidth(STAGE.getWidth());
    layout.setPrefHeight(STAGE.getHeight() - 20);
    Group root = new Group();
    layout.setTop(applyButton);
    layout.setCenter(settingsCol);
    root.getChildren().add(layout);
    SETTINGS_STAGE.setScene(new Scene(root, Color.WHITE));

    settings.setOnAction((e) ->
    {
      THREAD_TEXT.setText(Integer.toString(ENGINE.getNumThreads()));
      SETTINGS_STAGE.hide();
      SETTINGS_STAGE.show();
    });
    apply.setOnAction((e) ->
    {
      int newNumThreads = 0;
      SETTINGS_STAGE.hide();
      try
      {
        newNumThreads = Integer.parseInt(THREAD_TEXT.getText());
        if (newNumThreads > 8) return;
      }
      catch (NumberFormatException ex)
      {
        return;
      }
      STAGE.hide();
      if (!ENGINE.isPaused()) ENGINE.togglePause(true, false);
      ENGINE.shutdown();
      ENGINE.init(newNumThreads);
      PRESET_LIST.getSelectionModel().getSelectedItem().initEngine();
      setNeedsUpdate(true);
      STAGE.show();
    });
    PRESET_LIST.getSelectionModel().selectedItemProperty().addListener((value, oldVal, newVal) ->
    {
      System.out.println("-> Loading preset: " + value.getValue());
      viewXOffset = MIN_VIEW_OFFSET;
      viewYOffset = MIN_VIEW_OFFSET;
      value.getValue().initEngine();
      setNeedsUpdate(true);
    });
  }

  /**
   * Adds a button to the BUTTON_ROW_HORIZONTAL HBox which is set to sit along
   * the top of the window.
   *
   * @param text text for the button
   * @return reference to the newly-created button
   */
  private Button addButton(String text)
  {
    Button button = new Button(text);
    HBox.setHgrow(button, Priority.ALWAYS);
    button.setMaxWidth(Double.MAX_VALUE);
    BUTTON_ROW_HORIZONTAL.getChildren().add(button);
    return button;
  }

  /**
   * When the mouse is pressed down (but not released), this function is called
   * and sets prevX/prevY so that the mouse drag function can use them to calculate
   * the change in cursor position between frames.
   *
   * @param e generated mouse event
   */
  private void mousePressedDown(MouseEvent e)
  {
    mouseDragged = false; // flag used by mousePressedAndReleased
    prevX = (int)e.getX();
    prevY = (int)e.getY();
    setNeedsUpdate(true);
  }

  /**
   * This is called when the button is pressed and released. This is separated
   * from mousePressedDown so that the user does not unintentionally toggle
   * the life of one of the cells when they wanted to perform a drag motion.
   *
   * @param e generated mouse event
   */
  private void mousePressedAndReleased(MouseEvent e)
  {
    if (mouseDragged) return;
    ENGINE.lock();
    try
    {
      int x = (viewXOffset * zoom + (int)e.getX()) / zoom;
      int y = (viewYOffset * zoom + (int)e.getY()) / zoom;
      if (x < 0 || x >= ENGINE.getWorldWidth() || y < 0 || y >= ENGINE.getWorldHeight()) return;
      ENGINE.setAge(x, y, ENGINE.getAge(x, y) > 0 ? 0 : 1);
    }
    finally
    {
      ENGINE.unlock();
    }
    setNeedsUpdate(true);
  }

  /**
   * When the mouse is dragged this function is called and the viewX/YOffset variables
   * are updated and then adjusted if necessary to prevent them from going past
   * the edge of the grid.
   *
   * @param e generated mouse event
   */
  private void mouseDragged(MouseEvent e)
  {
    mouseDragged = true;
    viewXOffset += prevX - (int)e.getX();
    viewYOffset += prevY - (int)e.getY();
    adjustViewOffsetsToZoom();
    prevX = (int)e.getX();
    prevY = (int)e.getY();
    setNeedsUpdate(true);
  }

  /**
   * Whenever the mouse wheel is used, this function adjusts
   * the zoom and viewing offsets so the user can zoom in and out.
   *
   * @param e generated mouse event
   */
  private void mouseScroll(ScrollEvent e)
  {
    final int MAX_ZOOM = 50;
    final int MIN_ZOOM = 1;
    int scrollAmnt = (int)(e.getDeltaY() / e.getDeltaY());
    if (e.getDeltaY() < 0) scrollAmnt *= -1;
    zoom += scrollAmnt;
    if (zoom > MAX_ZOOM) zoom = MAX_ZOOM;
    else if (zoom < MIN_ZOOM) zoom = MIN_ZOOM;
    adjustViewOffsetsToZoom();
    setNeedsUpdate(true);
  }

  /**
   * This function performs bounds checking to make sure the user doesn't drag
   * off the edge of the board.
   */
  private void adjustViewOffsetsToZoom()
  {
    final int MAX_VIEWX_OFFSET = (ENGINE.getWorldWidth() - canvasWidth / zoom);
    final int MAX_VIEWY_OFFSET = (ENGINE.getWorldHeight() - canvasHeight / zoom);
    if (viewXOffset < 0) viewXOffset = MIN_VIEW_OFFSET;
    if (viewYOffset < 0) viewYOffset = MIN_VIEW_OFFSET;
    if (viewXOffset > MAX_VIEWX_OFFSET) viewXOffset = MAX_VIEWX_OFFSET;
    if (viewYOffset > MAX_VIEWY_OFFSET) viewYOffset = MAX_VIEWY_OFFSET;
  }

  /**
   * Setter function for the needsUpdate flag.
   *
   * @param val true if the renderer needs to redraw the screen and false if not
   */
  private void setNeedsUpdate(boolean val)
  {
    needsUpdate = val;
  }
}