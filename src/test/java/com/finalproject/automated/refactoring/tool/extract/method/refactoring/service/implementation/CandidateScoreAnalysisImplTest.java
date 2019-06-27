package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateScoreAnalysis;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 27 June 2019
 */

@SpringBootTest
@RunWith(SpringRunner.class)
public class CandidateScoreAnalysisImplTest {

    @Autowired
    private CandidateScoreAnalysis candidateScoreAnalysis;

    private static final String KEYWORD = "public";
    private static final String RETURN_TYPE = "Rectangle2D";
    private static final String METHOD_NAME = "getFigureDrawBounds";
    private static final String FIRST_GLOBAL_VARIABLE = "START";
    private static final String SECOND_GLOBAL_VARIABLE = "END";

    private static final Double EXPECTED_LENGTH_SCORE = 0.30000000000000004D;
    private static final Double EXPECTED_NESTING_DEPTH_SCORE = 1.0D;
    private static final Double EXPECTED_NESTING_AREA_SCORE = 0.0D;
    private static final Double EXPECTED_PARAMETER_SCORE = 3.0D;
    private static final Double EXPECTED_TOTAL_SCORE = 4.3D;

    private MethodModel methodModel;

    private Candidate candidate;

    @Before
    public void setUp() {
        methodModel = MethodModel.builder()
                .keywords(Collections.singletonList(KEYWORD))
                .returnType(RETURN_TYPE)
                .name(METHOD_NAME)
                .globalVariables(createGlobalVariables())
                .localVariables(createLocalVariables())
                .body(createBody())
                .statements(createExpectedStatements())
                .build();
        candidate = createCandidate();
    }

    @Test
    public void analysis_success() {
        candidateScoreAnalysis.analysis(methodModel, candidate);

        assertEquals(EXPECTED_LENGTH_SCORE, candidate.getLengthScore());
        assertEquals(EXPECTED_NESTING_DEPTH_SCORE, candidate.getNestingDepthScore());
        assertEquals(EXPECTED_NESTING_AREA_SCORE, candidate.getNestingAreaScore());
        assertEquals(EXPECTED_PARAMETER_SCORE, candidate.getParameterScore());
        assertEquals(EXPECTED_TOTAL_SCORE, candidate.getTotalScore());
        assertEquals(createExpectedParameters(), candidate.getParameters());
    }

    @Test(expected = NullPointerException.class)
    public void analysis_failed_methodModelIsNull() {
        candidateScoreAnalysis.analysis(null, candidate);
    }

    @Test(expected = NullPointerException.class)
    public void analysis_failed_candidateIsNull() {
        candidateScoreAnalysis.analysis(methodModel, null);
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
        return new ArrayList<>(Arrays.asList(FIRST_GLOBAL_VARIABLE, SECOND_GLOBAL_VARIABLE));
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

    private Candidate createCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createSecondBlockFirstStatement(),
                createSecondBlockSecondStatement()
        ));

        Candidate candidate = createCandidate(statements);
        addCandidateVariables(candidate);

        return candidate;
    }

    private Candidate createCandidate(List<StatementModel> statements) {
        return Candidate.builder()
                .statements(statements)
                .build();
    }

    private void addCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.getGlobalVariables()
                .add("r");
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables()
        )));
    }

    private List<String> createThirdStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "START", "!="
        ));
    }

    private List<String> createFourthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "Point", "p1", "="
        ));
    }

    private List<String> createFifthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "Point", "p2", "="
        ));
    }

    private List<String> createSixthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "r", "START", "p1", "p2"
        ));
    }

    private List<String> createSeventhStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "END", "!="
        ));
    }

    private List<String> createEighthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "Point", "p1", "=", "-"
        ));
    }

    private List<String> createNinthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "Point", "p2", "=", "-"
        ));
    }

    private List<String> createTenthStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "r", "END", "p1", "p2"
        ));
    }

    private List<VariablePropertyModel> createExpectedParameters() {
        return Collections.singletonList(
                createFirstLocalVariable()
        );
    }
}