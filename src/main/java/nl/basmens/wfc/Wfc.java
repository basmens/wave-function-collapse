package nl.basmens.wfc;

import java.util.ArrayDeque;
import java.util.Random;

import cern.colt.list.IntArrayList;

public final class Wfc implements Runnable {
  private int gridW;
  private int gridH;
  private PossibilitySet[][] grid;
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
      grid = new PossibilitySet[gridW][gridH];
      locationInEntropyList = new int[gridW][gridH];
      IntArrayList maxEntropyList = entropyLists[modules.length - 2];

      for (int x = 0; x < gridW; x++) {
        for (int y = 0; y < gridH; y++) {
          grid[x][y] = new PossibilitySet(modules.length, true);
          locationInEntropyList[x][y] = maxEntropyList.size();
          maxEntropyList.add(x * gridH + y);
        }
      }
    }

    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        updateTile(x, y, Direction.UP);
        updateTile(x, y, Direction.RIGHT);
        updateTile(x, y, Direction.DOWN);
        updateTile(x, y, Direction.LEFT);
      }
    }

    while (running) {
      while (!stack.isEmpty()) {
        QueueElm qe = stack.pop();
        updateTile(qe.x, qe.y, qe.direction);
      }
      collapseTile();
    }
  }

  public void updateTile(int x, int y, Direction direction) {
    x = (x + gridW) % gridW;
    y = (y + gridH) % gridH;
    PossibilitySet tile = grid[x][y];
    int startEntropy = tile.getEntropy();

    if (startEntropy <= 1) {
      return;
    }

    switch (direction) {
      case UP:
        if (y > 0 || loopEdgesEnabledY) {
          PossibilitySet possibilities = new PossibilitySet(modules.length, false);
          for (int i : grid[x][(y + gridH - 1) % gridH].getPossibilitiesAsArray()) {
            possibilities.unionWith(possibleModulesDown[i]);
          }
          tile.intersectionWith(possibilities);
        }
        break;
      case RIGHT:
        if (x > 0 || loopEdgesEnabledX) {
          PossibilitySet possibilities = new PossibilitySet(modules.length, false);
          for (int i : grid[(x + 1) % gridW][y].getPossibilitiesAsArray()) {
            possibilities.unionWith(possibleModulesLeft[i]);
          }
          tile.intersectionWith(possibilities);
        }
        break;
      case DOWN:
        if (y < gridH - 1 || loopEdgesEnabledY) {
          PossibilitySet possibilities = new PossibilitySet(modules.length, false);
          for (int i : grid[x][(y + 1) % gridH].getPossibilitiesAsArray()) {
            possibilities.unionWith(possibleModulesUp[i]);
          }
          tile.intersectionWith(possibilities);
        }
        break;
      case LEFT:
        if (x < gridW - 1 || loopEdgesEnabledX) {
          PossibilitySet possibilities = new PossibilitySet(modules.length, false);
          for (int i : grid[(x + gridW - 1) % gridW][y].getPossibilitiesAsArray()) {
            possibilities.unionWith(possibleModulesRight[i]);
          }
          tile.intersectionWith(possibilities);
        }
        break;
    }

    if (tile.getEntropy() < startEntropy) {
      moveBetweenEntropyLists(x, y, startEntropy, tile.getEntropy());

      switch (direction) {
        case UP:
          stack.push(new QueueElm(x + 1, y, Direction.LEFT));
          stack.push(new QueueElm(x, y + 1, Direction.UP));
          stack.push(new QueueElm(x - 1, y, Direction.RIGHT));
          break;
        case DOWN:
          stack.push(new QueueElm(x, y - 1, Direction.DOWN));
          stack.push(new QueueElm(x + 1, y, Direction.LEFT));
          stack.push(new QueueElm(x - 1, y, Direction.RIGHT));
          break;
        case LEFT:
          stack.push(new QueueElm(x, y - 1, Direction.DOWN));
          stack.push(new QueueElm(x + 1, y, Direction.LEFT));
          stack.push(new QueueElm(x, y + 1, Direction.UP));
          break;
        case RIGHT:
          stack.push(new QueueElm(x, y - 1, Direction.DOWN));
          stack.push(new QueueElm(x, y + 1, Direction.UP));
          stack.push(new QueueElm(x - 1, y, Direction.RIGHT));
          break;
      }
    }
  }

  public void collapseTile(int x, int y) {
    PossibilitySet tile = grid[x][y];

    int[] possibilities = tile.getPossibilitiesAsArray();
    moveBetweenEntropyLists(x, y, tile.getEntropy(), 1);
    tile.collapse(possibilities[random.nextInt(possibilities.length)]);

    updateTile(x, y + 1, Direction.UP);
    updateTile(x - 1, y, Direction.RIGHT);
    updateTile(x, y - 1, Direction.DOWN);
    updateTile(x + 1, y, Direction.LEFT);
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
    loopEdgesEnabledX = wfcFeatures.isLoopEdgesEnabledX();
    loopEdgesEnabledY = wfcFeatures.isLoopEdgesEnabledY();
  }
}
