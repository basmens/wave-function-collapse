package nl.basmens.wfc;

public final class Tile {
  private PossibilitySet possibilities;
  private int collapsedPossibility;
  private int entropy;

  public Tile(PossibilitySet initialPossibilities) {
    setPossibilities(initialPossibilities);
  }

  public synchronized boolean isCollapsed() {
    return entropy == 1;
  }

  public synchronized int getEntropy() {
    return entropy;
  }

  public synchronized int getCollapsedModule() {
    if (isCollapsed()) {
      return collapsedPossibility;
    }
    return -1;
  }

  public synchronized PossibilitySet getPossibilitiesSet() {
    return new PossibilitySet(possibilities);
  }

  public synchronized void setPossibilities(PossibilitySet possibilities) {
    this.possibilities = new PossibilitySet(possibilities);

    entropy = possibilities.getEntropy();

    if (entropy == 1) {
      long[] values = possibilities.getPossibilitiesArray();
      for (int i = 0; i < values.length; i++) {
        if (values[i] != 0) {
          collapsedPossibility = 0;
          while (values[i] >>> collapsedPossibility != 1) {
            collapsedPossibility++;
          }
        }
      }
    }
  }

  public synchronized void colapse(int module) {
    if (!isCollapsed()) {
      possibilities = new PossibilitySet(possibilities.getPossibilitiesCount(), false);
      possibilities.addPossibility(module);

      entropy = 1;
      collapsedPossibility = module;
    }
  }
}
