package cool.structures;

import java.util.LinkedHashMap;
import java.util.Map;

public class IdSymbol extends Symbol {

    protected String type;

    public IdSymbol(String name) {
        super(name);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
