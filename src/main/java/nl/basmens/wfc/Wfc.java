package nl.basmens.wfc;

import java.util.ArrayList;
import java.util.Random;

import cern.colt.list.IntArrayList;
import nl.benmens.processing.PApplet;

public final class Wfc implements Runnable {
  private static final int MAX_RECURSION = 500;

  private int gridW;
  private int gridH;
  private Tile[][] grid;
  private boolean loopEdgesEnabledX;
  private boolean loopEdgesEnabledY;

  private Module[] modules;
  private PossibilitySet[] possibleModulesUp;
  private PossibilitySet[] possibleModulesRight;
  private PossibilitySet[] possibleModulesDown;
  private PossibilitySet[] possibleModulesLeft;

  private IntArrayList[] entropyLists;
  private int[][] locationInEntropyList;
  private boolean running;

  private IntArrayList stackX = new IntArrayList();
  private IntArrayList stackY = new IntArrayList();
  private ArrayList<Direction> stackDirection = new ArrayList<>();

  private Random random = new Random();

  private enum Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT
  }

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures) {
    this.gridW = gridW;
    this.gridH = gridH;

    setWfcFeatures(wfcFeatures);
  }

  // ===================================================================================================================
  // Functionality
  // ===================================================================================================================
  public void run() {
    running = true;

    // Create entropy lists
    entropyLists = new IntArrayList[modules.length - 1];
    for (int i = 0; i < modules.length - 1; i++) {
      entropyLists[i] = new IntArrayList();
    }

    // Create the grid
    grid = new Tile[gridW][gridH];
    locationInEntropyList = new int[gridW][gridH];
    IntArrayList maxEntropyList = entropyLists[modules.length - 2];
    
    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        grid[x][y] = new Tile(new PossibilitySet(modules.length, true));
        locationInEntropyList[x][y] = maxEntropyList.size();
        maxEntropyList.add(x * gridH + y);
      }
    }

    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        updateTile(x, y, Direction.UP, 0);
        updateTile(x, y, Direction.RIGHT, 0);
        updateTile(x, y, Direction.DOWN, 0);
        updateTile(x, y, Direction.LEFT, 0);
      }
    }
    
    while (running) {
      while (stackX.size() > 0) {
        updateTile(stackX.get(0), stackY.get(0), stackDirection.get(0), 0);
        stackX.remove(0);
        stackY.remove(0);
        stackDirection.remove(0);
      }
      collapseTile();
    }
  }

  public void updateTile(int x, int y, Direction direction, int recursion) {
    x = (x + gridW) % gridW;
    y = (y + gridH) % gridH;
    Tile tile = grid[x][y];
    int startEntropy = tile.getEntropy();

    if (startEntropy <= 1) {
      return;
    }

    if ((direction == Direction.UP) && (y > 0 || loopEdgesEnabledY)) {
      Tile t = grid[x][(y + gridH - 1) % gridH];
      PossibilitySet possibilities = new PossibilitySet(modules.length, false);
      for (int i : t.getPossibilitiesSet().getPossibilities()) {
        possibilities.unionWith(possibleModulesDown[i]);
      }
      tile.setPossibilities(tile.getPossibilitiesSet().intersectionWith(possibilities));
    }
    if ((direction == Direction.RIGHT) && (x > 0 || loopEdgesEnabledX)) {
      Tile t = grid[(x + 1) % gridW][y];
      PossibilitySet possibilities = new PossibilitySet(modules.length, false);
      for (int i : t.getPossibilitiesSet().getPossibilities()) {
        possibilities.unionWith(possibleModulesLeft[i]);
      }
      tile.setPossibilities(tile.getPossibilitiesSet().intersectionWith(possibilities));
    }
    if ((direction == Direction.DOWN) && (y < gridH - 1 || loopEdgesEnabledY)) {
      Tile t = grid[x][(y + 1) % gridH];
      PossibilitySet possibilities = new PossibilitySet(modules.length, false);
      for (int i : t.getPossibilitiesSet().getPossibilities()) {
        possibilities.unionWith(possibleModulesUp[i]);
      }
      tile.setPossibilities(tile.getPossibilitiesSet().intersectionWith(possibilities));
    }
    if ((direction == Direction.LEFT) && (x < gridW - 1 || loopEdgesEnabledX)) {
      Tile t = grid[(x + gridW - 1) % gridW][y];
      PossibilitySet possibilities = new PossibilitySet(modules.length, false);
      for (int i : t.getPossibilitiesSet().getPossibilities()) {
        possibilities.unionWith(possibleModulesRight[i]);
      }
      tile.setPossibilities(tile.getPossibilitiesSet().intersectionWith(possibilities));
    }

    if (tile.getEntropy() < startEntropy) {
      moveBetweenEntropyLists(x, y, startEntropy, tile.getEntropy());

      if (recursion > MAX_RECURSION) {
        if (direction != Direction.UP) {
          stackX.add(x);
          stackY.add(y - 1);
          stackDirection.add(Direction.DOWN);
        }
        if (direction != Direction.RIGHT) {
          stackX.add(x + 1);
          stackY.add(y);
          stackDirection.add(Direction.LEFT);
        }
        if (direction != Direction.DOWN) {
          stackX.add(x);
          stackY.add(y + 1);
          stackDirection.add(Direction.UP);
        }
        if (direction != Direction.LEFT) {
          stackX.add(x - 1);
          stackY.add(y);
          stackDirection.add(Direction.RIGHT);
        }
      } else {
        if (direction != Direction.UP) {
          updateTile(x, y - 1, Direction.DOWN, recursion + 1);
        }
        if (direction != Direction.RIGHT) {
          updateTile(x + 1, y, Direction.LEFT, recursion + 1);
        }
        if (direction != Direction.DOWN) {
          updateTile(x, y + 1, Direction.UP, recursion + 1);
        }
        if (direction != Direction.LEFT) {
          updateTile(x - 1, y, Direction.RIGHT, recursion + 1);
        }
      }
    }
  }

  public void collapseTile(int x, int y) {
    Tile tile = grid[x][y];
    if (!tile.isCollapsed()) {
      int[] possibilities = tile.getPossibilitiesSet().getPossibilities();
      moveBetweenEntropyLists(x, y, tile.getEntropy(), 1);
      tile.colapse(possibilities[random.nextInt(possibilities.length)]);
  
      updateTile(x, y + 1, Direction.UP, 0);
      updateTile(x - 1, y, Direction.RIGHT, 0);
      updateTile(x, y - 1, Direction.DOWN, 0);
      updateTile(x + 1, y, Direction.LEFT, 0);
    }
  }

  public void collapseTile() {
    int entropy = 0;
    while (entropyLists[entropy].size() == 0) {
      entropy++;
      if (entropy == entropyLists.length) {
        running = false;
        return;
      }
    }

    IntArrayList entropyList = entropyLists[entropy];
    int tileToCollapse = entropyList.get(random.nextInt(entropyList.size()));
    collapseTile(tileToCollapse / gridH, tileToCollapse % gridH);
  }

  private void moveBetweenEntropyLists(int x, int y, int oldEntropy, int newEntropy) {
    IntArrayList oldList = entropyLists[oldEntropy - 2];
    int lastValue = oldList.get(oldList.size() - 1);
    locationInEntropyList[lastValue / gridH][lastValue % gridH] = locationInEntropyList[x][y];

    oldList.set(locationInEntropyList[x][y], lastValue);
    oldList.remove(oldList.size() - 1);

    newEntropy -= 2;
    if (newEntropy >= 0) {
      locationInEntropyList[x][y] = entropyLists[newEntropy].size();
      entropyLists[newEntropy].add(x * gridH + y);
    }
  }

  // ===================================================================================================================
  // Getters and Setters
  // ===================================================================================================================
  public int getGridW() {
    return gridW;
  }

  public int getGridH() {
    return gridH;
  }

  public Tile[][] getGrid() {
    if (grid == null) {
      return new Tile[0][0];
    }
    return grid.clone();
  }

  public Module getModule(int index) {
    return modules[index];
  }

  public boolean isRunning() {
    return running;
  }

  public void setWfcFeatures(WfcFeatures wfcFeatures) {
    modules = wfcFeatures.getModules();
    possibleModulesUp = wfcFeatures.getPossibleModulesUp();
    possibleModulesRight = wfcFeatures.getPossibleModulesRight();
    possibleModulesDown = wfcFeatures.getPossibleModulesDown();
    possibleModulesLeft = wfcFeatures.getPossibleModulesLeft();
    loopEdgesEnabledX = wfcFeatures.isLoopEdgesEnabledX();
    loopEdgesEnabledY = wfcFeatures.isLoopEdgesEnabledY();
  }
}
