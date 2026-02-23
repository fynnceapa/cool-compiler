package cool.structures;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClassSymbol extends IdSymbol implements Scope {
    public ClassSymbol parent;
    protected Map<String, Symbol> symbols = new LinkedHashMap<>(); 
    protected Map<String, MethodSymbol> methods = new LinkedHashMap<>(); 

    public ClassSymbol(ClassSymbol parent, String name) {
        super(name);
        this.parent = parent;
    }

    public Integer getNrSymbols() { return symbols.size(); }

    @Override
    public boolean add(Symbol sym) {
        
        if (symbols.containsKey(sym.getName())) return false;
        symbols.put(sym.getName(), sym);
        return true;
    }

    @Override
    public boolean add(MethodSymbol sym) {
        
        if (methods.containsKey(sym.getName())) return false;
        methods.put(sym.getName(), sym);
        return true;
    }

    @Override
    public Symbol lookup(String s) {
        
        var sym = symbols.get(s);
        if (sym != null) return sym;
        if (parent != null) return parent.lookup(s);
        return null;
    }

    
    public MethodSymbol lookupMethod(String s) {
        var sym = methods.get(s);
        if (sym != null) return sym;
        if (parent != null) return parent.lookupMethod(s);
        return null;
    }

    @Override
    public ClassSymbol getParent() {
        return parent;
    }

    @Override
    public String getText() {
        return name;
    }
}