package nl.basmens.wfc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

import cern.colt.list.IntArrayList;

public final class Wfc implements Runnable {
  private int gridW;
  private int gridH;
  private Tile[][] grid;
  private boolean isLoopEdgesEnabledX;
  private boolean isLoopEdgesEnabledY;

  private Module[] modules;
  private PossibilitySet[] possibleModulesUp;
  private PossibilitySet[] possibleModulesRight;
  private PossibilitySet[] possibleModulesDown;
  private PossibilitySet[] possibleModulesLeft;

  private IntArrayList[] entropyLists;
  private int[][] locationInEntropyList;
  private boolean running;

  private ArrayDeque<QueueElm> stack = new ArrayDeque<>(20000);

  private Random random = new Random();

  private enum Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT
  }

  record QueueElm(int x, int y, Direction direction) {
  }

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures) {
    this.gridW = gridW;
    this.gridH = gridH;

    setWfcFeatures(wfcFeatures);
  }

  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures, int seed) {
    this(gridW, gridH, wfcFeatures);
    this.random.setSeed(seed);
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
    synchronized (this) {
      grid = new Tile[gridW][gridH];
      locationInEntropyList = new int[gridW][gridH];
      IntArrayList maxEntropyList = entropyLists[modules.length - 2];

      for (int x = 0; x < gridW; x++) {
        for (int y = 0; y < gridH; y++) {
          grid[x][y] = new Tile(modules.length, true);
          locationInEntropyList[x][y] = maxEntropyList.size();
          maxEntropyList.add(x * gridH + y);
        }
      }
    }

    while (running) {
      int collapsedTile = collapseTile();
      while (!stack.isEmpty()) {
        QueueElm qe = stack.pop();
        if (updateTile(qe.x, qe.y, qe.direction, collapsedTile)) {
          stack.clear();

          int latestInfluence = grid[qe.x][qe.y].getLatestInfluence();
          rollBackWithTile(latestInfluence);

          int x = latestInfluence / gridH;
          int y = latestInfluence % gridH;
          int oldEntropy = grid[x][y].getEntropy();
          grid[x][y].uncollapse();
          moveBetweenEntropyLists(x, y, oldEntropy, grid[x][y].getEntropy());
        }
      }
    }
  }

  public boolean updateTile(int x, int y, Direction direction, int collapsedTileId) {
    Tile tile = grid[x][y];
    int startEntropy = tile.getEntropy();

    if (tile.isCollapsed()) {
      return false;
    }

    PossibilitySet possibilities;
    switch (direction) {
      case UP:
        possibilities = new PossibilitySet(modules.length, false);
        for (int i : grid[x][(y + gridH - 1) % gridH].getPossibilitiesAsArray()) {
          possibilities.unionWith(possibleModulesDown[i]);
        }
        tile.intersectionWith(possibilities);
        break;
      case RIGHT:
        possibilities = new PossibilitySet(modules.length, false);
        for (int i : grid[(x + 1) % gridW][y].getPossibilitiesAsArray()) {
          possibilities.unionWith(possibleModulesLeft[i]);
        }
        tile.intersectionWith(possibilities);
        break;
      case DOWN:
        possibilities = new PossibilitySet(modules.length, false);
        for (int i : grid[x][(y + 1) % gridH].getPossibilitiesAsArray()) {
          possibilities.unionWith(possibleModulesUp[i]);
        }
        tile.intersectionWith(possibilities);
        break;
      case LEFT:
        possibilities = new PossibilitySet(modules.length, false);
        for (int i : grid[(x + gridW - 1) % gridW][y].getPossibilitiesAsArray()) {
          possibilities.unionWith(possibleModulesRight[i]);
        }
        tile.intersectionWith(possibilities);
        break;
    }

    if (tile.getEntropy() < startEntropy) {
      moveBetweenEntropyLists(x, y, startEntropy, tile.getEntropy());

      if (tile.getEntropy() == 0) {
        return true;
      }

      tile.influence(collapsedTileId);

      if (direction != Direction.UP && (y > 0 || isLoopEdgesEnabledY)) {
        stack.push(new QueueElm(x, (y + gridH - 1) % gridH, Direction.DOWN));
      }
      if (direction != Direction.RIGHT && (x < gridW - 1 || isLoopEdgesEnabledX)) {
        stack.push(new QueueElm((x + 1) % gridW, y, Direction.LEFT));
      }
      if (direction != Direction.DOWN && (y < gridH - 1 || isLoopEdgesEnabledY)) {
        stack.push(new QueueElm(x, (y + 1) % gridH, Direction.UP));
      }
      if (direction != Direction.LEFT && (x > 0 || isLoopEdgesEnabledX)) {
        stack.push(new QueueElm((x + gridW - 1) % gridW, y, Direction.RIGHT));
      }
    }

    return false;
  }

  public void rollBackWithTile(int tile) {
    PossibilitySet resetter = new PossibilitySet(modules.length, true);
    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        Tile t = grid[x][y];
        if (t.isInfluencedBy(tile)) {
          t.removeInfluence(tile);

          if (t.isCollapsed()) {
            rollBackWithTile(x * gridH + y);
          }

          moveBetweenEntropyLists(x, y, t.getEntropy(), modules.length);
          t.unionWith(resetter);
          
          if (y > 0 || isLoopEdgesEnabledY) {
            stack.push(new QueueElm(x, (y + gridH - 1) % gridH, Direction.DOWN));
          }
          if (x < gridW - 1 || isLoopEdgesEnabledX) {
            stack.push(new QueueElm((x + 1) % gridW, y, Direction.LEFT));
          }
          if (y < gridH - 1 || isLoopEdgesEnabledY) {
            stack.push(new QueueElm(x, (y + 1) % gridH, Direction.UP));
          }
          if (x > 0 || isLoopEdgesEnabledX) {
            stack.push(new QueueElm((x + gridW - 1) % gridW, y, Direction.RIGHT));
          }
        }
      }
    }
  }

  public void collapseTile(int x, int y) {
    Tile tile = grid[x][y];

    int[] possibilities = tile.getPossibilitiesAsArray();
    moveBetweenEntropyLists(x, y, tile.getEntropy(), 1);
    tile.collapse(possibilities[random.nextInt(possibilities.length)]);

    if (y > 0 || isLoopEdgesEnabledY) {
      stack.push(new QueueElm(x, (y + gridH - 1) % gridH, Direction.DOWN));
    }
    if (x < gridW - 1 || isLoopEdgesEnabledX) {
      stack.push(new QueueElm((x + 1) % gridW, y, Direction.LEFT));
    }
    if (y < gridH - 1 || isLoopEdgesEnabledY) {
      stack.push(new QueueElm(x, (y + 1) % gridH, Direction.UP));
    }
    if (x > 0 || isLoopEdgesEnabledX) {
      stack.push(new QueueElm((x + gridW - 1) % gridW, y, Direction.RIGHT));
    }
  }

  public int collapseTile() {
    int entropy = 0;
    while (entropyLists[entropy].size() == 0) {
      entropy++;
      if (entropy == entropyLists.length) {
        running = false;
        return -1;
      }
    }

    IntArrayList entropyList = entropyLists[entropy];
    int tileToCollapse = entropyList.get(random.nextInt(entropyList.size()));
    
    collapseTile(tileToCollapse / gridH, tileToCollapse % gridH);
    return tileToCollapse;
  }

  private void moveBetweenEntropyLists(int x, int y, int oldEntropy, int newEntropy) {
    if (oldEntropy > 1) {
      IntArrayList oldList = entropyLists[oldEntropy - 2];
      int lastValue = oldList.get(oldList.size() - 1);
      locationInEntropyList[lastValue / gridH][lastValue % gridH] = locationInEntropyList[x][y];

      oldList.set(locationInEntropyList[x][y], lastValue);
      oldList.remove(oldList.size() - 1);
    }

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

  public synchronized PossibilitySet[][] getGrid() {
    if (grid == null) {
      return new PossibilitySet[0][0];
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
    isLoopEdgesEnabledX = wfcFeatures.isLoopEdgesEnabledX();
    isLoopEdgesEnabledY = wfcFeatures.isLoopEdgesEnabledY();
  }

  // ===================================================================================================================
  // Tile
  // ===================================================================================================================
  private class Tile extends PossibilitySet {
    private PossibilitySet possibilitiesBeforeCollapse;
    private IntArrayList influences = new IntArrayList();

    public Tile(int possibilitiesCount, boolean value) {
      super(possibilitiesCount, value);
    }

    public Tile(PossibilitySet toClone) {
      super(toClone);
    }

    @Override
    public void collapse(int index) {
      possibilitiesBeforeCollapse = new PossibilitySet(this);
      super.collapse(index);
    }

    public void uncollapse() {
      int collapsedTile = getPossibilitiesAsArray()[0];
      unionWith(possibilitiesBeforeCollapse).removePossibility(collapsedTile);
    }

    public void influence(int collapsedTileId) {
      influences.add(collapsedTileId);
    }

    public boolean isInfluencedBy(int influence) {
      return influences.contains(influence);
    }

    public int getLatestInfluence() {
      return influences.get(influences.size() - 1);
    }

    public void removeInfluence(int influence) {
      influences.delete(influence);
    }
  }
}
