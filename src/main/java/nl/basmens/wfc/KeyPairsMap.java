package nl.basmens.wfc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class KeyPairsMap {
  private HashMap<String, HashSet<String>> pairs = new HashMap<>();

  public KeyPairsMap() {
  }

  public KeyPairsMap(KeyPairsMap toBeCloned) {
    pairs = (HashMap<String, HashSet<String>>) toBeCloned.getPairMap();
  }

  public void addPair(String a, String b) {
    pairs.computeIfAbsent(a, s -> new HashSet<>()).add(b);
    pairs.computeIfAbsent(b, s -> new HashSet<>()).add(a);
  }

  public boolean checkPair(String a, String b) {
    return pairs.get(a).contains(b);
  }

  public Set<String> getKeySet() {
    return pairs.keySet();
  }

  @SuppressWarnings("unchecked")
  public Set<String> getPairs(String key) {
    return (Set<String>) pairs.get(key).clone();
  }

  @SuppressWarnings("unchecked")
  public Map<String, HashSet<String>> getPairMap() {
    HashMap<String, HashSet<String>> result = new HashMap<>();
    for (Entry<String, HashSet<String>> e : pairs.entrySet()) {
      result.put(e.getKey(), (HashSet<String>) e.getValue().clone());
    }
    return result;
  }
}
