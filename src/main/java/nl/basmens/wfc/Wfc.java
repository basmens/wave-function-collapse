package nl.basmens.wfc;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Wfc {
  private int gridW;
  private int gridH;
  private WfcFeatures wfcFeatures;
  private Tile[][] grid;

  private ExecutorService executorService;
  private TileUpdater[][] tileUpdaters;

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures, int threadCount) {
    this.gridW = gridW;
    this.gridH = gridH;
    this.wfcFeatures = wfcFeatures;
    this.executorService = Executors.newFixedThreadPool(threadCount);

    grid = new Tile[gridW][gridH];
    tileUpdaters = new TileUpdater[gridW][gridH];
    for (int x = 0; x < grid.length; x++) {
      for (int y = 0; y < grid[0].length; y++) {
        grid[x][y] = new Tile(wfcFeatures.getIntModules());

        TileUpdater tileUpdater = new TileUpdater(x, y);
        tileUpdater.updateFromUp();
        tileUpdater.updateFromRight();
        tileUpdater.updateFromDown();
        tileUpdater.updateFromLeft();
        tileUpdaters[x][y] = tileUpdater;
      }
    }
  }

  // ===================================================================================================================
  // Functionality
  // ===================================================================================================================

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
    return grid.clone();
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

    private final int x;
    private final int y;

    public TileUpdater(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public synchronized void updateFromUp() {
      if (y > 0 || wfcFeatures.isLoopEdgesEnabled()) {
        updateFromUp = true;
        submit();
      }
    }

    public synchronized void updateFromRight() {
      if (x < gridW || wfcFeatures.isLoopEdgesEnabled()) {
        updateFromRight = true;
        submit();
      }
    }

    public synchronized void updateFromDown() {
      if (y < gridH || wfcFeatures.isLoopEdgesEnabled()) {
        updateFromDown = true;
        submit();
      }
    }

    public synchronized void updateFromLeft() {
      if (x > 0 || wfcFeatures.isLoopEdgesEnabled()) {
        updateFromLeft = true;
        submit();
      }
    }

    private void submit() {
      if (!submitted && !grid[x][y].isCollapsed()) {
        executorService.submit(this);
      }
      submitted = true;
    }

    @Override
    public void run() {
      // Syncronize the update variables
      boolean localUpdateFromUp;
      boolean localUpdateRight;
      boolean localUpdateFromDown;
      boolean localUpdateFromLeft;
      synchronized (this) {
        localUpdateFromUp = updateFromUp;
        localUpdateRight = updateFromRight;
        localUpdateFromDown = updateFromDown;
        localUpdateFromLeft = updateFromLeft;
        updateFromUp = false;
        updateFromRight = false;
        updateFromDown = false;
        updateFromLeft = false;
        submitted = false;
      }

      // Do the computing
      boolean changed = false;
      if (localUpdateFromUp) {
        Random random = new Random();

        Tile tile = grid[x][y];
        IntModule intModule = tile.getPossibilities().toArray(IntModule[]::new)[
          random.nextInt(0, tile.getPossibilities().size())];
        // HashSet<IntModule> set = new HashSet<>();
        // set.add(intModule);
        // tile.setPossibilities(set);

        Set<IntModule> set = tile.getPossibilities();
        set.remove(intModule);
        tile.setPossibilities(set);

        changed = true;

        try {
          Thread.sleep(random.nextInt(10, 20));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      
      if (changed) {
        tileUpdaters[x][(y + gridH - 1) % gridH].updateFromDown();
        tileUpdaters[(x + 1) % gridH][y].updateFromLeft();
        tileUpdaters[x][(y + 1) % gridH].updateFromUp();
        tileUpdaters[(x + gridH - 1) % gridH][y].updateFromRight();
      }
    }
  }
}
