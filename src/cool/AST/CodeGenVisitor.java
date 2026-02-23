package cool.AST;

import cool.structures.*;
import org.antlr.v4.runtime.Token;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.*;

public class CodeGenVisitor implements ASTVisitor<ST> {

    private STGroup templates = new STGroupFile("cool/modules/cgen.stg");

    private int labelCounter = 0;
    private int dispatchCounter = 0;

    
    private Map<String, Integer> classTags = new LinkedHashMap<>();
    
    private Map<String, Integer> classMinTag = new HashMap<>();
    private Map<String, Integer> classMaxTag = new HashMap<>();

    
    private Map<String, String> stringConstants = new LinkedHashMap<>();
    private Map<Integer, String> intConstants = new LinkedHashMap<>();
    private int stringConstCounter = 0;
    private int intConstCounter = 0;

    
    private List<String> allClasses = new ArrayList<>();

    
    private Map<String, Map<String, Integer>> classMethodOffsets = new HashMap<>();
    
    private Map<String, List<String>> classDispTabs = new HashMap<>();

    
    
    private Map<String, Map<String, Integer>> classAttrOffsets = new HashMap<>();
    
    private Map<String, Integer> classAttrCount = new HashMap<>();

    
    private String currentClassName;
    private ClassSymbol currentClassSymbol;

    
    private int currentLocalOffset = 0;
    private Map<String, Integer> localOffsets = new HashMap<>();
    private Map<String, String> localTypes = new HashMap<>();

    
    private int currentParamCount = 0;

    
    private Program programAST;

    
    private Map<String, String> fileNameLabels = new HashMap<>();

    private String newLabel() {
        return "label" + (labelCounter++);
    }

    private String newDispatchLabel() {
        return "dispatch" + (dispatchCounter++);
    }

    
    private String getIntConst(int value) {
        if (intConstants.containsKey(value)) {
            return intConstants.get(value);
        }
        String label = "int_const" + intConstCounter++;
        intConstants.put(value, label);
        return label;
    }

    
    private String getStringConst(String value) {
        if (stringConstants.containsKey(value)) {
            return stringConstants.get(value);
        }
        String label = "str_const" + stringConstCounter++;
        stringConstants.put(value, label);
        return label;
    }

    
    private int stringSize(String s) {
        
        int len = s.length();
        return 4 + (len + 4) / 4;
    }

    private void setupClassHierarchy() {
        
        
        List<String> basicClasses = Arrays.asList("Object", "IO");

        
        Map<String, List<String>> children = new HashMap<>();
        children.put("Object", new ArrayList<>());

        
        DefaultScope globals = (DefaultScope) SymbolTable.globals;

        
        Map<String, String> parentMap = new HashMap<>();
        parentMap.put("Object", null);
        parentMap.put("IO", "Object");
        parentMap.put("Int", "Object");
        parentMap.put("String", "Object");
        parentMap.put("Bool", "Object");

        for (Class cls : programAST.classList) {
            String parent = cls.inherits != null ? cls.inherits : "Object";
            parentMap.put(cls.className, parent);
        }

        
        for (String cls : parentMap.keySet()) {
            children.putIfAbsent(cls, new ArrayList<>());
        }
        for (Map.Entry<String, String> e : parentMap.entrySet()) {
            if (e.getValue() != null) {
                children.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
        }

        
        
        int tag = 0;

        
        Stack<String> stack = new Stack<>();
        stack.push("Object");
        Set<String> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            String cls = stack.pop();
            if (visited.contains(cls))
                continue;

            
            if (cls.equals("Int") || cls.equals("String") || cls.equals("Bool")) {
                continue;
            }

            visited.add(cls);
            classTags.put(cls, tag);
            classMinTag.put(cls, tag);
            allClasses.add(cls);
            tag++;

            
            List<String> kids = children.getOrDefault(cls, new ArrayList<>());
            for (int i = kids.size() - 1; i >= 0; i--) {
                String kid = kids.get(i);
                if (!kid.equals("Int") && !kid.equals("String") && !kid.equals("Bool")) {
                    stack.push(kid);
                }
            }
        }

        
        classTags.put("Int", tag);
        classMinTag.put("Int", tag);
        classMaxTag.put("Int", tag);
        allClasses.add("Int");
        tag++;

        classTags.put("String", tag);
        classMinTag.put("String", tag);
        classMaxTag.put("String", tag);
        allClasses.add("String");
        tag++;

        classTags.put("Bool", tag);
        classMinTag.put("Bool", tag);
        classMaxTag.put("Bool", tag);
        allClasses.add("Bool");
        tag++;

        
        for (int i = allClasses.size() - 1; i >= 0; i--) {
            String cls = allClasses.get(i);
            int maxTag = classTags.get(cls);
            for (String child : children.getOrDefault(cls, new ArrayList<>())) {
                if (classMaxTag.containsKey(child)) {
                    maxTag = Math.max(maxTag, classMaxTag.get(child));
                }
            }
            classMaxTag.put(cls, maxTag);
        }
    }

    private void setupMethodTables() {
        
        
        List<String> objectMethods = Arrays.asList("Object.abort", "Object.type_name", "Object.copy");
        classDispTabs.put("Object", new ArrayList<>(objectMethods));

        Map<String, Integer> objectOffsets = new LinkedHashMap<>();
        objectOffsets.put("abort", 0);
        objectOffsets.put("type_name", 4);
        objectOffsets.put("copy", 8);
        classMethodOffsets.put("Object", objectOffsets);

        
        List<String> ioMethods = new ArrayList<>(objectMethods);
        ioMethods.addAll(Arrays.asList("IO.out_string", "IO.out_int", "IO.in_string", "IO.in_int"));
        classDispTabs.put("IO", ioMethods);

        Map<String, Integer> ioOffsets = new LinkedHashMap<>(objectOffsets);
        ioOffsets.put("out_string", 12);
        ioOffsets.put("out_int", 16);
        ioOffsets.put("in_string", 20);
        ioOffsets.put("in_int", 24);
        classMethodOffsets.put("IO", ioOffsets);

        
        classDispTabs.put("Int", new ArrayList<>(objectMethods));
        classMethodOffsets.put("Int", new LinkedHashMap<>(objectOffsets));

        
        List<String> stringMethods = new ArrayList<>(objectMethods);
        stringMethods.addAll(Arrays.asList("String.length", "String.concat", "String.substr"));
        classDispTabs.put("String", stringMethods);

        Map<String, Integer> stringOffsets = new LinkedHashMap<>(objectOffsets);
        stringOffsets.put("length", 12);
        stringOffsets.put("concat", 16);
        stringOffsets.put("substr", 20);
        classMethodOffsets.put("String", stringOffsets);

        classDispTabs.put("Bool", new ArrayList<>(objectMethods));
        classMethodOffsets.put("Bool", new LinkedHashMap<>(objectOffsets));

        
        for (String className : allClasses) {
            if (classDispTabs.containsKey(className))
                continue;

            ClassSymbol classSym = (ClassSymbol) SymbolTable.globals.lookup(className);
            if (classSym == null)
                continue;

            
            String parentName = classSym.parent != null ? classSym.parent.getName() : "Object";
            List<String> parentDispTab = classDispTabs.getOrDefault(parentName, new ArrayList<>());
            Map<String, Integer> parentOffsets = classMethodOffsets.getOrDefault(parentName, new LinkedHashMap<>());

            List<String> dispTab = new ArrayList<>(parentDispTab);
            Map<String, Integer> offsets = new LinkedHashMap<>(parentOffsets);

            
            for (Class cls : programAST.classList) {
                if (cls.className.equals(className)) {
                    for (Feature f : cls.featureList) {
                        if (f instanceof FuncDef) {
                            String methodName = ((FuncDef) f).featureName;
                            String fullName = className + "." + methodName;

                            if (offsets.containsKey(methodName)) {
                                
                                int offset = offsets.get(methodName);
                                int index = offset / 4;
                                dispTab.set(index, fullName);
                            } else {
                                
                                int offset = dispTab.size() * 4;
                                offsets.put(methodName, offset);
                                dispTab.add(fullName);
                            }
                        }
                    }
                    break;
                }
            }

            classDispTabs.put(className, dispTab);
            classMethodOffsets.put(className, offsets);
        }
    }

    private void setupAttributeTables() {
        
        classAttrOffsets.put("Object", new LinkedHashMap<>());
        classAttrCount.put("Object", 0);

        classAttrOffsets.put("IO", new LinkedHashMap<>());
        classAttrCount.put("IO", 0);

        
        Map<String, Integer> intAttrs = new LinkedHashMap<>();
        intAttrs.put("_val", 12);
        classAttrOffsets.put("Int", intAttrs);
        classAttrCount.put("Int", 1);

        
        Map<String, Integer> stringAttrs = new LinkedHashMap<>();
        stringAttrs.put("_len", 12);
        stringAttrs.put("_str", 16);
        classAttrOffsets.put("String", stringAttrs);
        classAttrCount.put("String", 2);

        
        Map<String, Integer> boolAttrs = new LinkedHashMap<>();
        boolAttrs.put("_val", 12);
        classAttrOffsets.put("Bool", boolAttrs);
        classAttrCount.put("Bool", 1);

        
        for (String className : allClasses) {
            if (classAttrOffsets.containsKey(className))
                continue;

            ClassSymbol classSym = (ClassSymbol) SymbolTable.globals.lookup(className);
            if (classSym == null)
                continue;

            String parentName = classSym.parent != null ? classSym.parent.getName() : "Object";
            Map<String, Integer> parentAttrs = classAttrOffsets.getOrDefault(parentName, new LinkedHashMap<>());
            int parentCount = classAttrCount.getOrDefault(parentName, 0);

            Map<String, Integer> attrs = new LinkedHashMap<>(parentAttrs);
            int count = parentCount;

            
            for (Class cls : programAST.classList) {
                if (cls.className.equals(className)) {
                    for (Feature f : cls.featureList) {
                        if (f instanceof VarDef) {
                            String attrName = ((VarDef) f).featureName;
                            int offset = 12 + count * 4;
                            attrs.put(attrName, offset);
                            count++;
                        }
                    }
                    break;
                }
            }

            classAttrOffsets.put(className, attrs);
            classAttrCount.put(className, count);
        }
    }

    private void collectConstants() {
        
        getStringConst("");

        
        for (String className : allClasses) {
            getStringConst(className);
        }

        
        collectConstantsFromProgram(programAST);

        
        getIntConst(0);
    }

    private void collectConstantsFromProgram(Program program) {
        for (Class cls : program.classList) {
            
            if (cls.fileName != null) {
                String fileName = new java.io.File(cls.fileName).getName();
                getStringConst(fileName);
                fileNameLabels.put(cls.fileName, getStringConst(fileName));
            }

            for (Feature f : cls.featureList) {
                if (f instanceof VarDef) {
                    VarDef v = (VarDef) f;
                    if (v.initExpr != null) {
                        collectConstantsFromExpr(v.initExpr);
                    }
                } else if (f instanceof FuncDef) {
                    FuncDef m = (FuncDef) f;
                    collectConstantsFromExpr(m.funcBody);
                }
            }
        }
    }

    private void collectConstantsFromExpr(Expression expr) {
        if (expr == null)
            return;

        if (expr instanceof Int) {
            int val = Integer.parseInt(((Int) expr).token.getText());
            getIntConst(val);
        } else if (expr instanceof StringT) {
            String val = parseStringLiteral(((StringT) expr).token.getText());
            getStringConst(val);
        } else if (expr instanceof BinaryOp) {
            collectConstantsFromExpr(((BinaryOp) expr).left);
            collectConstantsFromExpr(((BinaryOp) expr).right);
        } else if (expr instanceof UnaryOp) {
            collectConstantsFromExpr(((UnaryOp) expr).expr);
        } else if (expr instanceof If) {
            collectConstantsFromExpr(((If) expr).condition);
            collectConstantsFromExpr(((If) expr).thenBranch);
            collectConstantsFromExpr(((If) expr).elseBranch);
        } else if (expr instanceof While) {
            collectConstantsFromExpr(((While) expr).condition);
            collectConstantsFromExpr(((While) expr).body);
        } else if (expr instanceof Block) {
            for (Expression e : ((Block) expr).exprList) {
                collectConstantsFromExpr(e);
            }
        } else if (expr instanceof Let) {
            for (LocalPrm p : ((Let) expr).localPrmList) {
                if (p.val != null) {
                    collectConstantsFromExpr(p.val);
                }
            }
            collectConstantsFromExpr(((Let) expr).body);
        } else if (expr instanceof Case) {
            collectConstantsFromExpr(((Case) expr).expr);
            for (caseBranch b : ((Case) expr).branchList) {
                collectConstantsFromExpr(b.expr);
            }
        } else if (expr instanceof Call) {
            if (((Call) expr).prefix != null) {
                collectConstantsFromExpr(((Call) expr).prefix);
            }
            for (Expression arg : ((Call) expr).args) {
                collectConstantsFromExpr(arg);
            }
        } else if (expr instanceof InitCall) {
            for (Expression arg : ((InitCall) expr).args) {
                collectConstantsFromExpr(arg);
            }
        } else if (expr instanceof Assign) {
            collectConstantsFromExpr(((Assign) expr).expr);
        }
    }

    private String parseStringLiteral(String s) {
        
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n':
                        sb.append('\n');
                        i++;
                        break;
                    case 't':
                        sb.append('\t');
                        i++;
                        break;
                    case 'b':
                        sb.append('\b');
                        i++;
                        break;
                    case 'f':
                        sb.append('\f');
                        i++;
                        break;
                    case '\\':
                        sb.append('\\');
                        i++;
                        break;
                    case '"':
                        sb.append('"');
                        i++;
                        break;
                    case '0':
                        sb.append('\0');
                        i++;
                        break;
                    default:
                        sb.append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String escapeForAsm(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '\n')
                sb.append("\\n");
            else if (c == '\t')
                sb.append("\\t");
            else if (c == '\b')
                sb.append("\\b");
            else if (c == '\f')
                sb.append("\\f");
            else if (c == '\\')
                sb.append("\\\\");
            else if (c == '"')
                sb.append("\\\"");
            else if (c == '\0')
                sb.append("\\0");
            else if (c < 32 || c > 126)
                sb.append(String.format("\\%03o", (int) c));
            else
                sb.append(c);
        }
        return sb.toString();
    }

    private String generateDataSection() {
        StringBuilder sb = new StringBuilder();

        
        sb.append("_int_tag:\n    .word   ").append(classTags.get("Int")).append("\n");
        sb.append("_string_tag:\n    .word   ").append(classTags.get("String")).append("\n");
        sb.append("_bool_tag:\n    .word   ").append(classTags.get("Bool")).append("\n");

        
        for (Map.Entry<String, String> e : stringConstants.entrySet()) {
            String value = e.getKey();
            String label = e.getValue();
            int len = value.length();
            String lenLabel = getIntConst(len);
            int size = stringSize(value);

            sb.append(label).append(":\n");
            sb.append("    .word   ").append(classTags.get("String")).append("\n");
            sb.append("    .word   ").append(size).append("\n");
            sb.append("    .word   String_dispTab\n");
            sb.append("    .word   ").append(lenLabel).append("\n");
            sb.append("    .asciiz \"").append(escapeForAsm(value)).append("\"\n");
            sb.append("    .align  2\n");
        }

        
        for (Map.Entry<Integer, String> e : intConstants.entrySet()) {
            int value = e.getKey();
            String label = e.getValue();
            sb.append(label).append(":\n");
            sb.append("    .word   ").append(classTags.get("Int")).append("\n");
            sb.append("    .word   4\n");
            sb.append("    .word   Int_dispTab\n");
            sb.append("    .word   ").append(value).append("\n");
        }

        
        sb.append("bool_const0:\n");
        sb.append("    .word   ").append(classTags.get("Bool")).append("\n");
        sb.append("    .word   4\n");
        sb.append("    .word   Bool_dispTab\n");
        sb.append("    .word   0\n");

        sb.append("bool_const1:\n");
        sb.append("    .word   ").append(classTags.get("Bool")).append("\n");
        sb.append("    .word   4\n");
        sb.append("    .word   Bool_dispTab\n");
        sb.append("    .word   1\n");

        
        sb.append("class_nameTab:\n");
        for (String className : allClasses) {
            String strLabel = stringConstants.get(className);
            sb.append("    .word   ").append(strLabel).append("\n");
        }

        
        sb.append("class_objTab:\n");
        for (String className : allClasses) {
            sb.append("    .word   ").append(className).append("_protObj\n");
            sb.append("    .word   ").append(className).append("_init\n");
        }

        
        for (String className : allClasses) {
            int tag = classTags.get(className);
            int attrCount = classAttrCount.getOrDefault(className, 0);
            int size = 3 + attrCount;

            sb.append(className).append("_protObj:\n");
            sb.append("    .word   ").append(tag).append("\n");
            sb.append("    .word   ").append(size).append("\n");
            sb.append("    .word   ").append(className).append("_dispTab\n");

            
            if (className.equals("Int")) {
                sb.append("    .word   0\n");
            } else if (className.equals("Bool")) {
                sb.append("    .word   0\n");
            } else if (className.equals("String")) {
                sb.append("    .word   ").append(getIntConst(0)).append("\n");
                sb.append("    .asciiz \"\"\n");
                sb.append("    .align  2");
            } else {
                
                generateProtObjAttrs(sb, className);
            }
            sb.append("\n");
        }

        
        for (String className : allClasses) {
            sb.append(className).append("_dispTab:\n");
            List<String> methods = classDispTabs.get(className);
            if (methods != null) {
                for (String method : methods) {
                    sb.append("    .word   ").append(method).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private void generateProtObjAttrs(StringBuilder sb, String className) {
        Map<String, Integer> attrs = classAttrOffsets.get(className);
        if (attrs == null || attrs.isEmpty())
            return;

        
        List<Map.Entry<String, Integer>> sortedAttrs = new ArrayList<>(attrs.entrySet());
        sortedAttrs.sort(Comparator.comparingInt(Map.Entry::getValue));

        
        ClassSymbol classSym = (ClassSymbol) SymbolTable.globals.lookup(className);

        for (Map.Entry<String, Integer> attrEntry : sortedAttrs) {
            String attrName = attrEntry.getKey();

            
            String attrType = getAttributeType(className, attrName);

            
            if (attrType != null) {
                if (attrType.equals("Int")) {
                    sb.append("    .word   ").append(getIntConst(0)).append("\n");
                } else if (attrType.equals("Bool")) {
                    sb.append("    .word   bool_const0\n");
                } else if (attrType.equals("String")) {
                    sb.append("    .word   ").append(getStringConst("")).append("\n");
                } else {
                    
                    sb.append("    .word   0\n");
                }
            } else {
                sb.append("    .word   0\n");
            }
        }
    }

    private String getAttributeType(String className, String attrName) {
        
        ClassSymbol classSym = (ClassSymbol) SymbolTable.globals.lookup(className);
        while (classSym != null) {
            
            for (Class cls : programAST.classList) {
                if (cls.className.equals(classSym.getName())) {
                    for (Feature f : cls.featureList) {
                        if (f instanceof VarDef && ((VarDef) f).featureName.equals(attrName)) {
                            return ((VarDef) f).type.getText();
                        }
                    }
                }
            }
            classSym = classSym.parent;
        }
        return null;
    }

    private String generateTextSection() {
        StringBuilder sb = new StringBuilder();

        
        for (String className : allClasses) {
            sb.append(generateInitMethod(className));
        }

        
        for (Class cls : programAST.classList) {
            currentClassName = cls.className;
            currentClassSymbol = (ClassSymbol) SymbolTable.globals.lookup(cls.className);

            for (Feature f : cls.featureList) {
                if (f instanceof FuncDef) {
                    FuncDef func = (FuncDef) f;
                    sb.append(generateMethod(cls, func));
                }
            }
        }

        return sb.toString();
    }

    private String generateInitMethod(String className) {
        StringBuilder sb = new StringBuilder();

        sb.append(className).append("_init:\n");
        sb.append("    addiu   $sp $sp -12\n");
        sb.append("    sw      $fp 12($sp)\n");
        sb.append("    sw      $s0 8($sp)\n");
        sb.append("    sw      $ra 4($sp)\n");
        sb.append("    addiu   $fp $sp 4\n");
        sb.append("    move    $s0 $a0\n");

        
        if (!className.equals("Object")) {
            ClassSymbol classSym = (ClassSymbol) SymbolTable.globals.lookup(className);
            String parentName = "Object";
            if (classSym != null && classSym.parent != null) {
                parentName = classSym.parent.getName();
            }
            sb.append("    jal     ").append(parentName).append("_init\n");
        }

        
        for (Class cls : programAST.classList) {
            if (cls.className.equals(className)) {
                currentClassName = className;
                currentClassSymbol = (ClassSymbol) SymbolTable.globals.lookup(className);

                for (Feature f : cls.featureList) {
                    if (f instanceof VarDef) {
                        VarDef v = (VarDef) f;
                        if (v.initExpr != null) {
                            
                            String code = generateExpr(v.initExpr);
                            sb.append(code);
                            
                            int offset = classAttrOffsets.get(className).get(v.featureName);
                            sb.append("    sw      $a0 ").append(offset).append("($s0)\n");
                        }
                    }
                }
                break;
            }
        }

        sb.append("    move    $a0 $s0\n");
        sb.append("    lw      $fp 12($sp)\n");
        sb.append("    lw      $s0 8($sp)\n");
        sb.append("    lw      $ra 4($sp)\n");
        sb.append("    addiu   $sp $sp 12\n");
        sb.append("    jr      $ra\n");

        return sb.toString();
    }

    private String generateMethod(Class cls, FuncDef func) {
        StringBuilder sb = new StringBuilder();

        currentClassName = cls.className;
        currentClassSymbol = (ClassSymbol) SymbolTable.globals.lookup(cls.className);
        currentLocalOffset = 0;
        localOffsets.clear();
        currentParamCount = func.formalParmList.size();

        
        int paramOffset = 12;
        for (FormalPrm param : func.formalParmList) {
            localOffsets.put(param.prmName, paramOffset);
            localTypes.put(param.prmName, param.type.getText());
            paramOffset += 4;
        }

        
        int localsSize = countLocals(func.funcBody) * 4;

        sb.append(cls.className).append(".").append(func.featureName).append(":\n");
        sb.append("    addiu   $sp $sp -12\n");
        sb.append("    sw      $fp 12($sp)\n");
        sb.append("    sw      $s0 8($sp)\n");
        sb.append("    sw      $ra 4($sp)\n");
        sb.append("    addiu   $fp $sp 4\n");

        if (localsSize > 0) {
            sb.append("    addiu   $sp $sp -").append(localsSize).append("   # locals alloc\n");
        }

        sb.append("    move    $s0 $a0\n");

        
        String bodyCode = generateExpr(func.funcBody);
        sb.append(bodyCode);

        if (localsSize > 0) {
            sb.append("    addiu   $sp $sp ").append(localsSize).append("    # locals free\n");
        }

        sb.append("    lw      $fp 12($sp)\n");
        sb.append("    lw      $s0 8($sp)\n");
        sb.append("    lw      $ra 4($sp)\n");
        sb.append("    addiu   $sp $sp 12\n");

        if (currentParamCount > 0) {
            sb.append("    addiu   $sp $sp ").append(currentParamCount * 4).append("    # params free\n");
        }

        sb.append("    jr      $ra\n");

        return sb.toString();
    }

    private int countLocals(Expression expr) {
        if (expr == null)
            return 0;

        if (expr instanceof Let) {
            Let let = (Let) expr;
            return let.localPrmList.size() + countLocals(let.body);
        } else if (expr instanceof Block) {
            int max = 0;
            for (Expression e : ((Block) expr).exprList) {
                max = Math.max(max, countLocals(e));
            }
            return max;
        } else if (expr instanceof If) {
            return Math.max(countLocals(((If) expr).thenBranch), countLocals(((If) expr).elseBranch));
        } else if (expr instanceof While) {
            return countLocals(((While) expr).body);
        } else if (expr instanceof Case) {
            int max = 1; 
            for (caseBranch b : ((Case) expr).branchList) {
                max = Math.max(max, 1 + countLocals(b.expr));
            }
            return max;
        }
        return 0;
    }

    private String generateExpr(Expression expr) {
        if (expr == null)
            return "";

        if (expr instanceof Int) {
            int val = Integer.parseInt(((Int) expr).token.getText());
            String label = getIntConst(val);
            return "    la      $a0 " + label + "\n";
        }

        if (expr instanceof StringT) {
            String val = parseStringLiteral(((StringT) expr).token.getText());
            String label = getStringConst(val);
            return "    la      $a0 " + label + "\n";
        }

        if (expr instanceof Booll) {
            String val = ((Booll) expr).token.getText().toLowerCase();
            String label = val.equals("true") ? "bool_const1" : "bool_const0";
            return "    la      $a0 " + label + "\n";
        }

        if (expr instanceof Id) {
            return generateId((Id) expr);
        }

        if (expr instanceof Assign) {
            return generateAssign((Assign) expr);
        }

        if (expr instanceof Block) {
            return generateBlock((Block) expr);
        }

        if (expr instanceof If) {
            return generateIf((If) expr);
        }

        if (expr instanceof While) {
            return generateWhile((While) expr);
        }

        if (expr instanceof Let) {
            return generateLet((Let) expr);
        }

        if (expr instanceof BinaryOp) {
            return generateBinaryOp((BinaryOp) expr);
        }

        if (expr instanceof UnaryOp) {
            return generateUnaryOp((UnaryOp) expr);
        }

        if (expr instanceof Call) {
            return generateCall((Call) expr);
        }

        if (expr instanceof InitCall) {
            return generateInitCall((InitCall) expr);
        }

        if (expr instanceof New) {
            return generateNew((New) expr);
        }

        if (expr instanceof Case) {
            return generateCase((Case) expr);
        }

        return "";
    }

    private String generateId(Id id) {
        String name = id.token.getText();

        if (name.equals("self")) {
            return "    move    $a0 $s0\n";
        }

        
        if (localOffsets.containsKey(name)) {
            int offset = localOffsets.get(name);
            return "    lw      $a0 " + offset + "($fp)\n";
        }

        
        Map<String, Integer> attrs = classAttrOffsets.get(currentClassName);
        if (attrs != null && attrs.containsKey(name)) {
            int offset = attrs.get(name);
            return "    lw      $a0 " + offset + "($s0)\n";
        }

        return "";
    }

    private String generateAssign(Assign assign) {
        StringBuilder sb = new StringBuilder();

        
        sb.append(generateExpr(assign.expr));

        String name = assign.varName;

        
        if (localOffsets.containsKey(name)) {
            int offset = localOffsets.get(name);
            sb.append("    sw      $a0 ").append(offset).append("($fp)\n");
        } else {
            
            Map<String, Integer> attrs = classAttrOffsets.get(currentClassName);
            if (attrs != null && attrs.containsKey(name)) {
                int offset = attrs.get(name);
                sb.append("    sw      $a0 ").append(offset).append("($s0)\n");
            }
        }

        return sb.toString();
    }

    private String generateBlock(Block block) {
        StringBuilder sb = new StringBuilder();
        for (Expression e : block.exprList) {
            sb.append(generateExpr(e));
        }
        return sb.toString();
    }

    private String generateIf(If iff) {
        StringBuilder sb = new StringBuilder();
        String elseLabel = newLabel();
        String endLabel = newLabel();

        
        sb.append(generateExpr(iff.condition));
        sb.append("    lw      $t1 12($a0)     # bool slot\n");
        sb.append("    beqz    $t1 ").append(elseLabel).append("\n");

        
        sb.append(generateExpr(iff.thenBranch));
        sb.append("    b       ").append(endLabel).append("\n");

        
        sb.append(elseLabel).append(":\n");
        sb.append(generateExpr(iff.elseBranch));

        sb.append(endLabel).append(":\n");

        return sb.toString();
    }

    private String generateWhile(While whilee) {
        StringBuilder sb = new StringBuilder();
        String loopLabel = newLabel();
        String endLabel = newLabel();

        sb.append(loopLabel).append(":\n");

        
        sb.append(generateExpr(whilee.condition));
        sb.append("    lw      $t1 12($a0)     # bool slot\n");
        sb.append("    beqz    $t1 ").append(endLabel).append("\n");

        
        sb.append(generateExpr(whilee.body));
        sb.append("    b       ").append(loopLabel).append("\n");

        sb.append(endLabel).append(":\n");
        sb.append("    move    $a0 $zero\n");

        return sb.toString();
    }

    private String generateLet(Let let) {
        StringBuilder sb = new StringBuilder();

        int savedOffset = currentLocalOffset;
        List<String> addedVars = new ArrayList<>();

        
        for (LocalPrm prm : let.localPrmList) {
            currentLocalOffset -= 4;
            int offset = currentLocalOffset;
            localOffsets.put(prm.name, offset);
            localTypes.put(prm.name, prm.type.getText());
            addedVars.add(prm.name);

            if (prm.val != null) {
                
                sb.append(generateExpr(prm.val));
            } else {
                
                String typeName = prm.type.getText();
                if (typeName.equals("Int")) {
                    sb.append("    la      $a0 ").append(getIntConst(0)).append("\n");
                } else if (typeName.equals("Bool")) {
                    sb.append("    la      $a0 bool_const0\n");
                } else if (typeName.equals("String")) {
                    sb.append("    la      $a0 ").append(getStringConst("")).append("\n");
                } else {
                    sb.append("    move    $a0 $zero\n");
                }
            }
            sb.append("    sw      $a0 ").append(offset).append("($fp)\n");
        }

        
        sb.append(generateExpr(let.body));

        
        for (String var : addedVars) {
            localOffsets.remove(var);
            localTypes.remove(var);
        }
        currentLocalOffset = savedOffset;

        return sb.toString();
    }

    private String generateBinaryOp(BinaryOp op) {
        StringBuilder sb = new StringBuilder();
        String opText = op.operator.getText();

        if (opText.equals("+") || opText.equals("-") || opText.equals("*") || opText.equals("/")) {
            
            sb.append(generateExpr(op.left));
            sb.append("    sw      $a0 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");

            sb.append(generateExpr(op.right));
            sb.append("    jal     Object.copy\n");

            sb.append("    lw      $t1 4($sp)\n");
            sb.append("    addiu   $sp $sp 4\n");
            sb.append("    lw      $t1 12($t1)     # int slot\n");
            sb.append("    lw      $t2 12($a0)     # int slot\n");

            switch (opText) {
                case "+":
                    sb.append("    add     $t1 $t1 $t2\n");
                    break;
                case "-":
                    sb.append("    sub     $t1 $t1 $t2\n");
                    break;
                case "*":
                    sb.append("    mul     $t1 $t1 $t2\n");
                    break;
                case "/":
                    sb.append("    div     $t1 $t1 $t2\n");
                    break;
            }

            sb.append("    sw      $t1 12($a0)     # int slot\n");

        } else if (opText.equals("<") || opText.equals("<=")) {
            
            String trueLabel = newLabel();
            String endLabel = newLabel();

            sb.append(generateExpr(op.left));
            sb.append("    sw      $a0 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");

            sb.append(generateExpr(op.right));

            sb.append("    lw      $t1 4($sp)\n");
            sb.append("    addiu   $sp $sp 4\n");
            sb.append("    lw      $t1 12($t1)     # int slot\n");
            sb.append("    lw      $t2 12($a0)     # int slot\n");

            if (opText.equals("<")) {
                sb.append("    blt     $t1 $t2 ").append(trueLabel).append("\n");
            } else {
                sb.append("    ble     $t1 $t2 ").append(trueLabel).append("\n");
            }

            sb.append("    la      $a0 bool_const0\n");
            sb.append("    b       ").append(endLabel).append("\n");
            sb.append(trueLabel).append(":\n");
            sb.append("    la      $a0 bool_const1\n");
            sb.append(endLabel).append(":\n");

        } else if (opText.equals("=")) {
            
            String equalLabel = newLabel();
            String endLabel = newLabel();

            sb.append(generateExpr(op.left));
            sb.append("    sw      $a0 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");

            sb.append(generateExpr(op.right));

            sb.append("    lw      $t1 4($sp)\n");
            sb.append("    addiu   $sp $sp 4\n");
            sb.append("    move    $t2 $a0\n");
            sb.append("    beq     $t1 $t2 ").append(equalLabel).append("\n");

            
            sb.append("    la      $a0 bool_const1\n");
            sb.append("    la      $a1 bool_const0\n");
            sb.append("    jal     equality_test\n");
            sb.append("    b       ").append(endLabel).append("\n");

            sb.append(equalLabel).append(":\n");
            sb.append("    la      $a0 bool_const1\n");
            sb.append(endLabel).append(":\n");
        }

        return sb.toString();
    }

    private String generateUnaryOp(UnaryOp op) {
        StringBuilder sb = new StringBuilder();
        String opText = op.operator.getText().toLowerCase();

        sb.append(generateExpr(op.expr));

        if (opText.equals("not")) {
            String doneLabel = newLabel();
            sb.append("    lw      $t1 12($a0)     # bool slot\n");
            sb.append("    la      $a0 bool_const1\n");
            sb.append("    beqz    $t1 ").append(doneLabel).append("\n");
            sb.append("    la      $a0 bool_const0\n");
            sb.append(doneLabel).append(":\n");

        } else if (opText.equals("~")) {
            sb.append("    jal     Object.copy\n");
            sb.append("    lw      $t1 12($a0)\n");
            sb.append("    neg     $t1 $t1\n");
            sb.append("    sw      $t1 12($a0)\n");

        } else if (opText.equals("isvoid")) {
            String trueLabel = newLabel();
            String endLabel = newLabel();
            sb.append("    beqz    $a0 ").append(trueLabel).append("\n");
            sb.append("    la      $a0 bool_const0\n");
            sb.append("    b       ").append(endLabel).append("\n");
            sb.append(trueLabel).append(":\n");
            sb.append("    la      $a0 bool_const1\n");
            sb.append(endLabel).append(":\n");
        }

        return sb.toString();
    }

    private String generateCall(Call call) {
        StringBuilder sb = new StringBuilder();

        
        for (int i = call.args.size() - 1; i >= 0; i--) {
            Expression arg = call.args.get(i);
            sb.append(generateExpr(arg));
            sb.append("    sw      $a0 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");
        }

        
        if (call.prefix != null) {
            sb.append(generateExpr(call.prefix));
        } else {
            sb.append("    move    $a0 $s0\n");
        }

        String dispatchLabel = newDispatchLabel();

        
        sb.append("    bnez    $a0 ").append(dispatchLabel).append("\n");

        
        String fileName = getFileNameLabel(call);
        int lineNo = call.token.getLine();
        sb.append("    la      $a0 ").append(fileName).append("\n");
        sb.append("    li      $t1 ").append(lineNo).append("\n");
        sb.append("    jal     _dispatch_abort\n");

        sb.append(dispatchLabel).append(":\n");

        String methodName = call.name.getText();

        if (call.type != null) {
            
            String staticType = call.type.getText();
            Map<String, Integer> offsets = classMethodOffsets.get(staticType);
            int offset = offsets != null ? offsets.getOrDefault(methodName, 0) : 0;

            sb.append("    la      $t1 ").append(staticType).append("_dispTab   # dispatch table\n");
            sb.append("    lw      $t1 ").append(offset).append("($t1)   # method offset\n");
        } else {
            
            
            String receiverType = getReceiverType(call);
            sb.append("    # DEBUG: dispatch to ").append(methodName).append(" receiver type: ").append(receiverType)
                    .append("\n");
            Map<String, Integer> offsets = classMethodOffsets.get(receiverType);
            int offset = offsets != null ? offsets.getOrDefault(methodName, 0) : 0;

            sb.append("    lw      $t1 8($a0)   # dispatch table\n");
            sb.append("    lw      $t1 ").append(offset).append("($t1)   # method offset\n");
        }

        sb.append("    jalr    $t1\n");

        return sb.toString();
    }

    private String getReceiverType(Call call) {
        if (call.prefix == null) {
            return currentClassName;
        }

        String type = getExprType(call.prefix);

        if (type.equals("SELF_TYPE")) {
            return currentClassName;
        }

        return type;
    }

    private String getExprType(Expression expr) {
        if (expr instanceof StringT)
            return "String";
        if (expr instanceof Int)
            return "Int";
        if (expr instanceof Booll)
            return "Bool";

        if (expr instanceof New) {
            return ((New) expr).type.getText();
        }

        if (expr instanceof Id) {
            String name = ((Id) expr).token.getText();
            if (name.equals("self"))
                return "SELF_TYPE";

            
            if (localTypes.containsKey(name))
                return localTypes.get(name);

            
            String type = getAttributeType(currentClassName, name);
            if (type != null)
                return type;
        }

        if (expr instanceof Call) {
            String receiverType = getReceiverType((Call) expr);
            
            String lookupType = receiverType.equals("SELF_TYPE") ? currentClassName : receiverType;

            ClassSymbol scope = (ClassSymbol) SymbolTable.globals.lookup(lookupType);
            if (scope != null) {

                MethodSymbol method = (MethodSymbol) scope.lookupMethod(((Call) expr).name.getText());

                
                ClassSymbol currentScope = scope;
                while (method == null && currentScope.parent != null) {
                    currentScope = currentScope.parent;
                    method = (MethodSymbol) currentScope.lookupMethod(((Call) expr).name.getText());
                }

                if (method != null)
                    return method.getType();
            }
        }

        
        return "Object";
    }

    private String generateInitCall(InitCall call) {
        StringBuilder sb = new StringBuilder();

        
        for (int i = call.args.size() - 1; i >= 0; i--) {
            Expression arg = call.args.get(i);
            sb.append(generateExpr(arg));
            sb.append("    sw      $a0 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");
        }

        
        sb.append("    move    $a0 $s0\n");

        String dispatchLabel = newDispatchLabel();
        String fileName = getFileNameLabel(call);
        int lineNo = call.token.getLine();

        sb.append("    bnez    $a0 ").append(dispatchLabel).append("\n");
        sb.append("    la      $a0 ").append(fileName).append("\n");
        sb.append("    li      $t1 ").append(lineNo).append("\n");
        sb.append("    jal     _dispatch_abort\n");

        sb.append(dispatchLabel).append(":\n");

        String methodName = call.name.getText();
        Map<String, Integer> offsets = classMethodOffsets.get(currentClassName);
        int offset = offsets != null ? offsets.getOrDefault(methodName, 0) : 0;

        sb.append("    lw      $t1 8($a0)   # dispatch table\n");
        sb.append("    lw      $t1 ").append(offset).append("($t1)   # method offset\n");
        sb.append("    jalr    $t1\n");

        return sb.toString();
    }

    private String generateNew(New newExpr) {
        StringBuilder sb = new StringBuilder();
        String typeName = newExpr.type.getText();

        if (typeName.equals("SELF_TYPE")) {
            
            sb.append("    lw      $t1 0($s0)          # class tag\n");
            sb.append("    sll     $t1 $t1 3           # * 8\n");
            sb.append("    la      $t2 class_objTab\n");
            sb.append("    addu    $t1 $t1 $t2\n");
            sb.append("    sw      $t1 0($sp)\n");
            sb.append("    addiu   $sp $sp -4\n");
            sb.append("    lw      $a0 0($t1)          # protObj\n");
            sb.append("    jal     Object.copy\n");
            sb.append("    lw      $t1 4($sp)\n");
            sb.append("    addiu   $sp $sp 4\n");
            sb.append("    lw      $t1 4($t1)          # init\n");
            sb.append("    jalr    $t1\n");
        } else {
            
            sb.append("    la      $a0 ").append(typeName).append("_protObj\n");
            sb.append("    jal     Object.copy\n");
            sb.append("    jal     ").append(typeName).append("_init\n");
        }

        return sb.toString();
    }

    private String generateCase(Case caseExpr) {
        StringBuilder sb = new StringBuilder();

        String caseLabel = newLabel();
        String endLabel = "endcase" + labelCounter++;

        
        sb.append(generateExpr(caseExpr.expr));

        
        String fileName = getFileNameLabel(caseExpr);
        int lineNo = caseExpr.token.getLine();
        sb.append("    bnez    $a0 ").append(caseLabel).append("\n");
        sb.append("    la      $a0 ").append(fileName).append("\n");
        sb.append("    li      $t1 ").append(lineNo).append("\n");
        sb.append("    jal     _case_abort2\n");

        sb.append(caseLabel).append(":\n");

        
        int savedOffset = currentLocalOffset;
        currentLocalOffset -= 4;
        int caseVarOffset = currentLocalOffset;
        sb.append("    sw      $a0 ").append(caseVarOffset).append("($fp)\n");

        
        sb.append("    lw      $t1 0($a0)      # class tag\n");

        
        List<caseBranch> sortedBranches = new ArrayList<>(caseExpr.branchList);
        sortedBranches.sort((a, b) -> {
            int tagA = classTags.getOrDefault(a.type.getText(), 0);
            int tagB = classTags.getOrDefault(b.type.getText(), 0);
            
            int maxA = classMaxTag.getOrDefault(a.type.getText(), tagA);
            int maxB = classMaxTag.getOrDefault(b.type.getText(), tagB);
            
            int rangeA = maxA - classTags.getOrDefault(a.type.getText(), 0);
            int rangeB = maxB - classTags.getOrDefault(b.type.getText(), 0);
            return rangeA - rangeB;
        });

        
        String nextLabel = null;
        for (int i = 0; i < sortedBranches.size(); i++) {
            caseBranch branch = sortedBranches.get(i);
            String typeName = branch.type.getText();

            if (nextLabel != null) {
                sb.append(nextLabel).append(":\n");
            }
            nextLabel = "casebranch" + labelCounter++;

            int minTag = classTags.getOrDefault(typeName, 0);
            int maxTag = classMaxTag.getOrDefault(typeName, minTag);

            sb.append("    blt     $t1 ").append(minTag).append(" ").append(nextLabel).append("\n");
            sb.append("    bgt     $t1 ").append(maxTag).append(" ").append(nextLabel).append("\n");

            
            localOffsets.put(branch.name, caseVarOffset);
            localTypes.put(branch.name, typeName);
            sb.append("    lw      $a0 ").append(caseVarOffset).append("($fp)\n");

            
            sb.append(generateExpr(branch.expr));

            localOffsets.remove(branch.name);
            localTypes.remove(branch.name);

            sb.append("    b       ").append(endLabel).append("\n");
        }

        
        sb.append(nextLabel).append(":\n");
        sb.append("    lw      $a0 ").append(caseVarOffset).append("($fp)\n");
        sb.append("    jal     _case_abort\n");

        sb.append(endLabel).append(":\n");

        currentLocalOffset = savedOffset;

        return sb.toString();
    }

    private String getFileNameLabel(Expression expr) {
        if (expr.fileName != null) {
            String baseName = new java.io.File(expr.fileName).getName();
            return getStringConst(baseName);
        }
        
        for (Class cls : programAST.classList) {
            if (cls.className.equals(currentClassName) && cls.fileName != null) {
                String baseName = new java.io.File(cls.fileName).getName();
                return getStringConst(baseName);
            }
        }
        return getStringConst("unknown");
    }

    @Override
    public ST visit(Program program) {
        this.programAST = program;

        
        setupClassHierarchy();
        setupMethodTables();
        setupAttributeTables();
        collectConstants();

        
        String dataSection = generateDataSection();
        String textSection = generateTextSection();

        
        ST programST = templates.getInstanceOf("program");
        programST.add("dataSection", dataSection);
        programST.add("textSection", textSection);

        return programST;
    }

    @Override
    public ST visit(Class classNode) {
        return null;
    }

    @Override
    public ST visit(FuncDef funcDef) {
        return null;
    }

    @Override
    public ST visit(VarDef varDef) {
        return null;
    }

    @Override
    public ST visit(Assign assign) {
        return null;
    }

    @Override
    public ST visit(Call call) {
        return null;
    }

    @Override
    public ST visit(BinaryOp binaryOp) {
        return null;
    }

    @Override
    public ST visit(If iff) {
        return null;
    }

    @Override
    public ST visit(Let let) {
        return null;
    }

    @Override
    public Void visit(Feature feature) {
        return null;
    }

    @Override
    public ST visit(FormalPrm formalPrm) {
        return null;
    }

    @Override
    public ST visit(LocalPrm localPrm) {
        return null;
    }

    @Override
    public ST visit(caseBranch casebranch) {
        return null;
    }

    @Override
    public ST visit(New neww) {
        return null;
    }

    @Override
    public ST visit(Id id) {
        return null;
    }

    @Override
    public ST visit(Int intt) {
        return null;
    }

    @Override
    public ST visit(StringT stringt) {
        return null;
    }

    @Override
    public ST visit(Booll bool) {
        return null;
    }

    @Override
    public ST visit(Case casee) {
        return null;
    }

    @Override
    public ST visit(While whilee) {
        return null;
    }

    @Override
    public ST visit(UnaryOp unaryOp) {
        return null;
    }

    @Override
    public ST visit(InitCall initCall) {
        return null;
    }

    @Override
    public ST visit(Block block) {
        return null;
    }

    
    public List<Class> getClasses(Program program) {
        return program.classList;
    }

    public List<Feature> getFeatures(Class cls) {
        return cls.featureList;
    }
}