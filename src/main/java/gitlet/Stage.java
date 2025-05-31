package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Stage implements Serializable {

    public Map<String, String> additionMap;

    public Set<String> removalSet;

    public Stage() {
        additionMap = new HashMap<>();
        removalSet = new HashSet<>();
    }

    public boolean isEmpty() {
        return additionMap.isEmpty() && removalSet.isEmpty();
    }

    public void clear() {
        additionMap.clear();
        removalSet.clear();
    }

    public Map<String, String> getAdditionMap() {
        return additionMap;
    }
}
