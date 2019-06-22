package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExtractMethodImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private ExtractMethod extractMethod;

    private static final String KEYWORD = "public";
    private static final String RETURN_TYPE = "Rectangle2D";
    private static final String METHOD_NAME = "getFigureDrawBounds";

    private MethodModel methodModel;

    private Path path;

    @Before
    public void setUp() throws IOException {
        File file = temporaryFolder.newFile();
        methodModel = MethodModel.builder()
                .keywords(Collections.singletonList(KEYWORD))
                .returnType(RETURN_TYPE)
                .name(METHOD_NAME)
                .globalVariables(createGlobalVariables())
                .localVariables(createLocalVariables())
                .body(createBody())
                .statements(createExpectedStatements())
                .build();
        path = Paths.get(file.getPath());

        Files.write(path, createFileContent().getBytes());
    }

    @Test
    public void refactoring_success() throws IOException {
        assertTrue(extractMethod.refactoring(path.toString(), methodModel));
        assertEquals(createExpectedFileContent(), new String(Files.readAllBytes(path)));
    }

    @Test
    public void refactoring_failed_pathNotFound() {
        assertFalse(extractMethod.refactoring("", methodModel));
    }

    @Test(expected = NullPointerException.class)
    public void refactoring_failed_pathIsNull() {
        extractMethod.refactoring(null, methodModel);
    }

    @Test(expected = NullPointerException.class)
    public void refactoring_failed_methodModelIsNull() {
        extractMethod.refactoring(path.toString(), null);
    }

    private String createBody() {
        return "Rectangle2D r = super.getFigDrawBounds();\n" +
                "\n" +
                "    if (getNodeCount() > 1) {\n" +
                "        if (START.get(this) != null) {\n" +
                "            Point p1 = getPoint(0, 0);\n" +
                "            Point p2 = getPoint(1, 0);\n" +
                "            r.add(START.get(this).getBounds(p1, p2));\n" +
                "        }\n" +
                "        if (END.get(this) != null) {\n" +
                "            Point p1 = getPoint(getNodeCount() - 1, 0);\n" +
                "            Point p2 = getPoint(getNodeCount() - 2, 0);\n" +
                "            r.add(END.get(this).getBounds(p1, p2));\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    return r;";
    }

    private String createFileContent() {
        return "package com.test;\n" +
                "\n" +
                "/**\n" +
                " * @author Faza Zulfika P P\n" +
                " * @version 1.0.0\n" +
                " * @since 20 June 2019\n" +
                " */\n" +
                "\n" +
                "public class Test {\n" +
                "\n" +
                "    public Rectangle2D getFigureDrawBounds() {\n" +
                "        Rectangle2D r = super.getFigDrawBounds();\n" +
                "\n" +
                "        if (getNodeCount() > 1) {\n" +
                "            if (START.get(this) != null) {\n" +
                "                Point p1 = getPoint(0, 0);\n" +
                "                Point p2 = getPoint(1, 0);\n" +
                "                r.add(START.get(this).getBounds(p1, p2));\n" +
                "            }\n" +
                "            if (END.get(this) != null) {\n" +
                "                Point p1 = getPoint(getNodeCount() - 1, 0);\n" +
                "                Point p2 = getPoint(getNodeCount() - 2, 0);\n" +
                "                r.add(END.get(this).getBounds(p1, p2));\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return r;\n" +
                "    }\n" +
                "}";
    }

    private List<StatementModel> createExpectedStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createFirstStatement());
        statements.add(createSecondStatement());
        statements.add(createThirdStatement());

        return statements;
    }

    private StatementModel createFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Rectangle2D r = super.getFigDrawBounds();")
                .index(0)
                .startIndex(8)
                .endIndex(48)
                .build();
    }

    private StatementModel createSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createSecondBlockStatements())
                .endOfBlockStatement(createSecondBlockEndStatement())
                .build();
        blockModel.setStatement("if (getNodeCount() > 1) {");
        blockModel.setIndex(1);
        blockModel.setStartIndex(61);
        blockModel.setEndIndex(85);

        return blockModel;
    }

    private List<StatementModel> createSecondBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createSecondBlockFirstStatement());
        statements.add(createSecondBlockSecondStatement());

        return statements;
    }

    private StatementModel createSecondBlockFirstStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createThirdBlockStatements())
                .endOfBlockStatement(createThirdBlockEndStatement())
                .build();
        blockModel.setStatement("if (START.get(this) != null) {");
        blockModel.setIndex(2);
        blockModel.setStartIndex(102);
        blockModel.setEndIndex(131);

        return blockModel;
    }

    private List<StatementModel> createThirdBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createThirdBlockFirstStatement());
        statements.add(createThirdBlockSecondStatement());
        statements.add(createThirdBlockThirdStatement());

        return statements;
    }

    private StatementModel createThirdBlockFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p1 = getPoint(0, 0);")
                .index(3)
                .startIndex(152)
                .endIndex(177)
                .build();
    }

    private StatementModel createThirdBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint(1, 0);")
                .index(4)
                .startIndex(198)
                .endIndex(223)
                .build();
    }

    private StatementModel createThirdBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add(START.get(this).getBounds(p1, p2));")
                .index(5)
                .startIndex(256)
                .endIndex(284)
                .build();
    }

    private StatementModel createThirdBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .index(6)
                .startIndex(301)
                .endIndex(301)
                .build();
    }

    private StatementModel createSecondBlockSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createFourthBlockStatements())
                .endOfBlockStatement(createFourthBlockEndStatement())
                .build();
        blockModel.setStatement("if (END.get(this) != null) {");
        blockModel.setIndex(6);
        blockModel.setStartIndex(319);
        blockModel.setEndIndex(346);

        return blockModel;
    }

    private List<StatementModel> createFourthBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createFourthBlockFirstStatement());
        statements.add(createFourthBlockSecondStatement());
        statements.add(createFourthBlockThirdStatement());

        return statements;
    }

    private StatementModel createFourthBlockFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p1 = getPoint(getNodeCount() - 1, 0);")
                .index(7)
                .startIndex(367)
                .endIndex(409)
                .build();
    }

    private StatementModel createFourthBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint(getNodeCount() - 2, 0);")
                .index(8)
                .startIndex(430)
                .endIndex(472)
                .build();
    }

    private StatementModel createFourthBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add(END.get(this).getBounds(p1, p2));")
                .index(9)
                .startIndex(493)
                .endIndex(531)
                .build();
    }

    private StatementModel createFourthBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .index(10)
                .startIndex(548)
                .endIndex(548)
                .build();
    }

    private StatementModel createSecondBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .index(10)
                .startIndex(561)
                .endIndex(561)
                .build();
    }

    private StatementModel createThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("return r;")
                .index(10)
                .startIndex(575)
                .endIndex(583)
                .build();
    }

    private List<VariablePropertyModel> createLocalVariables() {
        return Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        );
    }

    private VariablePropertyModel createFirstLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(0)
                .build();

        variablePropertyModel.setType("Rectangle2D");
        variablePropertyModel.setName("r");

        return variablePropertyModel;
    }

    private VariablePropertyModel createSecondLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(3)
                .build();

        variablePropertyModel.setType("Point");
        variablePropertyModel.setName("p1");

        return variablePropertyModel;
    }

    private VariablePropertyModel createThirdLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(4)
                .build();

        variablePropertyModel.setType("Point");
        variablePropertyModel.setName("p2");

        return variablePropertyModel;
    }

    private VariablePropertyModel createFourthLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(7)
                .build();

        variablePropertyModel.setType("Point");
        variablePropertyModel.setName("p1");

        return variablePropertyModel;
    }

    private VariablePropertyModel createFifthLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(8)
                .build();

        variablePropertyModel.setType("Point");
        variablePropertyModel.setName("p2");

        return variablePropertyModel;
    }

    private List<String> createGlobalVariables() {
        return Arrays.asList("START", "END");
    }

    private String createExpectedFileContent() {
        return "package com.test;\n" +
                "\n" +
                "/**\n" +
                " * @author Faza Zulfika P P\n" +
                " * @version 1.0.0\n" +
                " * @since 20 June 2019\n" +
                " */\n" +
                "\n" +
                "public class Test {\n" +
                "\n" +
                "    public Rectangle2D getFigureDrawBounds() {\n" +
                "\t\tRectangle2D r = super.getFigDrawBounds();\n" +
                "\t\tif (getNodeCount() > 1) {\n" +
                "\t\t\tgetFigureDrawBoundsExtracted(r);\n" +
                "\t\t}\n" +
                "\t\treturn r;\n" +
                "\t}\n" +
                "\n" +
                "\tprivate void getFigureDrawBoundsExtracted(Rectangle2D r) {\n" +
                "\t\tif (START.get(this) != null) {\n" +
                "\t\t\tPoint p1 = getPoint(0, 0);\n" +
                "\t\t\tPoint p2 = getPoint(1, 0);\n" +
                "\t\t\tr.add(START.get(this).getBounds(p1, p2));\n" +
                "\t\t}\n" +
                "\t\tif (END.get(this) != null) {\n" +
                "\t\t\tPoint p1 = getPoint(getNodeCount() - 1, 0);\n" +
                "\t\t\tPoint p2 = getPoint(getNodeCount() - 2, 0);\n" +
                "\t\t\tr.add(END.get(this).getBounds(p1, p2));\n" +
                "\t\t}\n" +
                "\t}\n" +
                "}";
    }
}