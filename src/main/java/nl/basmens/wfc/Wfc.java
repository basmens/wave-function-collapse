package nl.basmens.wfc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import nl.benmens.processing.PApplet;

public final class Wfc {
  private int gridW;
  private int gridH;
  private WfcFeatures wfcFeatures;
  private Tile[][] grid;

  private ExecutorService executorService;
  private TileUpdater[][] tileUpdaters;
  private AtomicInteger threadCounter;
  private boolean isRunning;

  private ArrayList<IntModule> intModules;
  private ArrayList<HashSet<IntModule>> modulesForUpKey;
  private ArrayList<HashSet<IntModule>> modulesForRightKey;
  private ArrayList<HashSet<IntModule>> modulesForDownKey;
  private ArrayList<HashSet<IntModule>> modulesForLeftKey;

  private Random random = new Random();
  private LinkedListElement[] entropyLists;

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public Wfc(int gridW, int gridH, WfcFeatures wfcFeatures, int threadCount) {
    this.gridW = gridW;
    this.gridH = gridH;
    setWfcFeatures(wfcFeatures);
    this.executorService = Executors.newFixedThreadPool(threadCount);
    threadCounter = new AtomicInteger();

    createGrid();
  }

  // ===================================================================================================================
  // Functionality
  // ===================================================================================================================
  public void createGrid() {
    if (isRunning) {
      return;
    }

    entropyLists = new LinkedListElement[intModules.size() - 1];

    // Create the grid and manage relations
    grid = new Tile[gridW][gridH];
    tileUpdaters = new TileUpdater[gridW][gridH];
    for (int x = 0; x < gridW; x++) {
      for (int y = 0; y < gridH; y++) {
        grid[x][y] = new Tile(intModules);
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
    if (isRunning) {
      return;
    }

    isRunning = true;

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

  public void collapseTile(int x, int y) {
    collapseTile(tileUpdaters[x][y]);
  }

  public void collapseTile(TileUpdater tileUpdater) {
    Tile tile = tileUpdater.getTile();
    IntModule intModule = tile.getPossibilities()
        .toArray(IntModule[]::new)[random.nextInt(0, tile.getPossibilities().size())];
    tile.colapse(intModule);

    tileUpdater.updateChanged();
  }

  public void collapseTile() {
    int entropy;
    LinkedListElement next = null;
    for (entropy = 0; entropy < entropyLists.length; entropy++) {
      next = entropyLists[entropy];
      while (next != null && next.tileUpdater == null) {
        next = next.next;
        entropyLists[entropy] = next;
      }

      if (next != null) {
        entropyLists[entropy] = next.next;
        break;
      }
    }

    if (next == null) {
      isRunning = false;
      return;
    }

    // int randomDepth = random.nextInt(tileCount);
    // LinkedListElement element = entropyLists[entropy];
    // for (int i = 0; i < randomDepth; i++) {
    // element = element.next;
    // }

    collapseTile(next.tileUpdater);
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

  public boolean isRunning() {
    return isRunning;
  }

  public void setWfcFeatures(WfcFeatures wfcFeatures) {
    if (isRunning) {
      return;
    }

    this.wfcFeatures = wfcFeatures;

    intModules = new ArrayList<>(wfcFeatures.getIntModules());
    modulesForUpKey = new ArrayList<>(wfcFeatures.getModulesForUpKey());
    modulesForRightKey = new ArrayList<>(wfcFeatures.getModulesForRightKey());
    modulesForDownKey = new ArrayList<>(wfcFeatures.getModulesForDownKey());
    modulesForLeftKey = new ArrayList<>(wfcFeatures.getModulesForLeftKey());
  }

  // ===================================================================================================================
  // LickedList
  // ===================================================================================================================
  private class LinkedListElement {
    public LinkedListElement next;
    public TileUpdater tileUpdater;

    public LinkedListElement(LinkedListElement next, TileUpdater tileUpdater) {
      this.next = next;
      this.tileUpdater = tileUpdater;
    }
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
    private Object runningMonitor = new Object();

    private final Tile tile;
    private Tile tileUp;
    private Tile tileRight;
    private Tile tileDown;
    private Tile tileLeft;
    private TileUpdater updaterUp;
    private TileUpdater updaterRight;
    private TileUpdater updaterDown;
    private TileUpdater updaterLeft;

    private LinkedListElement linkedListElement;

    // -----------------------------------------------------------------------------------------------------------------
    // Creation
    // -----------------------------------------------------------------------------------------------------------------
    public TileUpdater(Tile tile) {
      this.tile = tile;

      prefixNewEntropyListElement();
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
    // Entropy list
    // -----------------------------------------------------------------------------------------------------------------
    private void prefixNewEntropyListElement() {
      synchronized (entropyLists) {
        linkedListElement = new LinkedListElement(entropyLists[tile.getEntropy() - 2], this);
        entropyLists[tile.getEntropy() - 2] = linkedListElement;
      }
    }

    private void updateEntropyInList() {
      linkedListElement.tileUpdater = null;

      if (tile.getEntropy() == 0) {
        isRunning = false;
        executorService.shutdown();
        // PApplet.println("No solution");
        return;
      }

      if (!tile.isCollapsed()) {
        prefixNewEntropyListElement();
      }
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
        threadCounter.incrementAndGet();
        executorService.submit(this);
      }
      submitted = true;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Evaluating
    // -----------------------------------------------------------------------------------------------------------------
    public void updateChanged() {
      updaterUp.updateFromDown();
      updaterRight.updateFromLeft();
      updaterDown.updateFromUp();
      updaterLeft.updateFromRight();

      updateEntropyInList();
    }

    private void collapseUntillUpdates() {
      while (threadCounter.get() == 0 && isRunning) {
        collapseTile();
      }
    }

    @Override
    public void run() {
      synchronized (runningMonitor) {
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

        // Do the computing
        boolean changed = false;
        HashSet<IntModule> newPossibilities = tile.getPossibilities();
        if (localUpdateFromUp) {
          HashSet<Integer> keys = new HashSet<>();
          tileUp.getPossibilities().forEach(m -> keys.add(m.keyDown));

          HashSet<IntModule> comparePossibilities = new HashSet<>();
          keys.forEach(k -> comparePossibilities.addAll(modulesForDownKey.get(k)));
          changed = newPossibilities.retainAll(comparePossibilities) || changed;
        }
        if (localUpdateFromRight) {
          HashSet<Integer> keys = new HashSet<>();
          tileRight.getPossibilities().forEach(m -> keys.add(m.keyLeft));

          HashSet<IntModule> comparePossibilities = new HashSet<>();
          keys.forEach(k -> comparePossibilities.addAll(modulesForLeftKey.get(k)));
          changed = newPossibilities.retainAll(comparePossibilities) || changed;
        }
        if (localUpdateFromDown) {
          HashSet<Integer> keys = new HashSet<>();
          tileDown.getPossibilities().forEach(m -> keys.add(m.keyUp));

          HashSet<IntModule> comparePossibilities = new HashSet<>();
          keys.forEach(k -> comparePossibilities.addAll(modulesForUpKey.get(k)));
          changed = newPossibilities.retainAll(comparePossibilities) || changed;
        }
        if (localUpdateFromLeft) {
          HashSet<Integer> keys = new HashSet<>();
          tileLeft.getPossibilities().forEach(m -> keys.add(m.keyRight));

          HashSet<IntModule> comparePossibilities = new HashSet<>();
          keys.forEach(k -> comparePossibilities.addAll(modulesForRightKey.get(k)));
          changed = newPossibilities.retainAll(comparePossibilities) || changed;
        }

        if (changed) {
          tile.setPossibilities(newPossibilities);
          updateChanged();
        }

        if (threadCounter.decrementAndGet() == 0) {
          collapseUntillUpdates();
        }
      }
    }

    public Tile getTile() {
      return tile;
    }
  }
}
