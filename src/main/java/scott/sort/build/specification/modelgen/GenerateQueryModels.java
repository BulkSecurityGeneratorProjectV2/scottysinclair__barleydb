package scott.sort.build.specification.modelgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import scott.sort.api.specification.DefinitionsSpec;
import scott.sort.api.specification.EntitySpec;
import scott.sort.api.specification.JoinTypeSpec;
import scott.sort.api.specification.NodeSpec;
import scott.sort.api.specification.RelationSpec;

public class GenerateQueryModels extends GenerateModelsHelper {

    public void generateQueryModels(String path, DefinitionsSpec definitions) throws IOException {
        Set<EntitySpec> parentSpecs = new HashSet<>();
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            if (entitySpec.getParentEntity() != null) {
                parentSpecs.add( entitySpec.getParentEntity() );
            }
        }
        for (EntitySpec parentSpec: parentSpecs) {
            generateAsbtractQueryModel(path, definitions, parentSpec);
        }
        for (EntitySpec entitySpec: definitions.getEntitySpecs()) {
            generateQueryModel(path, definitions, entitySpec);
        }
    }

    private void generateAsbtractQueryModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toAbstractQueryFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getQueryPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            out.write("import scott.sort.api.query.QProperty;\n");
            out.write("import scott.sort.api.query.QueryObject;\n");
            writeModelImports(definitions, entitySpec, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeAbstractQueryClassDeclaration(out, entitySpec);
            out.write("{\n");
            out.write("  private static final long serialVersionUID = 1L;\n");

            writeConstructors(out, "QAbstract" + getModelSimpleClassName(entitySpec), entitySpec);
            out.write("\n");
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (!isCompletelySuppressed(nodeSpec)) {
                    writeNodeMethods(out, nodeSpec);
                }
            }
            out.write("}");
         }
    }

    private void generateQueryModel(String path, DefinitionsSpec definitions, EntitySpec entitySpec) throws IOException {
        System.out.println("Generating model class " + entitySpec.getClassName());
        File classFile = toFile(path, entitySpec);
        classFile.getParentFile().mkdirs();
        try (Writer out = new FileWriter(classFile); ) {
            out.write("package ");
            out.write(getQueryPackageName(entitySpec));
            out.write(";\n");
            out.write("\n");
            out.write("import scott.sort.api.query.QProperty;\n");
            out.write("import scott.sort.api.query.QueryObject;\n");
            writeModelImports(definitions, entitySpec, out);
            out.write("\n");
            writeClassJavaDoc(out, entitySpec);
            writeClassDeclaration(out, entitySpec);
            out.write("{\n");
            out.write("  private static final long serialVersionUID = 1L;\n");

            writeConstructors(out, getQuerySimpleClassName(entitySpec), entitySpec);
            out.write("\n");
            for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
                if (!isCompletelySuppressed(nodeSpec)) {
                    writeNodeMethods(out, nodeSpec);
                }
            }
            out.write("}");
         }
    }

    private void writeModelImports(DefinitionsSpec definitions, EntitySpec entitySpec, Writer out) throws IOException {
        out.write("import ");
        out.write(entitySpec.getClassName());
        out.write(";\n");

        for (NodeSpec nodeSpec: entitySpec.getNodeSpecs()) {
            if (nodeSpec.getRelationSpec() != null) {
                RelationSpec relationSpec = nodeSpec.getRelationSpec();
                out.write("import ");
                out.write(relationSpec.getEntitySpec().getQueryClassName());
                out.write(";\n");
            }
            if (nodeSpec.getEnumType() != null) {
                out.write("import ");
                out.write(nodeSpec.getEnumType().getName());
                out.write(";\n");
            }
        }
        if (entitySpec.getParentEntity() != null) {
            out.write("import ");
            out.write(entitySpec.getParentEntity().getClassName());
            out.write(";\n");
        }
    }

    private void writeClassJavaDoc(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("/**\n");
        out.write(" * Generated from Entity Specification on ");
        out.write(new Date().toString());
        out.write("\n");
        out.write(" *\n");
        out.write(" * @author ");
        out.write(System.getProperty("user.name"));
        out.write("\n");
        out.write(" */\n");
    }


    private void writeAbstractQueryClassDeclaration(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("public class QAbstract");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(" ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends QAbstract");
            out.write(getModelSimpleClassName(entitySpec.getParentEntity()));
            out.write("<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write(", ");
            out.write(getQuerySimpleClassName(entitySpec));
            out.write("> ");
        }
        else {
            out.write("extends QueryObject<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write("> ");
        }
    }

    private void writeClassDeclaration(Writer out, EntitySpec entitySpec) throws IOException {
        out.write("public class ");
        out.write(getQuerySimpleClassName(entitySpec));
        out.write(" ");
        if (entitySpec.getParentEntity() != null) {
            out.write("extends QAbstract");
            out.write(getModelSimpleClassName(entitySpec.getParentEntity()));
            out.write("<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write(", ");
            out.write(getQuerySimpleClassName(entitySpec));
            out.write("> ");
        }
        else {
            out.write("extends QueryObject<");
            out.write(getModelSimpleClassName(entitySpec));
            out.write("> ");
        }
    }

    private void writeConstructors(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        writeDefaultConstructor(out, simpleClassName, entitySpec);
        out.write("\n");
        writeSubQueryConstructor(out, simpleClassName, entitySpec);
    }
    private void writeDefaultConstructor(Writer out, String simpleClassName, EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(simpleClassName);
        out.write("() {\n");
        out.write("    super(");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(".class);\n");
        out.write("  }\n");
    }

    private void writeSubQueryConstructor(Writer out, String simpleClassName,EntitySpec entitySpec) throws IOException {
        out.write("  public ");
        out.write(simpleClassName);
        out.write("(QueryObject<?> parent) {\n");
        out.write("    super(");
        out.write(getModelSimpleClassName(entitySpec));
        out.write(".class, parent);\n");
        out.write("  }\n");
    }

    private void writeNodeMethods(Writer out, NodeSpec nodeSpec) throws IOException {
        if (nodeSpec.getRelationSpec() == null) {
            /*
             * this is a simple node so we write the QProperty
             */
            writeQPropertyMethod(out, nodeSpec);
        }
        else {
            writeJoinMethod(out, nodeSpec);
            writeExistsMethod(out, nodeSpec);
        }
    }

    private void writeJoinMethod(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        out.write("\n  public ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(getJoinToMethodName(nodeSpec));
        out.write("() {\n");
        out.write("    ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(nodeSpec.getName());
        out.write(" = new ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write("();\n");
        out.write("    ");
        writeAddJoinMethodName(out, nodeSpec);
        out.write("(");
        out.write(nodeSpec.getName());
        out.write(", \"");
        out.write(nodeSpec.getName());
        out.write("\");\n");
        out.write("    return ");
        out.write(nodeSpec.getName());
        out.write(";\n");
        out.write("  }\n");
    }

    private void writeExistsMethod(Writer out, NodeSpec nodeSpec) throws IOException {
        RelationSpec relationSpec = nodeSpec.getRelationSpec();
        out.write("\n  public ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(getExistsMethodName(nodeSpec));
        out.write("() {\n");
        out.write("    ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write(" ");
        out.write(nodeSpec.getName());
        out.write(" = new ");
        out.write(getQuerySimpleClassName(relationSpec.getEntitySpec()));
        out.write("();\n");
        out.write("    addExists(");
        out.write(nodeSpec.getName());
        out.write(", \"");
        out.write(nodeSpec.getName());
        out.write("\");\n");
        out.write("    return ");
        out.write(nodeSpec.getName());
        out.write(";\n");
        out.write("  }\n");
    }

    private void writeQPropertyMethod(Writer out, NodeSpec nodeSpec) throws IOException {
        out.write("\n  public QProperty<");
        writeJavaType(out, nodeSpec);
        out.write("> ");
        out.write(nodeSpec.getName());
        out.write("() {\n");
        out.write("    return new QProperty<");
        writeJavaType(out, nodeSpec);
        out.write(">(this, \"");
        out.write(nodeSpec.getName());
        out.write("\");\n");
        out.write("  }\n");
    }

    private void writeAddJoinMethodName(Writer out, NodeSpec nodeSpec) throws IOException {
        JoinTypeSpec joinTypeSpec = nodeSpec.getRelationSpec().getJoinType();
        switch (joinTypeSpec) {
            case INNER_JOIN:
                out.write("addInnerJoin");
                break;
            case LEFT_OUTER_JOIN:
                out.write("addLeftOuterJoin");
                break;
            default:
                throw new IllegalStateException("Missing case for join type spec " + joinTypeSpec);
        }
    }

    private String getJoinToMethodName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "joinTo" + fc + name.substring(1, name.length());
    }

    private String getExistsMethodName(NodeSpec nodeSpec) {
        String name = nodeSpec.getName();
        char fc = Character.toUpperCase( name.charAt(0) );
        return "exists" + fc + name.substring(1, name.length());
    }

    private File toAbstractQueryFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        File file = new File(path + getJavaPath(entitySpec.getQueryClassName()));
        File dir = file.getParentFile();
        return new File(dir, "QAbstract" + getModelSimpleClassName(entitySpec) + ".java");
    }

    private File toFile(String path, EntitySpec entitySpec) {
        if (!path.endsWith("/")) {
            path += "/";
        }
        return new File(path + getJavaPath(entitySpec.getQueryClassName()) + ".java");
    }


}