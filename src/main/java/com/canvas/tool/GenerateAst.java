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

        System.out.println("Finished");
    }

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

        for (String type : types) {
            var splitString = type.split(":");
            String className = splitString[0].trim();
            String fields = splitString[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println("}");

        writer.close();
    }

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

        for (String field : fields ) {
            String name = field.split(" ")[1];
            writer.printf("            this.%s = %s;\n", name, name);
        }

        writer.printf("     }\n");
        // Constructor - end

        writer.printf("    }\n");   // Class end
    }
}
