package nl.basmens.wfc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Tile {
  private HashSet<IntModule> possibilities = new HashSet<>();

  public Tile(List<IntModule> initialPossibilities) {
    possibilities.addAll(initialPossibilities);
  }

  public synchronized boolean isCollapsed() {
    return this.possibilities.size() == 1;
  }

  public synchronized int getEntropy() {
    return possibilities.size();
  }

  public synchronized IntModule getModule() {
    if (isCollapsed()) {
      return possibilities.toArray(new IntModule[]{})[0];
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public synchronized HashSet<IntModule> getPossibilities() {
    return (HashSet<IntModule>) possibilities.clone();
  }

  public synchronized void setPossibilities(Set<IntModule> possibilities) {
    if (!isCollapsed()) {
      this.possibilities = new HashSet<>(possibilities);
    }
  }

  public synchronized void colapse(IntModule intModule) {
    if (!isCollapsed()) {
      possibilities = new HashSet<>();
      possibilities.add(intModule);
    }
  }
}
