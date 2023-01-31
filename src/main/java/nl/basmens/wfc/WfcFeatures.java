package nl.basmens.wfc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import processing.core.PApplet;

public class WfcFeatures {
  private final Module[] modules;
  private final KeyPairsMap keyRules;

  private final ArrayList<IntModule> intModules;
  private final ArrayList<HashSet<IntModule>> modulesForUpKey;
  private final ArrayList<HashSet<IntModule>> modulesForRightKey;
  private final ArrayList<HashSet<IntModule>> modulesForDownKey;
  private final ArrayList<HashSet<IntModule>> modulesForLeftKey;

  private final boolean loopEdgesEnabled;

  // ===================================================================================================================
  // Constructor
  // ===================================================================================================================
  public WfcFeatures(Module[] modules, KeyPairsMap keyPairMap, boolean loopEdgesEnabled) {
    this.keyRules = new KeyPairsMap(keyPairMap);
    this.loopEdgesEnabled = loopEdgesEnabled;

    // Calculate a map to translate the key strings to integers
    HashMap<String, Integer> stringToIntMap = new HashMap<>();
    int i = 0;
    for (String s : this.keyRules.getKeySet()) {
      stringToIntMap.put(s, i);
      i++;
    }

    // Eliminate invalid modules and add rotation variants
    this.modules = Arrays.stream(modules)
        .filter(m -> stringToIntMap.containsKey(m.keyUp) && stringToIntMap.containsKey(m.keyRight)
            && stringToIntMap.containsKey(m.keyDown) && stringToIntMap.containsKey(m.keyLeft))
        .flatMap(m -> Arrays.stream(m.rotations).mapToObj(r -> rotateModule(m, r)))
        .toArray(Module[]::new);
    this.intModules = new ArrayList<>(this.modules.length);

    // Create empty arrays of sets
    int keysCount = stringToIntMap.size();
    ArrayList<HashSet<IntModule>> modulesWithUpKey = new ArrayList<>(keysCount);
    ArrayList<HashSet<IntModule>> modulesWithRightKey = new ArrayList<>(keysCount);
    ArrayList<HashSet<IntModule>> modulesWithDownKey = new ArrayList<>(keysCount);
    ArrayList<HashSet<IntModule>> modulesWithLeftKey = new ArrayList<>(keysCount);
    modulesForUpKey = new ArrayList<>(keysCount);
    modulesForRightKey = new ArrayList<>(keysCount);
    modulesForDownKey = new ArrayList<>(keysCount);
    modulesForLeftKey = new ArrayList<>(keysCount);
    for (i = 0; i < stringToIntMap.size(); i++) {
      modulesWithUpKey.add(new HashSet<>());
      modulesWithRightKey.add(new HashSet<>());
      modulesWithDownKey.add(new HashSet<>());
      modulesWithLeftKey.add(new HashSet<>());
      modulesForUpKey.add(new HashSet<>());
      modulesForRightKey.add(new HashSet<>());
      modulesForDownKey.add(new HashSet<>());
      modulesForLeftKey.add(new HashSet<>());
    }
    // Fill the intModules array and fill the modulesWith_Key sets
    for (i = 0; i < this.modules.length; i++) {
      Module mod = this.modules[i];
      IntModule m = new IntModule(stringToIntMap.get(mod.keyUp), stringToIntMap.get(mod.keyRight),
          stringToIntMap.get(mod.keyDown), stringToIntMap.get(mod.keyLeft), mod);
      this.intModules.add(m);

      modulesWithUpKey.get(m.keyUp).add(m);
      modulesWithRightKey.get(m.keyRight).add(m);
      modulesWithDownKey.get(m.keyDown).add(m);
      modulesWithLeftKey.get(m.keyLeft).add(m);
    }
    // Fill the modulesFor_Key arrays
    for (String k : this.keyRules.getKeySet()) {
      int intKey = stringToIntMap.get(k);
      HashSet<IntModule> modulesForUpKeySet = modulesForUpKey.get(intKey);
      HashSet<IntModule> modulesForRightKeySet = modulesForRightKey.get(intKey);
      HashSet<IntModule> modulesForDownKeySet = modulesForDownKey.get(intKey);
      HashSet<IntModule> modulesForLeftKeySet = modulesForLeftKey.get(intKey);

      this.keyRules.getPairs(k).forEach((String v) -> {
        int intValue = stringToIntMap.get(v);
        modulesForUpKeySet.addAll(modulesWithDownKey.get(intValue));
        modulesForDownKeySet.addAll(modulesWithUpKey.get(intValue));
        modulesForRightKeySet.addAll(modulesWithLeftKey.get(intValue));
        modulesForLeftKeySet.addAll(modulesWithRightKey.get(intValue));
      });
    }

    PApplet.println();
    stringToIntMap.entrySet().forEach(entry -> PApplet.println(entry.getValue() + " - " + entry.getKey()));
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
  // Getters
  // ===================================================================================================================
  public boolean isLoopEdgesEnabled() {
    return loopEdgesEnabled;
  }

  public Module[] getModules() {
    return modules.clone();
  }

  public KeyPairsMap getKeyRules() {
    return keyRules;
  }

  public List<IntModule> getIntModules() {
    return (List<IntModule>) intModules.clone();
  }

  public List<HashSet<IntModule>> getModulesForUpKey() {
    return new ArrayList<>(modulesForUpKey.stream().map(s -> (HashSet<IntModule>) s.clone()).toList());
  }

  public List<HashSet<IntModule>> getModulesForRightKey() {
    return new ArrayList<>(modulesForRightKey.stream().map(s -> (HashSet<IntModule>) s.clone()).toList());
  }

  public List<HashSet<IntModule>> getModulesForDownKey() {
    return new ArrayList<>(modulesForDownKey.stream().map(s -> (HashSet<IntModule>) s.clone()).toList());
  }

  public List<HashSet<IntModule>> getModulesForLeftKey() {
    return new ArrayList<>(modulesForLeftKey.stream().map(s -> (HashSet<IntModule>) s.clone()).toList());
  }
}
