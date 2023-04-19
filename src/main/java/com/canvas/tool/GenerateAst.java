package com.canvas.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generate_ast <output_directory>");
            System.exit(64);
        }

        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary     : Expr left, Token operator, Expr right",
                "Grouping   : Expr expression",
                "Literal    : Object value",
                "Unary      : Token operator, Expr right"
        ));

        System.out.printf("Finished Generating to %s\n", outputDir);
    }

    /**
     * A function that generates AST classes for the lox interpreter and writes to a file
     *
     * @param outputDir The directory to write a file in
     * @param baseName  The name of the abstract class that encapsulates the other generates classes,
     *                  it is also used as the name for the generates file
     * @param types     The types to generate as in the following format: <br/>
     *                  <code>ClassName   : Type1 param1, Type2 param2, ...etc</code>
     * @throws IOException If the output directory (outputDir argument) does not exist,
     *                     the function will fail to create a file
     */
    private static void defineAst(
            String outputDir,
            String baseName,
            List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java"; // PS: will this work on windows?
        var writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.canvas.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // The AST classes
        for (String type : types) {
            var splitString = type.split(":");
            String className = splitString[0].trim();
            String fields = splitString[1].trim();
            defineType(writer, baseName, className, fields);
        }

        // The base 'accept' method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");

        writer.close();
    }

    /**
     * Helper function for defineAst
     *
     * @param writer    the file writer
     * @param baseName the name of the encapsulating class
     * @param types The types to generate visitor functions for
     */
    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.printf("     R visit%s%s(%s %s);\n", typeName, baseName, typeName, baseName.toLowerCase());
        }

        writer.println("    }");
    }

    /**
     * Helper function for defineAst
     *
     * @param writer    the file writer
     * @param baseClass the name of the encapsulating class
     * @param className the name of the encapsulated class
     * @param fieldList the list of fields as in a java function declaration
     */
    private static void defineType(
            PrintWriter writer,
            String baseClass,
            String className,
            String fieldList) {
        String[] fields = fieldList.split(", ");

        writer.printf("    static class %s extends %s {\n", className, baseClass);

        // Fields
        writer.printf("\n");
        for (String field : fields) {
            writer.printf("     final %s;", field);
        }

        // Constructor
        writer.printf("         %s(%s){\n", className, fieldList);

        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.printf("            this.%s = %s;\n", name, name);
        }

        writer.printf("     }\n");
        // Constructor - end

        // Visitor pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.printf("        return visitor.visit%s%s(this);\n", className, baseClass);
        writer.println("    }");

        writer.printf("    }\n");   // Class end
    }
}
