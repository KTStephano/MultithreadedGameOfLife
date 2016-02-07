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

public class GameUI
{
  private int width, height;
  private int zoom;
  private final int MIN_VIEW_OFFSET = 0;
  private int viewXOffset = MIN_VIEW_OFFSET, viewYOffset = MIN_VIEW_OFFSET;
  private int prevX, prevY;
  private final int STANDARD_BUTTON_SPACING = 10;
  private final Stage STAGE;
  private final Stage SETTINGS_STAGE = new Stage();
  private Canvas Canvas;
  private final HBox BUTTON_ROW_HORIZONTAL = new HBox();
  private final SimulationEngine ENGINE;
  private final ObservableList<World> PRESETS;
  private Color[] availableColors = null;
  private volatile boolean needsUpdate = true;
  private boolean mouseDragged = false;
  // holds all of the presets after the UI is initialized
  private final ListView<World> PRESET_LIST = new ListView<>();

  public GameUI(SimulationEngine engine, String title, ObservableList<World> presets, Stage stage, int width, int height, int zoom)
  {
    ENGINE = engine;
    PRESETS = presets;
    this.width = width;
    this.height = height;
    this.zoom = zoom;

    STAGE = stage;
    STAGE.setTitle(title);
    STAGE.setWidth(this.width);
    STAGE.setHeight(this.height);
    STAGE.setOnCloseRequest((e) -> signalClose());

    BUTTON_ROW_HORIZONTAL.setSpacing(STANDARD_BUTTON_SPACING);
    BorderPane layout = new BorderPane();
    Group root = new Group();
    Canvas = new Canvas(width, height);
    layout.setCenter(Canvas);
    layout.setTop(BUTTON_ROW_HORIZONTAL);
    root.getChildren().add(layout);
    STAGE.setScene(new Scene(root, Color.WHITE));
    initButtons();

    Canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressedDown);
    Canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::mousePressedAndReleased);
    Canvas.addEventHandler(ScrollEvent.SCROLL, this::mouseScroll);
    Canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::mouseDragged);

    stage.show();
  }

  public void update()
  {
    // if prevFrameComplete is true at the start of this, the engine has already
    // swapped its buffers so that the renderer will see the latest version and it
    // can stop updating after this rendering call
    boolean prevFrameComplete = ENGINE.previousFrameCompleted();
    adjustWindowDimensions();
    render(Canvas.getGraphicsContext2D());
    setNeedsUpdate(!prevFrameComplete);
  }

  public void signalClose()
  {
    SETTINGS_STAGE.close();
    STAGE.close();
  }

  public void setRenderingColorsBasedOnAge(Color[] colors)
  {
    availableColors = colors;
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
    else if (availableColors == null) return;
    context.setFill(Color.WHITE);
    context.fillRect(0, 0, width, height);
    context.setFill(Color.BLACK);
    ENGINE.lock();
    try
    {
      //byte[][] grid = ENGINE.getFrontBuffer();
      //if (grid == null) return;
      int numCellsX = width / zoom;
      int numCellsY = height / zoom;
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
            //context.setFill(Color.BLACK);
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

  private void initSettings(Button settings)
  {
    SETTINGS_STAGE.setTitle("Settings");
    SETTINGS_STAGE.setWidth(250);
    SETTINGS_STAGE.setHeight(250);
    SETTINGS_STAGE.setResizable(false);
    SETTINGS_STAGE.setAlwaysOnTop(true);

    final Label THREAD_LABEL = new Label("Threads ");
    final TextField THREAD_TEXT = new TextField();
    PRESET_LIST.setItems(PRESETS);
    PRESET_LIST.getSelectionModel().select(0); // set the default selection to the first element
    HBox threadRow = new HBox();
    VBox settingsCol = new VBox();
    settingsCol.setSpacing(STANDARD_BUTTON_SPACING);
    //threadRow.setSpacing(STANDARD_BUTTON_SPACING);
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
      SETTINGS_STAGE.hide();
      STAGE.hide();
      if (!ENGINE.isPaused()) ENGINE.togglePause(true, false);
      ENGINE.shutdown();
      ENGINE.init(Integer.parseInt(THREAD_TEXT.getText()));
      PRESET_LIST.getSelectionModel().getSelectedItem().initEngine();
      setNeedsUpdate(true);
      STAGE.show();
    });
    PRESET_LIST.getSelectionModel().selectedItemProperty().addListener((value, oldVal, newVal) ->
    {
      int numThreads = ENGINE.getNumThreads();
      ENGINE.shutdown();
      ENGINE.init(numThreads);
      value.getValue().initEngine();
      setNeedsUpdate(true);
    });
  }

  private Button addButton(String text)
  {
    Button button = new Button(text);
    HBox.setHgrow(button, Priority.ALWAYS);
    button.setMaxWidth(Double.MAX_VALUE);
    BUTTON_ROW_HORIZONTAL.getChildren().add(button);
    return button;
  }

  private void mousePressedDown(MouseEvent e)
  {
    mouseDragged = false; // flag used by mousePressedAndReleased
    prevX = (int)e.getX();
    prevY = (int)e.getY();
    setNeedsUpdate(true);
  }

  private void mousePressedAndReleased(MouseEvent e)
  {
    if (mouseDragged) return;
    ENGINE.lock();
    try
    {
      int x = (viewXOffset * zoom + (int)e.getX()) / zoom;
      int y = (viewYOffset * zoom + (int)e.getY()) / zoom;
      ENGINE.setAge(x, y, ENGINE.getAge(x, y) > 0 ? 0 : 1);
    }
    finally
    {
      ENGINE.unlock();
    }
    setNeedsUpdate(true);
  }

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

  private void adjustViewOffsetsToZoom()
  {
    final int MAX_VIEW_OFFSET = ENGINE.getWorldWidth() - width / zoom;
    System.out.println(viewXOffset);
    if (viewXOffset < 0) viewXOffset = MIN_VIEW_OFFSET;
    if (viewYOffset < 0) viewYOffset = MIN_VIEW_OFFSET;
    if (viewXOffset > MAX_VIEW_OFFSET) viewXOffset = MAX_VIEW_OFFSET;
    //if (viewYOffset > MAX_VIEW_OFFSET) viewYOffset = MAX_VIEW_OFFSET;
  }

  private void setNeedsUpdate(boolean val)
  {
    needsUpdate = val;
  }
}