package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateVariableAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper.CandidateHelper;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 26 June 2019
 */

@SpringBootTest
@RunWith(SpringRunner.class)
public class CandidateAnalysisImplTest {

    @Autowired
    private CandidateAnalysis candidateAnalysis;

    @MockBean
    private CandidateVariableAnalysis candidateVariableAnalysis;

    private static final String KEYWORD = "public";
    private static final String RETURN_TYPE = "Rectangle2D";
    private static final String METHOD_NAME = "getFigureDrawBounds";
    private static final String FIRST_GLOBAL_VARIABLE = "START";
    private static final String SECOND_GLOBAL_VARIABLE = "END";

    private static final Integer SECOND_INDEX = 1;

    private MethodModel methodModel;

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

        mockAnalysisVariable();
        mockRemainingAnalysisVariable();
    }

    @Test
    public void analysis_success() {
        List<Candidate> candidates = candidateAnalysis.analysis(methodModel);
        assertEquals(createExpectedCandidates(), candidates);
    }

    @Test(expected = NullPointerException.class)
    public void analysis_failed_methodIsNull() {
        candidateAnalysis.analysis(null);
        verifyNoMoreInteractions(candidateVariableAnalysis);
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

    private List<String> createFirstStatementRawVariables() {
        return new ArrayList<>(Arrays.asList(
                "Rectangle2D", "r", "="
        ));
    }

    private List<String> createSecondStatementRawVariables() {
        return new ArrayList<>(Collections.singletonList(
                ">"
        ));
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

    private List<String> createEleventhStatementRawVariables() {
        return new ArrayList<>(Collections.singletonList(
                "r"
        ));
    }

    private void mockAnalysisVariable() {
        mockFirstBlockAnalysisVariable();
        mockSecondBlockAnalysisVariable();
        mockThirdBlockAnalysisVariable();
        mockFourthBlockAnalysisVariable();
    }

    private void mockFirstBlockAnalysisVariable() {
        mockAnalysisFirstCandidateVariable();
        mockAnalysisSecondCandidateVariable();
        mockAnalysisThirdCandidateVariable();
        mockAnalysisFourthCandidateVariable();
        mockAnalysisFifthCandidateVariable();
        mockAnalysisSixthCandidateVariable();
    }

    private void mockSecondBlockAnalysisVariable() {
        mockAnalysisSeventhCandidateVariable();
        mockAnalysisEighthCandidateVariable();
        mockAnalysisNinthCandidateVariable();
    }

    private void mockThirdBlockAnalysisVariable() {
        mockAnalysisTenthCandidateVariable();
        mockAnalysisEleventhCandidateVariable();
        mockAnalysisTwelfthCandidateVariable();
        mockAnalysisThirteenthCandidateVariable();
        mockAnalysisFourteenthCandidateVariable();
        mockAnalysisFifteenthCandidateVariable();
    }

    private void mockFourthBlockAnalysisVariable() {
        mockAnalysisSixteenthCandidateVariable();
        mockAnalysisSeventeenthCandidateVariable();
        mockAnalysisEighteenthCandidateVariable();
        mockAnalysisNineteenthCandidateVariable();
        mockAnalysisTwentiethCandidateVariable();
        mockAnalysisTwentyFirstCandidateVariable();
    }

    private void mockAnalysisFirstCandidateVariable() {
        doAnswer(this::doMockAnalysisFirstCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFirstCandidate()));
    }

    private Candidate createFirstCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createFirstStatement()
        ));

        return createCandidate(statements);
    }

    private Candidate createCandidate(List<StatementModel> statements) {
        return Candidate.builder()
                .statements(statements)
                .build();
    }

    private Object doMockAnalysisFirstCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFirstCandidateVariables(candidate);

        return null;
    }

    private void addFirstCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFirstLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createFirstStatementRawVariables()
        )));
    }

    private void mockAnalysisSecondCandidateVariable() {
        doAnswer(this::doMockAnalysisSecondCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSecondCandidate()));
    }

    private Candidate createSecondCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFirstStatement(),
                createSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisSecondCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSecondCandidateVariables(candidate);

        return null;
    }

    private void addSecondCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
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

    private void mockAnalysisThirdCandidateVariable() {
        doAnswer(this::doMockAnalysisThirdCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createThirdCandidate()));
    }

    private Candidate createThirdCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFirstStatement(),
                createSecondStatement(),
                createThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisThirdCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addThirdCandidateVariables(candidate);

        return null;
    }

    private void addThirdCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisFourthCandidateVariable() {
        doAnswer(this::doMockAnalysisFourthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFourthCandidate()));
    }

    private Candidate createFourthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisFourthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFourthCandidateVariables(candidate);

        return null;
    }

    private void addFourthCandidateVariables(Candidate candidate) {
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
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisFifthCandidateVariable() {
        doAnswer(this::doMockAnalysisFifthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFifthCandidate()));
    }

    private Candidate createFifthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createSecondStatement(),
                createThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisFifthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFifthCandidateVariables(candidate);

        return null;
    }

    private void addFifthCandidateVariables(Candidate candidate) {
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
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisSixthCandidateVariable() {
        doAnswer(this::doMockAnalysisSixthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSixthCandidate()));
    }

    private Candidate createSixthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisSixthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSixthCandidateVariables(candidate);

        return null;
    }

    private void addSixthCandidateVariables(Candidate candidate) {
        candidate.setGlobalVariables(Collections.singletonList(
                "r"
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singleton(
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisSeventhCandidateVariable() {
        doAnswer(this::doMockAnalysisSeventhCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSeventhCandidate()));
    }

    private Candidate createSeventhCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createSecondBlockFirstStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisSeventhCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSeventhCandidateVariables(candidate);

        return null;
    }

    private void addSeventhCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                FIRST_GLOBAL_VARIABLE,
                "r"
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables()
        )));
    }

    private void mockAnalysisEighthCandidateVariable() {
        doAnswer(this::doMockAnalysisEighthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEighthCandidate()));
    }

    private Candidate createEighthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createSecondBlockFirstStatement(),
                createSecondBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisEighthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEighthCandidateVariables(candidate);

        return null;
    }

    private void addEighthCandidateVariables(Candidate candidate) {
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

    private void mockAnalysisNinthCandidateVariable() {
        doAnswer(this::doMockAnalysisNinthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createNinthCandidate()));
    }

    private Candidate createNinthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createSecondBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisNinthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addNinthCandidateVariables(candidate);

        return null;
    }

    private void addNinthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                SECOND_GLOBAL_VARIABLE,
                "r"
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables()
        )));
    }

    private void mockAnalysisTenthCandidateVariable() {
        doAnswer(this::doMockAnalysisTenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTenthCandidate()));
    }

    private Candidate createTenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createThirdBlockFirstStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisTenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTenthCandidateVariables(candidate);

        return null;
    }

    private void addTenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createSecondLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createFourthStatementRawVariables()
        )));
    }

    private void mockAnalysisEleventhCandidateVariable() {
        doAnswer(this::doMockAnalysisEleventhCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEleventhCandidate()));
    }

    private Candidate createEleventhCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createThirdBlockFirstStatement(),
                createThirdBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisEleventhCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEleventhCandidateVariables(candidate);

        return null;
    }

    private void addEleventhCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables()
        )));
    }

    private void mockAnalysisTwelfthCandidateVariable() {
        doAnswer(this::doMockAnalysisTwelfthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwelfthCandidate()));
    }

    private Candidate createTwelfthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createThirdBlockFirstStatement(),
                createThirdBlockSecondStatement(),
                createThirdBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisTwelfthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwelfthCandidateVariables(candidate);

        return null;
    }

    private void addTwelfthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                FIRST_GLOBAL_VARIABLE
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables()
        )));
    }

    private void mockAnalysisThirteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisThirteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createThirteenthCandidate()));
    }

    private Candidate createThirteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createThirdBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisThirteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addThirteenthCandidateVariables(candidate);

        return null;
    }

    private void addThirteenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createSecondLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createFifthStatementRawVariables()
        )));
    }

    private void mockAnalysisFourteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisFourteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFourteenthCandidate()));
    }

    private Candidate createFourteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createThirdBlockSecondStatement(),
                createThirdBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisFourteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFourteenthCandidateVariables(candidate);

        return null;
    }

    private void addFourteenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createSecondLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                FIRST_GLOBAL_VARIABLE,
                "p1"
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables()
        )));
    }

    private void mockAnalysisFifteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisFifteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFifteenthCandidate()));
    }

    private Candidate createFifteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createThirdBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisFifteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFifteenthCandidateVariables(candidate);

        return null;
    }

    private void addFifteenthCandidateVariables(Candidate candidate) {
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                FIRST_GLOBAL_VARIABLE,
                "p1",
                "p2"
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createSixthStatementRawVariables()
        )));
    }

    private void mockAnalysisSixteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisSixteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSixteenthCandidate()));
    }

    private Candidate createSixteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createFourthBlockFirstStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisSixteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSixteenthCandidateVariables(candidate);

        return null;
    }

    private void addSixteenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFourthLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createEighthStatementRawVariables()
        )));
    }

    private void mockAnalysisSeventeenthCandidateVariable() {
        doAnswer(this::doMockAnalysisSeventeenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSeventeenthCandidate()));
    }

    private Candidate createSeventeenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFourthBlockFirstStatement(),
                createFourthBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisSeventeenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSeventeenthCandidateVariables(candidate);

        return null;
    }

    private void addSeventeenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables()
        )));
    }

    private void mockAnalysisEighteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisEighteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEighteenthCandidate()));
    }

    private Candidate createEighteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFourthBlockFirstStatement(),
                createFourthBlockSecondStatement(),
                createFourthBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisEighteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEighteenthCandidateVariables(candidate);

        return null;
    }

    private void addEighteenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                SECOND_GLOBAL_VARIABLE
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables()
        )));
    }

    private void mockAnalysisNineteenthCandidateVariable() {
        doAnswer(this::doMockAnalysisNineteenthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createNineteenthCandidate()));
    }

    private Candidate createNineteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createFourthBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisNineteenthCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addNineteenthCandidateVariables(candidate);

        return null;
    }

    private void addNineteenthCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFifthLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createNinthStatementRawVariables()
        )));
    }

    private void mockAnalysisTwentiethCandidateVariable() {
        doAnswer(this::doMockAnalysisTwentiethCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwentiethCandidate()));
    }

    private Candidate createTwentiethCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFourthBlockSecondStatement(),
                createFourthBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisTwentiethCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwentiethCandidateVariables(candidate);

        return null;
    }

    private void addTwentiethCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                SECOND_GLOBAL_VARIABLE,
                "p1"
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables()
        )));
    }

    private void mockAnalysisTwentyFirstCandidateVariable() {
        doAnswer(this::doMockAnalysisTwentyFirstCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwentyFirstCandidate()));
    }

    private Candidate createTwentyFirstCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createFourthBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Object doMockAnalysisTwentyFirstCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwentyFirstCandidateVariables(candidate);

        return null;
    }

    private void addTwentyFirstCandidateVariables(Candidate candidate) {
        candidate.setGlobalVariables(Arrays.asList(
                "r",
                SECOND_GLOBAL_VARIABLE,
                "p1",
                "p2"
        ));
        candidate.setRawVariables(new ArrayList<>(Collections.singletonList(
                createTenthStatementRawVariables()
        )));
    }

    private void mockRemainingAnalysisVariable() {
        mockFirstBlockRemainingAnalysisVariable();
        mockSecondBlockRemainingAnalysisVariable();
        mockThirdBlockRemainingAnalysisVariable();
        mockFourthBlockRemainingAnalysisVariable();
    }

    private void mockFirstBlockRemainingAnalysisVariable() {
        mockAnalysisFirstRemainingCandidateVariable();
        mockAnalysisSecondRemainingCandidateVariable();
        mockAnalysisThirdRemainingCandidateVariable();
        mockAnalysisFourthRemainingCandidateVariable();
        mockAnalysisFifthRemainingCandidateVariable();
        mockAnalysisSixthRemainingCandidateVariable();
    }

    private void mockSecondBlockRemainingAnalysisVariable() {
        mockAnalysisSeventhRemainingCandidateVariable();
        mockAnalysisEighthRemainingCandidateVariable();
        mockAnalysisNinthRemainingCandidateVariable();
    }

    private void mockThirdBlockRemainingAnalysisVariable() {
        mockAnalysisTenthRemainingCandidateVariable();
        mockAnalysisEleventhRemainingCandidateVariable();
        mockAnalysisTwelfthRemainingCandidateVariable();
        mockAnalysisThirteenthRemainingCandidateVariable();
        mockAnalysisFourteenthRemainingCandidateVariable();
        mockAnalysisFifteenthRemainingCandidateVariable();
    }

    private void mockFourthBlockRemainingAnalysisVariable() {
        mockAnalysisSixteenthRemainingCandidateVariable();
        mockAnalysisSeventeenthRemainingCandidateVariable();
        mockAnalysisEighteenthRemainingCandidateVariable();
        mockAnalysisNineteenthRemainingCandidateVariable();
        mockAnalysisTwentiethRemainingCandidateVariable();
        mockAnalysisTwentyFirstRemainingCandidateVariable();
    }

    private void mockAnalysisFirstRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisFourthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFirstRemainingCandidate()));
    }

    private Candidate createFirstRemainingCandidate() {
        return createRemainingCandidate(createFirstCandidate());
    }

    private Candidate createRemainingCandidate(Candidate candidate) {
        BlockModel methodBlock = CandidateHelper.getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, candidateBlock);

        return createCandidate(remainingBlock.getStatements());
    }

    private void mockAnalysisSecondRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisSixthCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSecondRemainingCandidate()));
    }

    private Candidate createSecondRemainingCandidate() {
        return createRemainingCandidate(createSecondCandidate());
    }

    private void mockAnalysisThirdRemainingCandidateVariable() {
        doNothing()
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createThirdRemainingCandidate()));
    }

    private Candidate createThirdRemainingCandidate() {
        return createRemainingCandidate(createThirdCandidate());
    }

    private void mockAnalysisFourthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisFourthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFourthRemainingCandidate()));
    }

    private Candidate createFourthRemainingCandidate() {
        return createRemainingCandidate(createFourthCandidate());
    }

    private Object doMockAnalysisFourthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFourthRemainingCandidateVariable(candidate);

        return null;
    }

    private void addFourthRemainingCandidateVariable(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFirstLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisFifthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisFirstCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFifthRemainingCandidate()));
    }

    private Candidate createFifthRemainingCandidate() {
        return createRemainingCandidate(createFifthCandidate());
    }

    private void mockAnalysisSixthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisSecondCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSixthRemainingCandidate()));
    }

    private Candidate createSixthRemainingCandidate() {
        return createRemainingCandidate(createSixthCandidate());
    }

    private void mockAnalysisSeventhRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisSeventhRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSeventhRemainingCandidate()));
    }

    private Candidate createSeventhRemainingCandidate() {
        return createRemainingCandidate(createSeventhCandidate());
    }

    private Object doMockAnalysisSeventhRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSeventhRemainingCandidateVariables(candidate);

        return null;
    }

    private void addSeventhRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(Collections.singletonList(
                SECOND_GLOBAL_VARIABLE
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisEighthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisEighthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEighthRemainingCandidate()));
    }

    private Candidate createEighthRemainingCandidate() {
        return createRemainingCandidate(createEighthCandidate());
    }

    private Object doMockAnalysisEighthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEighthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addEighthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Collections.singletonList(
                createFirstLocalVariable()
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisNinthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisNinthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createNinthRemainingCandidate()));
    }

    private Candidate createNinthRemainingCandidate() {
        return createRemainingCandidate(createNinthCandidate());
    }

    private Object doMockAnalysisNinthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addNinthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addNinthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setGlobalVariables(Collections.singletonList(
                FIRST_GLOBAL_VARIABLE
        ));
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisTenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisTenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTenthRemainingCandidate()));
    }

    private Candidate createTenthRemainingCandidate() {
        return createRemainingCandidate(createTenthCandidate());
    }

    private Object doMockAnalysisTenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addTenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisEleventhRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisEleventhRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEleventhRemainingCandidate()));
    }

    private Candidate createEleventhRemainingCandidate() {
        return createRemainingCandidate(createEleventhCandidate());
    }

    private Object doMockAnalysisEleventhRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEleventhRemainingCandidateVariables(candidate);

        return null;
    }

    private void addEleventhRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisTwelfthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisTwelfthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwelfthRemainingCandidate()));
    }

    private Candidate createTwelfthRemainingCandidate() {
        return createRemainingCandidate(createTwelfthCandidate());
    }

    private Object doMockAnalysisTwelfthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwelfthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addTwelfthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisThirteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisThirteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createThirteenthRemainingCandidate()));
    }

    private Candidate createThirteenthRemainingCandidate() {
        return createRemainingCandidate(createThirteenthCandidate());
    }

    private Object doMockAnalysisThirteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addThirteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addThirteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisFourteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisFourteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFourteenthRemainingCandidate()));
    }

    private Candidate createFourteenthRemainingCandidate() {
        return createRemainingCandidate(createFourteenthCandidate());
    }

    private Object doMockAnalysisFourteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFourteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addFourteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisFifteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisFifteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createFifteenthRemainingCandidate()));
    }

    private Candidate createFifteenthRemainingCandidate() {
        return createRemainingCandidate(createFifteenthCandidate());
    }

    private Object doMockAnalysisFifteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFifteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addFifteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisSixteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisSixteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSixteenthRemainingCandidate()));
    }

    private Candidate createSixteenthRemainingCandidate() {
        return createRemainingCandidate(createSixteenthCandidate());
    }

    private Object doMockAnalysisSixteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSixteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addSixteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createNinthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisSeventeenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisSeventeenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createSeventeenthRemainingCandidate()));
    }

    private Candidate createSeventeenthRemainingCandidate() {
        return createRemainingCandidate(createSeventeenthCandidate());
    }

    private Object doMockAnalysisSeventeenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSeventeenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addSeventeenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisEighteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisEighteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createEighteenthRemainingCandidate()));
    }

    private Candidate createEighteenthRemainingCandidate() {
        return createRemainingCandidate(createEighteenthCandidate());
    }

    private Object doMockAnalysisEighteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addEighteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addEighteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisNineteenthRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisNineteenthRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createNineteenthRemainingCandidate()));
    }

    private Candidate createNineteenthRemainingCandidate() {
        return createRemainingCandidate(createNineteenthCandidate());
    }

    private Object doMockAnalysisNineteenthRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addNineteenthRemainingCandidateVariables(candidate);

        return null;
    }

    private void addNineteenthRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createTenthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisTwentiethRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisTwentiethRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwentiethRemainingCandidate()));
    }

    private Candidate createTwentiethRemainingCandidate() {
        return createRemainingCandidate(createTwentiethCandidate());
    }

    private Object doMockAnalysisTwentiethRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwentiethRemainingCandidateVariables(candidate);

        return null;
    }

    private void addTwentiethRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private void mockAnalysisTwentyFirstRemainingCandidateVariable() {
        doAnswer(this::doMockAnalysisTwentyFirstRemainingCandidateVariable)
                .when(candidateVariableAnalysis)
                .analysis(eq(methodModel), eq(createTwentyFirstRemainingCandidate()));
    }

    private Candidate createTwentyFirstRemainingCandidate() {
        return createRemainingCandidate(createTwentyFirstCandidate());
    }

    private Object doMockAnalysisTwentyFirstRemainingCandidateVariable(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addTwentyFirstRemainingCandidateVariables(candidate);

        return null;
    }

    private void addTwentyFirstRemainingCandidateVariables(Candidate candidate) {
        candidate.setLocalVariables(Arrays.asList(
                createFirstLocalVariable(),
                createSecondLocalVariable(),
                createThirdLocalVariable(),
                createFourthLocalVariable(),
                createFifthLocalVariable()
        ));
        candidate.setGlobalVariables(createGlobalVariables());
        candidate.setRawVariables(new ArrayList<>(Arrays.asList(
                createFirstStatementRawVariables(),
                createSecondStatementRawVariables(),
                createThirdStatementRawVariables(),
                createFourthStatementRawVariables(),
                createFifthStatementRawVariables(),
                createSixthStatementRawVariables(),
                createSeventhStatementRawVariables(),
                createEighthStatementRawVariables(),
                createNinthStatementRawVariables(),
                createEleventhStatementRawVariables()
        )));
    }

    private List<Candidate> createExpectedCandidates() {
        return Arrays.asList(
                createExpectedSeventhCandidate(),
                createExpectedEighthCandidate(),
                createExpectedNinthCandidate(),
                createExpectedTwelfthCandidate(),
                createExpectedEighteenthCandidate()
        );
    }

    private Candidate createExpectedSeventhCandidate() {
        Candidate seventhCandidate = createSeventhCandidate();
        addSeventhCandidateVariables(seventhCandidate);

        return seventhCandidate;
    }

    private Candidate createExpectedEighthCandidate() {
        Candidate eighthCandidate = createEighthCandidate();
        addEighthCandidateVariables(eighthCandidate);

        return eighthCandidate;
    }

    private Candidate createExpectedNinthCandidate() {
        Candidate ninthCandidate = createNinthCandidate();
        addNinthCandidateVariables(ninthCandidate);

        return ninthCandidate;
    }

    private Candidate createExpectedTwelfthCandidate() {
        Candidate twelfthCandidate = createTwelfthCandidate();
        addTwelfthCandidateVariables(twelfthCandidate);

        return twelfthCandidate;
    }

    private Candidate createExpectedEighteenthCandidate() {
        Candidate eighteenthCandidate = createEighteenthCandidate();
        addEighteenthCandidateVariables(eighteenthCandidate);

        return eighteenthCandidate;
    }
}