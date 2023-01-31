package nl.basmens.wfc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Wfc {
  private int gridW;
  private int gridH;
  private WfcFeatures wfcFeatures;
  private Tile[][] grid;

  private ExecutorService executorService;
  private TileUpdater[][] tileUpdaters;

  private ArrayList<HashSet<IntModule>> modulesForUpKey;
  private ArrayList<HashSet<IntModule>> modulesForRightKey;
  private ArrayList<HashSet<IntModule>> modulesForDownKey;
  private ArrayList<HashSet<IntModule>> modulesForLeftKey;

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures, int threadCount) {
    this.gridW = gridW;
    this.gridH = gridH;
    this.wfcFeatures = wfcFeatures;
    this.executorService = Executors.newFixedThreadPool(threadCount);

    createGrid();
  }

  // ===================================================================================================================
  // Functionality
  // ===================================================================================================================
  public void createGrid() {
    // Create the grid and manage relations
    grid = new Tile[gridW][gridH];
    tileUpdaters = new TileUpdater[gridW][gridH];
    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        grid[x][y] = new Tile(wfcFeatures.getIntModules());
        tileUpdaters[x][y] = new TileUpdater(grid[x][y]);

        if (x > 0) {
          TileUpdater left = tileUpdaters[x - 1][y];
          tileUpdaters[x][y].setNeighbourLeft(grid[x - 1][y], left);
          left.setNeighbourRight(grid[x][y], tileUpdaters[x][y]);
        }
        if (y > 0) {
          TileUpdater up = tileUpdaters[x][y - 1];
          tileUpdaters[x][y].setNeighbourUp(grid[x][y - 1], up);
          up.setNeighbourDown(grid[x][y], tileUpdaters[x][y]);
        }
      }
    }

    // Manage relations looping over the edge
    boolean loop = wfcFeatures.isLoopEdgesEnabled();
    for (int x = 0; x < gridW; x++) {
      TileUpdater top = tileUpdaters[x][0];
      TileUpdater bottom = tileUpdaters[x][gridH - 1];
      Tile topTile = (loop) ? grid[x][0] : null;
      Tile bottomTile = (loop) ? grid[x][gridH - 1] : null;

      top.setNeighbourUp(bottomTile, bottom);
      bottom.setNeighbourDown(topTile, top);
    }
    for (int y = 0; y < gridH; y++) {
      TileUpdater left = tileUpdaters[0][y];
      TileUpdater right = tileUpdaters[gridW - 1][y];
      Tile leftTile = (loop) ? grid[0][y] : null;
      Tile rightTile = (loop) ? grid[gridW - 1][y] : null;

      left.setNeighbourLeft(rightTile, right);
      right.setNeighbourRight(leftTile, left);
    }
  }

  public void start() {
    modulesForUpKey = new ArrayList<>(wfcFeatures.getModulesForUpKey());
    modulesForRightKey = new ArrayList<>(wfcFeatures.getModulesForRightKey());
    modulesForDownKey = new ArrayList<>(wfcFeatures.getModulesForDownKey());
    modulesForLeftKey = new ArrayList<>(wfcFeatures.getModulesForLeftKey());

    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        TileUpdater tileUpdater = tileUpdaters[x][y];
        tileUpdater.updateFromUp();
        tileUpdater.updateFromRight();
        tileUpdater.updateFromDown();
        tileUpdater.updateFromLeft();
      }
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
    return grid;
  }

  // ===================================================================================================================
  // TileUpdater
  // ===================================================================================================================
  private class TileUpdater implements Runnable {
    private boolean updateFromUp;
    private boolean updateFromRight;
    private boolean updateFromDown;
    private boolean updateFromLeft;
    private boolean submitted;

    private final Tile tile;
    private Tile tileUp;
    private Tile tileRight;
    private Tile tileDown;
    private Tile tileLeft;
    private TileUpdater updaterUp;
    private TileUpdater updaterRight;
    private TileUpdater updaterDown;
    private TileUpdater updaterLeft;

    // -----------------------------------------------------------------------------------------------------------------
    // Creation
    // -----------------------------------------------------------------------------------------------------------------
    public TileUpdater(Tile tile) {
      this.tile = tile;
    }

    public void setNeighbourUp(Tile tileUp, TileUpdater updaterUp) {
      this.tileUp = tileUp;
      this.updaterUp = updaterUp;
    }

    public void setNeighbourRight(Tile tileRight, TileUpdater updaterRight) {
      this.tileRight = tileRight;
      this.updaterRight = updaterRight;
    }

    public void setNeighbourDown(Tile tileDown, TileUpdater updaterDown) {
      this.tileDown = tileDown;
      this.updaterDown = updaterDown;
    }

    public void setNeighbourLeft(Tile tileLeft, TileUpdater updaterLeft) {
      this.tileLeft = tileLeft;
      this.updaterLeft = updaterLeft;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Queueing
    // -----------------------------------------------------------------------------------------------------------------
    public synchronized void updateFromUp() {
      if (tileUp != null) {
        updateFromUp = true;
        submit();
      }
    }

    public synchronized void updateFromRight() {
      if (tileRight != null) {
        updateFromRight = true;
        submit();
      }
    }

    public synchronized void updateFromDown() {
      if (tileDown != null) {
        updateFromDown = true;
        submit();
      }
    }

    public synchronized void updateFromLeft() {
      if (tileLeft != null) {
        updateFromLeft = true;
        submit();
      }
    }

    private void submit() {
      if (!submitted && !tile.isCollapsed()) {
        executorService.submit(this);
      }
      submitted = true;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Evaluating
    // -----------------------------------------------------------------------------------------------------------------
    @Override
    public void run() {
      // Syncronize the update variables
      boolean localUpdateFromUp;
      boolean localUpdateFromRight;
      boolean localUpdateFromDown;
      boolean localUpdateFromLeft;
      synchronized (this) {
        localUpdateFromUp = updateFromUp;
        localUpdateFromRight = updateFromRight;
        localUpdateFromDown = updateFromDown;
        localUpdateFromLeft = updateFromLeft;
        updateFromUp = false;
        updateFromRight = false;
        updateFromDown = false;
        updateFromLeft = false;
        submitted = false;
      }
      // PApplet.println("start: " + localUpdateFromUp + " - " + localUpdateFromRight
      // + " - " + localUpdateFromDown + " - " + localUpdateFromLeft);

      // Do the computing
      boolean changed = false;
      if (localUpdateFromUp) {
        HashSet<Integer> keys = new HashSet<>();
        tileUp.getPossibilities().forEach(m -> keys.add(m.keyDown));

        HashSet<IntModule> comparePossibilities = new HashSet<>();
        keys.forEach(k -> comparePossibilities.addAll(modulesForDownKey.get(k)));

        HashSet<IntModule> newPossibilities = tile.getPossibilities();
        changed = newPossibilities.retainAll(comparePossibilities) || changed;
        tile.setPossibilities(newPossibilities);
      }
      if (localUpdateFromRight) {
        HashSet<Integer> keys = new HashSet<>();
        tileRight.getPossibilities().forEach(m -> keys.add(m.keyLeft));

        HashSet<IntModule> comparePossibilities = new HashSet<>();
        keys.forEach(k -> comparePossibilities.addAll(modulesForLeftKey.get(k)));

        HashSet<IntModule> newPossibilities = tile.getPossibilities();
        changed = newPossibilities.retainAll(comparePossibilities) || changed;
        tile.setPossibilities(newPossibilities);
      }
      if (localUpdateFromDown) {
        HashSet<Integer> keys = new HashSet<>();
        tileDown.getPossibilities().forEach(m -> keys.add(m.keyUp));

        HashSet<IntModule> comparePossibilities = new HashSet<>();
        keys.forEach(k -> comparePossibilities.addAll(modulesForUpKey.get(k)));

        HashSet<IntModule> newPossibilities = tile.getPossibilities();
        changed = newPossibilities.retainAll(comparePossibilities) || changed;
        tile.setPossibilities(newPossibilities);
      }
      if (localUpdateFromLeft) {
        HashSet<Integer> keys = new HashSet<>();
        tileLeft.getPossibilities().forEach(m -> keys.add(m.keyRight));

        HashSet<IntModule> comparePossibilities = new HashSet<>();
        keys.forEach(k -> comparePossibilities.addAll(modulesForRightKey.get(k)));

        HashSet<IntModule> newPossibilities = tile.getPossibilities();
        changed = newPossibilities.retainAll(comparePossibilities) || changed;
        tile.setPossibilities(newPossibilities);
      }

      if (changed) {
        updaterUp.updateFromDown();
        updaterRight.updateFromLeft();
        updaterDown.updateFromUp();
        updaterLeft.updateFromRight();
      }
    }
  }

  public void collapseTile(int x, int y) {
    Tile tile = grid[x][y];
    Random random = new Random();
    IntModule intModule = tile.getPossibilities().toArray(IntModule[]::new)[random.nextInt(0,
        tile.getPossibilities().size())];
    HashSet<IntModule> set = new HashSet<>();
    set.add(intModule);
    tile.setPossibilities(set);

    tileUpdaters[x][(y + gridH - 1) % gridH].updateFromDown();
    tileUpdaters[(x + 1) % gridW][y].updateFromLeft();
    tileUpdaters[x][(y + 1) % gridH].updateFromUp();
    tileUpdaters[(x + gridW - 1) % gridW][y].updateFromRight();
  }
}
