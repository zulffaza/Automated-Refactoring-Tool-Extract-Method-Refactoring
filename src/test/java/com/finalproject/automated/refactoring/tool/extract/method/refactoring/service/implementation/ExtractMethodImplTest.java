package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateScoreAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import com.finalproject.automated.refactoring.tool.utils.model.request.ReplaceFileVA;
import com.finalproject.automated.refactoring.tool.utils.service.MethodModelHelper;
import com.finalproject.automated.refactoring.tool.utils.service.ReplaceFileHelper;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExtractMethodImplTest {

    @Autowired
    private ExtractMethod extractMethod;

    @MockBean
    private CandidateAnalysis candidateAnalysis;

    @MockBean
    private CandidateScoreAnalysis candidateScoreAnalysis;

    @MockBean
    private MethodModelHelper methodModelHelper;

    @MockBean
    private ReplaceFileHelper replaceFileHelper;

    private static final String PATH = "filePath";
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

        when(candidateAnalysis.analysis(eq(methodModel)))
                .thenReturn(createExpectedCandidates());
        mockCandidateScoreAnalysis();
        mockMethodModelHelper();
        mockReplaceFile();
    }

    @Test
    public void refactoring_success() {
        assertTrue(extractMethod.refactoring(PATH, methodModel));

        verify(candidateAnalysis).analysis(eq(methodModel));
        verifyCandidateScoreAnalysis();
        verifyMethodModelHelper();
        verifyReplaceFile();

        verifyNoMoreInteractions(candidateAnalysis);
        verifyNoMoreInteractions(candidateScoreAnalysis);
        verifyNoMoreInteractions(methodModelHelper);
        verifyNoMoreInteractions(replaceFileHelper);
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
        extractMethod.refactoring(PATH, null);
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

    private List<Candidate> createExpectedCandidates() {
        return new ArrayList<>(Arrays.asList(
                createExpectedSeventhCandidate(),
                createExpectedEighthCandidate(),
                createExpectedNinthCandidate(),
                createExpectedTwelfthCandidate(),
                createExpectedEighteenthCandidate()
        ));
    }

    private Candidate createCandidate(List<StatementModel> statements) {
        return Candidate.builder()
                .statements(statements)
                .build();
    }

    private Candidate createSeventhCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createSecondBlockFirstStatement()
        ));

        return createCandidate(statements);
    }

    private Candidate createExpectedSeventhCandidate() {
        Candidate seventhCandidate = createSeventhCandidate();
        addSeventhCandidateVariables(seventhCandidate);

        return seventhCandidate;
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

    private Candidate createEighthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createSecondBlockFirstStatement(),
                createSecondBlockSecondStatement()
        ));

        return createCandidate(statements);
    }

    private Candidate createExpectedEighthCandidate() {
        Candidate eighthCandidate = createEighthCandidate();
        addEighthCandidateVariables(eighthCandidate);

        return eighthCandidate;
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

    private Candidate createNinthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createSecondBlockSecondStatement()
        ));

        return createCandidate(statements);
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

    private Candidate createExpectedNinthCandidate() {
        Candidate ninthCandidate = createNinthCandidate();
        addNinthCandidateVariables(ninthCandidate);

        return ninthCandidate;
    }

    private Candidate createTwelfthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createThirdBlockFirstStatement(),
                createThirdBlockSecondStatement(),
                createThirdBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Candidate createExpectedTwelfthCandidate() {
        Candidate twelfthCandidate = createTwelfthCandidate();
        addTwelfthCandidateVariables(twelfthCandidate);

        return twelfthCandidate;
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

    private Candidate createEighteenthCandidate() {
        List<StatementModel> statements = new ArrayList<>(Arrays.asList(
                createFourthBlockFirstStatement(),
                createFourthBlockSecondStatement(),
                createFourthBlockThirdStatement()
        ));

        return createCandidate(statements);
    }

    private Candidate createExpectedEighteenthCandidate() {
        Candidate eighteenthCandidate = createEighteenthCandidate();
        addEighteenthCandidateVariables(eighteenthCandidate);

        return eighteenthCandidate;
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

    private void mockCandidateScoreAnalysis() {
        mockFirstCandidateScoreAnalysis();
        mockSecondCandidateScoreAnalysis();
        mockThirdCandidateScoreAnalysis();
        mockFourthCandidateScoreAnalysis();
        mockFifthCandidateScoreAnalysis();
    }

    private void mockFirstCandidateScoreAnalysis() {
        doAnswer(this::doMockFirstCandidateScoreAnalysis)
                .when(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(createExpectedSeventhCandidate()));
    }

    private Object doMockFirstCandidateScoreAnalysis(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFirstCandidateScoreAnalysis(candidate);

        return null;
    }

    private void addFirstCandidateScoreAnalysis(Candidate candidate) {
        candidate.setLengthScore(0.4);
        candidate.setNestingDepthScore(0.0);
        candidate.setNestingAreaScore(0.0);
        candidate.setParameterScore(3.0);
        candidate.setTotalScore(3.4);
        candidate.setParameters(createCandidateParameters());
    }

    private List<PropertyModel> createCandidateParameters() {
        return Collections.singletonList(
                createFirstLocalVariable()
        );
    }

    private void mockSecondCandidateScoreAnalysis() {
        doAnswer(this::doMockSecondCandidateScoreAnalysis)
                .when(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(createExpectedEighthCandidate()));
    }

    private Object doMockSecondCandidateScoreAnalysis(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addSecondCandidateScoreAnalysis(candidate);

        return null;
    }

    private void addSecondCandidateScoreAnalysis(Candidate candidate) {
        candidate.setLengthScore(0.30000000000000004);
        candidate.setNestingDepthScore(1.0);
        candidate.setNestingAreaScore(0.0);
        candidate.setParameterScore(3.0);
        candidate.setTotalScore(4.3);
        candidate.setParameters(createCandidateParameters());
    }

    private void mockThirdCandidateScoreAnalysis() {
        doAnswer(this::doMockThirdCandidateScoreAnalysis)
                .when(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(createExpectedNinthCandidate()));
    }

    private Object doMockThirdCandidateScoreAnalysis(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addThirdCandidateScoreAnalysis(candidate);

        return null;
    }

    private void addThirdCandidateScoreAnalysis(Candidate candidate) {
        candidate.setLengthScore(0.4);
        candidate.setNestingDepthScore(0.0);
        candidate.setNestingAreaScore(0.0);
        candidate.setParameterScore(3.0);
        candidate.setTotalScore(3.4);
        candidate.setParameters(createCandidateParameters());
    }

    private void mockFourthCandidateScoreAnalysis() {
        doAnswer(this::doMockFourthCandidateScoreAnalysis)
                .when(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(createExpectedTwelfthCandidate()));
    }

    private Object doMockFourthCandidateScoreAnalysis(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFourthCandidateScoreAnalysis(candidate);

        return null;
    }

    private void addFourthCandidateScoreAnalysis(Candidate candidate) {
        candidate.setLengthScore(0.30000000000000004);
        candidate.setNestingDepthScore(0.0);
        candidate.setNestingAreaScore(0.0);
        candidate.setParameterScore(3.0);
        candidate.setTotalScore(3.3);
        candidate.setParameters(createCandidateParameters());
    }

    private void mockFifthCandidateScoreAnalysis() {
        doAnswer(this::doMockFifthCandidateScoreAnalysis)
                .when(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(createExpectedEighteenthCandidate()));
    }

    private Object doMockFifthCandidateScoreAnalysis(InvocationOnMock invocationOnMock) {
        Candidate candidate = invocationOnMock.getArgument(SECOND_INDEX);
        addFifthCandidateScoreAnalysis(candidate);

        return null;
    }

    private void addFifthCandidateScoreAnalysis(Candidate candidate) {
        candidate.setLengthScore(0.30000000000000004);
        candidate.setNestingDepthScore(0.0);
        candidate.setNestingAreaScore(0.0);
        candidate.setParameterScore(3.0);
        candidate.setTotalScore(3.3);
        candidate.setParameters(createCandidateParameters());
    }

    private void mockMethodModelHelper() {
        when(methodModelHelper.createMethodRegex(eq(methodModel)))
                .thenReturn(createMethodModelRegex());
        when(methodModelHelper.createMethod(eq(createCandidateMethodModel())))
                .thenReturn(createCandidateMethodModelString());
        when(methodModelHelper.createMethod(eq(createRemainingMethodModel())))
                .thenReturn(createRemainingMethodModelString());
    }

    private String createMethodModelRegex() {
        return "public(?:\\s)*Rectangle2D(?:\\s)*getFigureDrawBounds(?:\\s)*\\((?:\\s)*\\)(?:\\s)*\\{(?:\\s)*Rectangle2D(?:\\s)*r(?:\\s)*=(?:\\s)*super\\.getFigDrawBounds\\(\\);(?:\\s)*if(?:\\s)*\\(getNodeCount\\(\\)(?:\\s)*>(?:\\s)*1\\)(?:\\s)*\\{(?:\\s)*if(?:\\s)*\\(START\\.get\\(this\\)(?:\\s)*!=(?:\\s)*null\\)(?:\\s)*\\{(?:\\s)*Point(?:\\s)*p1(?:\\s)*=(?:\\s)*getPoint\\(0,(?:\\s)*0\\);(?:\\s)*Point(?:\\s)*p2(?:\\s)*=(?:\\s)*getPoint\\(1,(?:\\s)*0\\);(?:\\s)*r\\.add\\(START\\.get\\(this\\)\\.getBounds\\(p1,(?:\\s)*p2\\)\\);(?:\\s)*\\}(?:\\s)*if(?:\\s)*\\(END\\.get\\(this\\)(?:\\s)*!=(?:\\s)*null\\)(?:\\s)*\\{(?:\\s)*Point(?:\\s)*p1(?:\\s)*=(?:\\s)*getPoint\\(getNodeCount\\(\\)(?:\\s)*\\-(?:\\s)*1,(?:\\s)*0\\);(?:\\s)*Point(?:\\s)*p2(?:\\s)*=(?:\\s)*getPoint\\(getNodeCount\\(\\)(?:\\s)*\\-(?:\\s)*2,(?:\\s)*0\\);(?:\\s)*r\\.add\\(END\\.get\\(this\\)\\.getBounds\\(p1,(?:\\s)*p2\\)\\);(?:\\s)*\\}(?:\\s)*\\}(?:\\s)*return(?:\\s)*r;(?:\\s)*}";
    }

    private MethodModel createCandidateMethodModel() {
        Candidate candidate = createExpectedEighthCandidate();

        return MethodModel.builder()
                .keywords(Collections.singletonList("private"))
                .returnType("void")
                .name(METHOD_NAME + "Extracted")
                .parameters(createCandidateParameters())
                .globalVariables(candidate.getGlobalVariables())
                .localVariables(candidate.getLocalVariables())
                .body("if (START.get(this) != null) {\n" +
                        "\t\t\tPoint p1 = getPoint(0, 0);\n" +
                        "\t\t\tPoint p2 = getPoint(1, 0);\n" +
                        "\t\t\tr.add(START.get(this).getBounds(p1, p2));\n" +
                        "\t\t}\n" +
                        "\t\tif (END.get(this) != null) {\n" +
                        "\t\t\tPoint p1 = getPoint(getNodeCount() - 1, 0);\n" +
                        "\t\t\tPoint p2 = getPoint(getNodeCount() - 2, 0);\n" +
                        "\t\t\tr.add(END.get(this).getBounds(p1, p2));\n" +
                        "\t\t}")
                .statements(candidate.getStatements())
                .build();
    }

    private String createCandidateMethodModelString() {
        return "private void getFigureDrawBoundsExtracted(Rectangle2D r) {\n" +
                "\tif (START.get(this) != null) {\n" +
                "\t\t\tPoint p1 = getPoint(0, 0);\n" +
                "\t\t\tPoint p2 = getPoint(1, 0);\n" +
                "\t\t\tr.add(START.get(this).getBounds(p1, p2));\n" +
                "\t\t}\n" +
                "\t\tif (END.get(this) != null) {\n" +
                "\t\t\tPoint p1 = getPoint(getNodeCount() - 1, 0);\n" +
                "\t\t\tPoint p2 = getPoint(getNodeCount() - 2, 0);\n" +
                "\t\t\tr.add(END.get(this).getBounds(p1, p2));\n" +
                "\t\t}\n" +
                "}";
    }

    private MethodModel createRemainingMethodModel() {
        StatementModel secondStatement = createSecondStatement();
        ((BlockModel) secondStatement).setStatements(createNewStatements());

        return MethodModel.builder()
                .keywords(Collections.singletonList("public"))
                .returnType("Rectangle2D")
                .name(METHOD_NAME)
                .body("Rectangle2D r = super.getFigDrawBounds();\n" +
                        "\t\tif (getNodeCount() > 1) {\n" +
                        "\t\t\tgetFigureDrawBoundsExtracted(r);\n" +
                        "\t\t}\n" +
                        "\t\treturn r;")
                .statements(Arrays.asList(
                        createFirstStatement(),
                        secondStatement,
                        createThirdStatement()
                ))
                .build();
    }

    private List<StatementModel> createNewStatements() {
        return Collections.singletonList(
                StatementModel.statementBuilder()
                        .index(2)
                        .statement("getFigureDrawBoundsExtracted(r);")
                        .build()
        );
    }

    private String createRemainingMethodModelString() {
        return "public Rectangle2D getFigureDrawBounds() {\n" +
                "\tRectangle2D r = super.getFigDrawBounds();\n" +
                "\t\tif (getNodeCount() > 1) {\n" +
                "\t\t\tgetFigureDrawBoundsExtracted(r);\n" +
                "\t\t}\n" +
                "\t\treturn r;\n" +
                "}";
    }

    private void mockReplaceFile() {
        when(replaceFileHelper.replaceFile(eq(createSuccessReplaceFileVA())))
                .thenReturn(Boolean.TRUE);
    }

    private ReplaceFileVA createSuccessReplaceFileVA() {
        return ReplaceFileVA.builder()
                .filePath(PATH)
                .target(createMethodModelRegex())
                .replacement(createReplacementString())
                .build();
    }

    private String createReplacementString() {
        return "public Rectangle2D getFigureDrawBounds() {\n" +
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
                "\t}";
    }

    private void verifyCandidateScoreAnalysis() {
        verifyFirstCandidateScoreAnalysis();
        verifySecondCandidateScoreAnalysis();
        verifyThirdCandidateScoreAnalysis();
        verifyFourthCandidateScoreAnalysis();
        verifyFifthCandidateScoreAnalysis();
    }

    private void verifyFirstCandidateScoreAnalysis() {
        Candidate expectedSeventhCandidate = createExpectedSeventhCandidate();
        addFirstCandidateScoreAnalysis(expectedSeventhCandidate);

        verify(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(expectedSeventhCandidate));
    }

    private void verifySecondCandidateScoreAnalysis() {
        Candidate expectedEighthCandidate = createExpectedEighthCandidate();
        addSecondCandidateScoreAnalysis(expectedEighthCandidate);

        verify(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(expectedEighthCandidate));
    }

    private void verifyThirdCandidateScoreAnalysis() {
        Candidate expectedNinthCandidate = createExpectedNinthCandidate();
        addThirdCandidateScoreAnalysis(expectedNinthCandidate);

        verify(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(expectedNinthCandidate));
    }

    private void verifyFourthCandidateScoreAnalysis() {
        Candidate expectedTwelfthCandidate = createExpectedTwelfthCandidate();
        addFourthCandidateScoreAnalysis(expectedTwelfthCandidate);

        verify(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(expectedTwelfthCandidate));
    }

    private void verifyFifthCandidateScoreAnalysis() {
        Candidate expectedEighteenthCandidate = createExpectedEighteenthCandidate();
        addFifthCandidateScoreAnalysis(expectedEighteenthCandidate);

        verify(candidateScoreAnalysis)
                .analysis(eq(methodModel), eq(expectedEighteenthCandidate));
    }

    private void verifyMethodModelHelper() {
        verify(methodModelHelper)
                .createMethodRegex(eq(methodModel));
        verify(methodModelHelper)
                .createMethod(eq(createCandidateMethodModel()));
        verify(methodModelHelper)
                .createMethod(eq(createRemainingMethodModel()));
    }

    private void verifyReplaceFile() {
        verify(replaceFileHelper)
                .replaceFile(eq(createSuccessReplaceFileVA()));
    }
}