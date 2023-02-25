package nl.basmens.wfc;

public final class PossibilitySet {
  private static final long ALL_ONE = 0xffff_ffff_ffff_ffffL;

  private final long removeTrailingOnesLong;

  private final long[] possibilitiesArray;
  private final int possibilitiesCount;

  private boolean isEntropyUpdated;
  private int entropy;

  // ===================================================================================================================
  // Construction
  // ===================================================================================================================
  public PossibilitySet(int possibilitiesCount, boolean value) {
    this.possibilitiesCount = possibilitiesCount;
    this.possibilitiesArray = new long[possibilitiesCount / 64 + 1];

    for (int i = 0; i < possibilitiesArray.length; i++) {
      possibilitiesArray[i] = value ? ALL_ONE : 0;
    }

    removeTrailingOnesLong = ALL_ONE >>> (64 - possibilitiesCount % 64);
    int index = possibilitiesArray.length - 1;
    possibilitiesArray[index] = possibilitiesArray[index] & removeTrailingOnesLong;
  }
  
  public PossibilitySet(PossibilitySet toClone) {
    this.possibilitiesCount = toClone.getPossibilitiesCount();
    this.possibilitiesArray = toClone.getPossibilitiesArray();
    
    removeTrailingOnesLong = ALL_ONE >>> (64 - possibilitiesCount % 64);
  }

  // ===================================================================================================================
  // Functionality
  // ===================================================================================================================
  private void calculateEntropy() {
    entropy = 0;
    for (int i = 0; i < possibilitiesArray.length; i++) {
      long value = possibilitiesArray[i];

      for (int j = 0; j < 64; j++) {
        entropy += value & 1L;
        value >>>= 1;
      }
    }
    isEntropyUpdated = true;
  }

  // ===================================================================================================================
  // Operations
  // ===================================================================================================================
  public void addPossibility(int index) {
    if (index >= possibilitiesCount || hasPossibility(index)) {
      return;
    }
    possibilitiesArray[index / 64] = possibilitiesArray[index / 64] | (1L << (index % 64));
    entropy++;
  }

  public void removePossibility(int index) {
    if (index >= possibilitiesCount  || !hasPossibility(index)) {
      return;
    }
    possibilitiesArray[index / 64] = possibilitiesArray[index / 64] & ~(1L << (index % 64));
    entropy--;
  }

  public PossibilitySet unionWith(PossibilitySet set) {
    long[] values = set.getPossibilitiesArray();

    for (int i = 0; i <= Math.max(possibilitiesCount, set.getPossibilitiesCount()) / 64; i++) {
      possibilitiesArray[i] = possibilitiesArray[i] | values[i];
    }

    int index = possibilitiesArray.length - 1;
    possibilitiesArray[index] = possibilitiesArray[index] & removeTrailingOnesLong;
    isEntropyUpdated = false;

    return this;
  }

  public PossibilitySet intersectionWith(PossibilitySet set) {
    long[] values = set.getPossibilitiesArray();

    for (int i = 0; i <= Math.max(possibilitiesCount, set.getPossibilitiesCount()) / 64; i++) {
      possibilitiesArray[i] = possibilitiesArray[i] & values[i];
    }

    isEntropyUpdated = false;
    return this;
  }

  public void collapse(int index) {
    for (int i = 0; i < possibilitiesArray.length; i++) {
      possibilitiesArray[i] = 0L;
    }
    addPossibility(index);
    entropy = 1;
    isEntropyUpdated = true;
  }

  // ===================================================================================================================
  // Getters
  // ===================================================================================================================
  public boolean hasPossibility(int index) {
    if (index >= possibilitiesCount) {
      return false;
    }

    long possibilities = possibilitiesArray[index / 64];
    index %= 64;
    return ((possibilities >>> index) & 1L) == 1L;
  }

  public int[] getPossibilities() {
    int[] result = new int[getEntropy()];
    int index = 0;

    if (result.length > 0) {
      for (int i = 0; i < possibilitiesArray.length; i++) {
        long value = possibilitiesArray[i];
  
        for (int j = 0; j < 64; j++) {
          if ((value & 1L) == 1) {
            result[index] = i * 64 + j;
            index++;
          }
          value >>>= 1;
        }
      }
    }
    
    return result;
  }

  public long[] getPossibilitiesArray() {
    return possibilitiesArray.clone();
  }

  public int getPossibilitiesCount() {
    return possibilitiesCount;
  }

  public int getEntropy() {
    if (!isEntropyUpdated) {
      calculateEntropy();
    }
    return entropy;
  }

  public boolean isCollapsed() {
    return getEntropy() == 1;
  }
}
