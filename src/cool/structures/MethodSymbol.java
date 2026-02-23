package cool.structures;

import java.util.LinkedHashMap;
import java.util.Map;

public class MethodSymbol extends IdSymbol implements Scope {

    public Scope parent;

    protected Map<String, Symbol> symbols = new LinkedHashMap<>();

    public MethodSymbol(Scope parent, String name) {
        super(name);
        this.parent = parent;
    }

    public Integer getNrSymbols() { return symbols.size(); }

    public Symbol getSymbol(int idx) {
        int currentIndex = 0;
        for (Symbol value : symbols.values()) {
            if (currentIndex == idx) {
                return value;
            }
            currentIndex++;
        }
        return null;
    }

    @Override
    public boolean add(Symbol sym) {
        if (symbols.containsKey(sym.getName())) return false;
        symbols.put(sym.getName(), sym);
        return true;
    }

    @Override
    public boolean add(MethodSymbol sym) {
        if (symbols.containsKey(sym.getName())) return false;
        symbols.put(sym.getName(), sym);
        return true;
    }

    @Override
    public Symbol lookup(String str) {
        var sym = symbols.get(str);
        if (sym != null) return sym;
        if (parent != null) return parent.lookup(str);
        return null;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public String getText() {
        return getName();
    }

}