package nl.basmens.wfc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.benmens.processing.PApplet;

public class Tile {
  private HashSet<IntModule> possibilities = new HashSet<>();
  private boolean collapsed;

  public Tile(List<IntModule> initialPossibilities) {
    possibilities.addAll(initialPossibilities);

    if (this.possibilities.size() == 1) {
      collapsed = true;
    }
  }

  public boolean isCollapsed() {
    return collapsed;
  }

  public IntModule getModule() {
    if(collapsed) {
      return possibilities.toArray(new IntModule[]{})[0];
    }
    return null;
  }

  public Set<IntModule> getPossibilities() {
    return (Set<IntModule>) possibilities.clone();
  }

  public void setPossibilities(Set<IntModule> possibilities) {
    if (!collapsed) {
      this.possibilities = new HashSet<>(possibilities);

      if (this.possibilities.size() == 1) {
        collapsed = true;
      }
    }
  }
}
