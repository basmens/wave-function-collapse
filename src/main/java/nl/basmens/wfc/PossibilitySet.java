package nl.basmens.wfc;

import java.util.stream.IntStream;

public final class PossibilitySet {
  private static final long ALL_ONE = 0xffff_ffff_ffff_ffffL;

  private final long removeTrailingOnesLong;

  private final long[] possibilitiesArray;
  private final int possibilitiesCount;

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

  public boolean containsPossibility(int index) {
    if (index >= possibilitiesCount) {
      return false;
    }

    long possibilities = possibilitiesArray[index / 64];
    index %= 64;
    return possibilities >>> index == 0;
  }

  public void addPossibility(int index) {
    if (index >= possibilitiesCount) {
      return;
    }
    possibilitiesArray[index / 64] = possibilitiesArray[index / 64] | (1L << (index % 64));
  }

  public void removePossibility(int index) {
    if (index >= possibilitiesCount) {
      return;
    }
    possibilitiesArray[index / 64] = possibilitiesArray[index / 64] & ~(1L << (index % 64));
  }

  public PossibilitySet unionWith(PossibilitySet set) {
    long[] values = set.getPossibilitiesArray();

    for (int i = 0; i <= Math.max(possibilitiesCount, set.getPossibilitiesCount()) / 64; i++) {
      possibilitiesArray[i] = possibilitiesArray[i] | values[i];
    }

    int index = possibilitiesArray.length - 1;
    possibilitiesArray[index] = possibilitiesArray[index] & removeTrailingOnesLong;

    return this;
  }

  public PossibilitySet intersectionWith(PossibilitySet set) {
    long[] values = set.getPossibilitiesArray();

    for (int i = 0; i <= Math.max(possibilitiesCount, set.getPossibilitiesCount()) / 64; i++) {
      possibilitiesArray[i] = possibilitiesArray[i] & values[i];
    }

    return this;
  }

  public long[] getPossibilitiesArray() {
    return possibilitiesArray.clone();
  }

  public int getPossibilitiesCount() {
    return possibilitiesCount;
  }

  public int[] getPossibilities() {
    IntStream.Builder builder = IntStream.builder();
    for (int i = 0; i < possibilitiesArray.length; i++) {
      long value = possibilitiesArray[i];

      for (int j = 0; j < 64; j++) {
        if ((value & 1L) == 1) {
          builder.accept(i * 64 + j);
        }
        value >>>= 1;
      }
    }
    return builder.build().toArray();
  }

  public int getEntropy() {
    int count = 0;
    for (int i = 0; i < possibilitiesArray.length; i++) {
      long value = possibilitiesArray[i];

      for (int j = 0; j < 64; j++) {
        count += value & 1L;
        value >>>= 1;
      }
    }
    return count;
  }
}
