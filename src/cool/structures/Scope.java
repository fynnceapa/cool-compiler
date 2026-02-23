package cool.structures;

public interface Scope {
    public boolean add(Symbol sym);

    boolean add(MethodSymbol sym);

    public Symbol lookup(String str);
    
    public Scope getParent();

    String getText();
}
