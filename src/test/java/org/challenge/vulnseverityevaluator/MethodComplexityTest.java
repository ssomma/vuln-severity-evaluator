package org.challenge.vulnseverityevaluator;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.file.Files.walk;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.slf4j.LoggerFactory.getLogger;

public class MethodComplexityTest {
    private static final Logger logger = getLogger(MethodComplexityTest.class);
    private final JavaParser parser = new JavaParser();
    public static final String FILE_EXTENSION = ".java";
    public static final String ERROR_MESSAGE
            = "The number of methods with cyclomatic complexity above the threshold does not meet the requirement";
    public static final String PATH = "src/main";
    public static final Void UNUSED = null;
    public static final int METHOD_COMPLEXITY_THRESHOLD = 10;
    public static final int METHOD_COUNT_THRESHOLD = 5;

    @Test
    public void givenMethodsComplexityWhenCheckThenDoNotThrowException() throws IOException {
        List<File> javaFiles = javaFiles();
        int methodsExceedingThreshold = 0;
        for (File file : javaFiles) {
            ParseResult<CompilationUnit> parsed = parser.parse(file);
            MethodComplexityVisitor visitor = new MethodComplexityVisitor(METHOD_COMPLEXITY_THRESHOLD);
            if (parsed.getResult().isPresent()) {
                visitor.visit(parsed.getResult().get(), UNUSED);
                methodsExceedingThreshold += visitor.getMethodsExceedingThresholdCount();
            }
        }
        assertThat(ERROR_MESSAGE, methodsExceedingThreshold, is(lessThanOrEqualTo(METHOD_COUNT_THRESHOLD)));
    }

    private List<File> javaFiles() throws IOException {
        List<File> files;
        try (Stream<Path> walk = walk(Path.of(PATH))) {
            files = walk.filter(file -> file.toString().endsWith(FILE_EXTENSION)).map(Path::toFile).collect(toList());
        }
        return files;
    }

    public static class MethodComplexityVisitor extends VoidVisitorAdapter<Void> {
        private static final List<String> KEYWORDS = List.of(
                " && ", "case ", "catch ", "default: ", "do ", "else ", "for ", "if ", "while ", " || "
        );
        private static final String COMMENT = "//.*|/\\*(.|[\\n\\r])*?\\*/";
        private static final String EMPTY = "";
        private static final int METHOD_INHERENT_COMPLEXITY = 1;
        public static final String LOGGER_MESSAGE = "Cyclomatic complexity of [%s] in class %s is %s";
        private final int threshold;
        private int methodsExceedingThresholdCount;

        public MethodComplexityVisitor(int threshold) {
            this.threshold = threshold;
            this.methodsExceedingThresholdCount = 0;
        }

        public int getMethodsExceedingThresholdCount() {
            return methodsExceedingThresholdCount;
        }

        @Override
        public void visit(MethodDeclaration method, Void unused) {
            super.visit(method, unused);
            boolean exceeded = cyclomaticComplexity(method.toString()) > threshold;
            String message = format(LOGGER_MESSAGE, methodName(method), className(method), exceeded ? "EXCEEDED" : "OK");
            if (exceeded) {
                methodsExceedingThresholdCount++;
                logger.warn(message);
            } else {
                logger.info(message);
            }
        }

        private static String methodName(MethodDeclaration declaration) {
            return declaration.getDeclarationAsString(
                    false, false, false
            );
        }

        private String className(MethodDeclaration declaration) {
            return declaration
                    .findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(NodeWithSimpleName::getNameAsString)
                    .orElse(EMPTY);
        }

        private static int cyclomaticComplexity(String sourceCode) {
            return METHOD_INHERENT_COMPLEXITY
                    + KEYWORDS
                    .stream()
                    .mapToInt(keyword -> count(sourceCode.replaceAll(COMMENT, EMPTY), keyword))
                    .sum();
        }

        private static int count(String sourceCode, String keyword) {
            int count = 0;
            int index = 0;
            while ((index = sourceCode.indexOf(keyword, index)) != -1) {
                count++;
                index += keyword.length();
            }
            return count;
        }

    }

}

