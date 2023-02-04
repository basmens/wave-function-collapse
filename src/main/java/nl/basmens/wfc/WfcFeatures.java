package nl.basmens.wfc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class WfcFeatures {
  private Module[] modules;
  private PossibilitySet[] possibleModulesUp;
  private PossibilitySet[] possibleModulesRight;
  private PossibilitySet[] possibleModulesDown;
  private PossibilitySet[] possibleModulesLeft;

  private boolean isLoopEdgesEnabledX;
  private boolean isLoopEdgesEnabledY;

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public WfcFeatures(Module[] modules, KeyPairsMap keyPairMap) {
    // Eliminate invalid modules and add rotation variants
    Set<String> possibleKeys = keyPairMap.getKeySet();
    this.modules = Arrays.stream(modules)
        .filter(m -> possibleKeys.contains(m.keyUp) && possibleKeys.contains(m.keyRight)
            && possibleKeys.contains(m.keyDown) && possibleKeys.contains(m.keyLeft))
        .flatMap(m -> Arrays.stream(m.rotations).mapToObj(r -> rotateModule(m, r)))
        .toArray(Module[]::new);

    // Create possibleModules* arrays and track modulesWith*Key
    int keysCount = possibleKeys.size();
    int moduleCount = this.modules.length;
    HashMap<String, PossibilitySet> modulesWithUpKey = new HashMap<>(keysCount);
    HashMap<String, PossibilitySet> modulesWithRightKey = new HashMap<>(keysCount);
    HashMap<String, PossibilitySet> modulesWithDownKey = new HashMap<>(keysCount);
    HashMap<String, PossibilitySet> modulesWithLeftKey = new HashMap<>(keysCount);
    possibleModulesUp = new PossibilitySet[moduleCount];
    possibleModulesRight = new PossibilitySet[moduleCount];
    possibleModulesDown = new PossibilitySet[moduleCount];
    possibleModulesLeft = new PossibilitySet[moduleCount];
    for (int i = 0; i < moduleCount; i++) {
      possibleModulesUp[i] = new PossibilitySet(moduleCount, false);
      possibleModulesRight[i] = new PossibilitySet(moduleCount, false);
      possibleModulesDown[i] = new PossibilitySet(moduleCount, false);
      possibleModulesLeft[i] = new PossibilitySet(moduleCount, false);

      modulesWithUpKey.computeIfAbsent(this.modules[i].keyUp, x -> new PossibilitySet(moduleCount, false))
          .addPossibility(i);
      modulesWithRightKey.computeIfAbsent(this.modules[i].keyRight, x -> new PossibilitySet(moduleCount, false))
          .addPossibility(i);
      modulesWithDownKey.computeIfAbsent(this.modules[i].keyDown, x -> new PossibilitySet(moduleCount, false))
          .addPossibility(i);
      modulesWithLeftKey.computeIfAbsent(this.modules[i].keyLeft, x -> new PossibilitySet(moduleCount, false))
          .addPossibility(i);
    }

    // Fill possibleModules* arrays
    for (int i = 0; i < moduleCount; i++) {
      Module module = this.modules[i];
      Set<String> possibleKeysUp = keyPairMap.getPairs(module.keyUp);
      possibleModulesUp[i] = possibleKeysUp.stream().map(modulesWithDownKey::get)
          .reduce(new PossibilitySet(moduleCount, false), PossibilitySet::unionWith);

      Set<String> possibleKeysRight = keyPairMap.getPairs(module.keyRight);
      possibleModulesRight[i] = possibleKeysRight.stream().map(modulesWithLeftKey::get)
          .reduce(new PossibilitySet(moduleCount, false), PossibilitySet::unionWith);

      Set<String> possibleKeysDown = keyPairMap.getPairs(module.keyDown);
      possibleModulesDown[i] = possibleKeysDown.stream().map(modulesWithUpKey::get)
          .reduce(new PossibilitySet(moduleCount, false), PossibilitySet::unionWith);

      Set<String> possibleKeysLeft = keyPairMap.getPairs(module.keyLeft);
      possibleModulesLeft[i] = possibleKeysLeft.stream().map(modulesWithRightKey::get)
          .reduce(new PossibilitySet(moduleCount, false), PossibilitySet::unionWith);
    }
  }

  private static Module rotateModule(Module m, int rot) {
    rot %= 4;
    if (rot == 0) {
      return new Module(m.keyUp, m.keyRight, m.keyDown, m.keyLeft, new int[] { 0 }, m.getChildItem());

    } else if (rot == 1) {
      return new Module(m.keyLeft, m.keyUp, m.keyRight, m.keyDown, new int[] { 1 }, m.getChildItem());

    } else if (rot == 2) {
      return new Module(m.keyDown, m.keyLeft, m.keyUp, m.keyRight, new int[] { 2 }, m.getChildItem());

    } else {
      return new Module(m.keyRight, m.keyDown, m.keyLeft, m.keyUp, new int[] { 3 }, m.getChildItem());
    }
  }

  // ===================================================================================================================
  // Getters and setters
  // ===================================================================================================================
  public boolean isLoopEdgesEnabledX() {
    return isLoopEdgesEnabledX;
  }

  public void setLoopEdgesEnabledX(boolean loopEdgesEnabledX) {
    this.isLoopEdgesEnabledX = loopEdgesEnabledX;
  }

  public boolean isLoopEdgesEnabledY() {
    return isLoopEdgesEnabledY;
  }

  public void setLoopEdgesEnabledY(boolean loopEdgesEnabledY) {
    this.isLoopEdgesEnabledY = loopEdgesEnabledY;
  }

  public Module[] getModules() {
    return modules.clone();
  }

  public Module getModule(int index) {
    return modules[index];
  }

  public PossibilitySet[] getPossibleModulesUp() {
    return possibleModulesUp.clone();
  }

  public PossibilitySet[] getPossibleModulesRight() {
    return possibleModulesRight.clone();
  }

  public PossibilitySet[] getPossibleModulesDown() {
    return possibleModulesDown.clone();
  }

  public PossibilitySet[] getPossibleModulesLeft() {
    return possibleModulesLeft.clone();
  }
}
