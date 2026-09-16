package org.challenge.vulnseverityevaluator;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21;
import static java.lang.Boolean.FALSE;
import static java.lang.Math.max;
import static java.lang.String.format;
import static java.nio.file.Files.walk;
import static java.util.Map.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.slf4j.LoggerFactory.getLogger;

class MethodComplexityTest {

    /**
     * The language level must be set explicitly. With the default level JavaParser rejects record declarations, and
     * a file it cannot parse contributes no measurements — so the metrics below would silently pass by measuring
     * nothing. That is why {@link #givenMethodsComplexityWhenCheckThenDoNotThrowException()} also fails on any file
     * it could not parse.
     */
    private static final JavaParser parser = new JavaParser(
            new ParserConfiguration().setLanguageLevel(JAVA_21));
    private static final Logger logger = getLogger(MethodComplexityTest.class);
    private static final String COMMENT = "//.*|/\\*(.|[\\n\\r])*?\\*/";
    private static final String COUNT = "count";
    private static final String EMPTY = "";
    private static final String FILE_EXTENSION = ".java";
    private static final String LIMIT = "limit";
    private static final String METRIC_CYCLOMATIC_COMPLEXITY = "cyclomatic_complexity";
    private static final String METRIC_LOGICAL_LINES_OF_CODE = "logical_lines_of_code";
    private static final String METRIC_NESTING_DEPTH = "nesting_depth";
    private static final String METRIC_NUMBER_OF_PARAMETERS = "number_of_parameters";
    private static final String METRIC_PHYSICAL_LINES_OF_CODE = "physical_lines_of_code";
    private static final String PATH = "src/main";
    private static final String REASON = "The number of methods that exceed at least one threshold does not satisfy the requirements";
    private static final String UNPARSED_REASON = "Every source file must be parseable, otherwise it is not measured";
    private static final String VISITOR = "visitor";
    private static final Void UNUSED = null;

    private static final Map<String, Map<String, Object>> metrics = new HashMap<>(of(
            METRIC_CYCLOMATIC_COMPLEXITY, new HashMap<>(of(
                    VISITOR, new MetricExceededVisitor(new CyclomaticComplexity(), 5, METRIC_CYCLOMATIC_COMPLEXITY),
                    COUNT, 0,
                    LIMIT, 0
            )),
            METRIC_LOGICAL_LINES_OF_CODE, new HashMap<>(of(
                    VISITOR, new MetricExceededVisitor(new LogicalLinesOfCode(), 32, METRIC_LOGICAL_LINES_OF_CODE),
                    COUNT, 0,
                    LIMIT, 0
            )),
            METRIC_NESTING_DEPTH, new HashMap<>(of(
                    VISITOR, new MetricExceededVisitor(new NestingDepth(), 3, METRIC_NESTING_DEPTH),
                    COUNT, 0,
                    LIMIT, 0
            )),
            METRIC_NUMBER_OF_PARAMETERS, new HashMap<>(of(
                    VISITOR, new MetricExceededVisitor(new NumberOfParameters(), 5, METRIC_NUMBER_OF_PARAMETERS),
                    COUNT, 0,
                    LIMIT, 0
            )),
            METRIC_PHYSICAL_LINES_OF_CODE, new HashMap<>(of(
                    VISITOR, new MetricExceededVisitor(new PhysicalLinesOfCode(), 64, METRIC_PHYSICAL_LINES_OF_CODE),
                    COUNT, 0,
                    LIMIT, 0
            ))
    ));

    static class MetricExceededVisitor extends VoidVisitorAdapter<Void> {

        private Integer exceededCount;
        private final Function<MethodDeclaration, Integer> metric;
        private final Integer threshold;
        private final String metricName;
        private static final String EXCEEDED_MESSAGE = "{} of {} in class {} is {}, exceeded";

        public MetricExceededVisitor(Function<MethodDeclaration, Integer> metric, Integer threshold, String metricName) {
            this.exceededCount = 0;
            this.metric = metric;
            this.threshold = threshold;
            this.metricName = metricName;
        }

        public Boolean isExceeded() {
            return exceededCount > 0;
        }

        public void reset() {
            this.exceededCount = 0;
        }

        public String name(MethodDeclaration method) {
            return method.getDeclarationAsString(FALSE, FALSE, FALSE);
        }

        public String type(MethodDeclaration method) {
            return method.findAncestor(ClassOrInterfaceDeclaration.class).map(NodeWithSimpleName::getNameAsString).orElse(EMPTY);
        }

        @Override
        public void visit(MethodDeclaration method, Void unused) {
            super.visit(method, unused);
            Integer value = metric.apply(method);
            if (value > threshold) {
                exceededCount++;
                logger.warn(EXCEEDED_MESSAGE, metricName, name(method), type(method), value);
            }
        }

    }

    static class CyclomaticComplexity implements Function<MethodDeclaration, Integer> {

        private static final List<String> KEYWORDS = List.of(
                " && ", " case ", " catch ", " default: ", " do ", " else ", " for ", " if ", " while ", " || "
        );
        private static final Integer INHERENT_COMPLEXITY = 1;

        private Integer count(String sourceCode, String keyword) {
            int count = 0;
            int index = 0;
            while ((index = sourceCode.indexOf(keyword, index)) != -1) {
                count++;
                index += keyword.length();
            }
            return count;
        }

        private String code(MethodDeclaration method) {
            return method.toString().replaceAll(COMMENT, EMPTY);
        }

        @Override
        public Integer apply(MethodDeclaration method) {
            return INHERENT_COMPLEXITY + KEYWORDS.stream().mapToInt(keyword -> count(code(method), keyword)).sum();
        }

    }

    static class NestingDepth implements Function<MethodDeclaration, Integer> {

        private Integer depth(Node node, Integer current) {
            Integer max = current;
            if (node instanceof BlockStmt) {
                current++;
            }
            for (Node child : node.getChildNodes()) {
                Integer childDepth = depth(child, current);
                max = max(max, childDepth);
            }
            return max;
        }

        @Override
        public Integer apply(MethodDeclaration method) {
            Integer nestingDepth = 0;
            Optional<BlockStmt> blockStmt = method.getBody();
            if (blockStmt.isPresent()) {
                nestingDepth = depth(blockStmt.get(), -1);
            }
            return nestingDepth;
        }
    }

    static class NumberOfParameters implements Function<MethodDeclaration, Integer> {

        @Override
        public Integer apply(MethodDeclaration method) {
            return method.getParameters().size();
        }

    }

    static class LogicalLinesOfCode implements Function<MethodDeclaration, Integer> {

        @Override
        public Integer apply(MethodDeclaration method) {
            return method.getBody().map(body -> body.getStatements().size()).orElse(0);
        }

    }

    static class PhysicalLinesOfCode implements Function<MethodDeclaration, Integer> {

        private static Integer commentLines(MethodDeclaration method) {
            return method.getAllContainedComments().stream().filter(comment -> comment.getRange().isPresent()).mapToInt(comment -> comment.getRange().get().end.line - comment.getRange().get().begin.line + 1).sum();
        }

        private static Integer linesOfCode(MethodDeclaration method) {
            return method.getRange().map(range -> range.end.line - range.begin.line + 1).orElse(0);
        }

        @Override
        public Integer apply(MethodDeclaration method) {
            return linesOfCode(method) - commentLines(method);
        }
    }

    private List<File> javaFiles() throws IOException {
        try (Stream<Path> walk = walk(Path.of(PATH))) {
            return walk.filter(file -> file.toString().endsWith(FILE_EXTENSION)).map(Path::toFile).collect(toList());
        }
    }

    private static Boolean atLeastOneLimitExceeded() {
        return metrics.values().stream().anyMatch(map -> (Integer) map.get(COUNT) > (Integer) map.get(LIMIT));
    }

    @Test
    void givenMethodsComplexityWhenCheckThenDoNotThrowException() throws IOException {
        List<String> unparsed = new ArrayList<>();
        for (File file : javaFiles()) {
            ParseResult<CompilationUnit> parsed = parser.parse(file);
            collect(parsed, unparsed, file);
            parsed.getResult().ifPresent(unit -> metrics.values().forEach(metric -> {
                MetricExceededVisitor visitor = (MetricExceededVisitor) metric.get(VISITOR);
                visitor.visit(unit, UNUSED);
                if (visitor.isExceeded()) {
                    metric.put(COUNT, (Integer) metric.get(COUNT) + 1);
                }
                visitor.reset();
            }));
        }
        report(metrics);
        assertThat(UNPARSED_REASON, unparsed, is(empty()));
        assertThat(REASON, atLeastOneLimitExceeded(), is(FALSE));
    }

    private static void collect(ParseResult<CompilationUnit> parsed, List<String> unparsed, File file) {
        if (!parsed.getProblems().isEmpty()) {
            unparsed.add(file + " -> " + parsed.getProblems());
        }
    }

    public void report(Map<String, Map<String, Object>> metrics) {
        StringBuilder builder = new StringBuilder("\n");
        metrics.forEach((metric, configuration) -> {
            builder.append(format("%s\n", metric));
            configuration.forEach((k, v) -> {
                if (!VISITOR.equals(k)) {
                    builder.append(format("\t%s: %s\n", k, v));
                }
            });
        });
        logger.info(builder.toString());
    }

}
