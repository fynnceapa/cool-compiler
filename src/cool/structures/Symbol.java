package cool.structures;

import java.util.LinkedHashMap;
import java.util.Map;

public class Symbol {
    protected String name;
    
    public Symbol(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
