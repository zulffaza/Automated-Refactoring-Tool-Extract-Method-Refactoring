package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper;

import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 27 June 2019
 */

public class CandidateHelperTest {

    private static final Integer FIRST_INDEX = 0;
    private static final Integer PARENT_BLOCK_STATEMENT_COUNT = 3;
    private static final Integer SEARCH_STATEMENT_INDEX = 4;

    @Before
    public void setUp() {
    }

    @Test
    public void getMethodBlockStatement_success() {
        StatementModel statement = StatementModel.statementBuilder()
                .build();
        List<StatementModel> statements = Collections.singletonList(statement);
        BlockModel blockModel = CandidateHelper.getMethodBlockStatement(statements);

        assertEquals(statements, blockModel.getStatements());
        assertEquals(statement, blockModel.getStatements().get(FIRST_INDEX));
    }

    @Test
    public void getMethodBlockStatement_success_emptyStatements() {
        List<StatementModel> statements = Collections.emptyList();
        BlockModel blockModel = CandidateHelper.getMethodBlockStatement(statements);

        assertEquals(statements, blockModel.getStatements());
    }

    @Test(expected = NullPointerException.class)
    public void getMethodBlockStatement_failed_statementsIsNull() {
        CandidateHelper.getMethodBlockStatement(null);
    }

    @Test
    public void getRemainingBlockModel_success() {
        BlockModel methodBlock = createParentBlockStatement();
        BlockModel removedBlock = CandidateHelper.getMethodBlockStatement(
                Collections.singletonList(createBlockFirstStatement()));

        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, removedBlock);
        assertEquals(createExpectedRemainingBlock(), remainingBlock);
    }

    @Test(expected = NullPointerException.class)
    public void getRemainingBlockModel_failed_methodBlockIsNull() {
        BlockModel removedBlock = CandidateHelper.getMethodBlockStatement(
                Collections.singletonList(createBlockFirstStatement()));
        CandidateHelper.getRemainingBlockModel(null, removedBlock);
    }

    @Test(expected = NullPointerException.class)
    public void getRemainingBlockModel_failed_candidateBlockIsNull() {
        BlockModel methodBlock = createParentBlockStatement();
        CandidateHelper.getRemainingBlockModel(methodBlock, null);
    }

    @Test
    public void getStatementCount_success() {
        BlockModel methodBlock = createParentBlockStatement();
        Integer statementCount = CandidateHelper.getStatementCount(methodBlock);

        assertEquals(PARENT_BLOCK_STATEMENT_COUNT, statementCount);
    }

    @Test(expected = NullPointerException.class)
    public void getStatementCount_failed_blockIsNull() {
        CandidateHelper.getStatementCount(null);
    }

    @Test
    public void isLocalVariableNameEquals_success() {
        VariablePropertyModel localVariable = createLocalVariable();
        assertTrue(CandidateHelper.isLocalVariableNameEquals(localVariable, "p1"));
    }

    @Test
    public void isLocalVariableNameEquals_success_nameNotEquals() {
        VariablePropertyModel localVariable = createLocalVariable();
        assertFalse(CandidateHelper.isLocalVariableNameEquals(localVariable, "p2"));
    }

    @Test(expected = NullPointerException.class)
    public void isLocalVariableNameEquals_failed_localVariableIsNull() {
        CandidateHelper.isLocalVariableNameEquals(null, "p2");
    }

    @Test
    public void isLocalVariableNameEquals_failed_variableNameIsNull() {
        VariablePropertyModel localVariable = createLocalVariable();
        assertFalse(CandidateHelper.isLocalVariableNameEquals(localVariable, null));
    }

    @Test
    public void mergeAllStatements_success() {
        List<StatementModel> statements = new ArrayList<>(Collections.singletonList(
                createParentBlockStatement()
        ));
        List<StatementModel> mergeStatements = CandidateHelper.mergeAllStatements(statements);

        assertEquals(createExpectedMergeStatements(), mergeStatements);
    }

    @Test
    public void mergeAllStatements_success_emptyStatements() {
        List<StatementModel> mergeStatements = CandidateHelper.mergeAllStatements(Collections.emptyList());
        assertEquals(Collections.emptyList(), mergeStatements);
    }

    @Test(expected = NullPointerException.class)
    public void mergeAllStatements_failed_nullStatements() {
        CandidateHelper.mergeAllStatements(null);
    }

    @Test
    public void searchStatementByIndex_success() {
        StatementModel statementModel = CandidateHelper.searchStatementByIndex(
                createParentBlockStatement().getStatements(), SEARCH_STATEMENT_INDEX);
        assertEquals(createBlockSecondStatement(), statementModel);
    }

    @Test
    public void searchStatementByIndex_success_statementNotFound() {
        StatementModel statementModel = CandidateHelper.searchStatementByIndex(
                Collections.emptyList(), SEARCH_STATEMENT_INDEX);
        assertNull(statementModel.getIndex());
        assertNull(statementModel.getStatement());
        assertNull(statementModel.getStartIndex());
        assertNull(statementModel.getEndIndex());
    }

    @Test
    public void searchStatementByIndex_failed_indexIsNull() {
        StatementModel statementModel = CandidateHelper.searchStatementByIndex(
                createParentBlockStatement().getStatements(), null);
        assertNull(statementModel.getIndex());
        assertNull(statementModel.getStatement());
        assertNull(statementModel.getStartIndex());
        assertNull(statementModel.getEndIndex());
    }

    @Test(expected = NullPointerException.class)
    public void searchStatementByIndex_failed_statementIsNull() {
        CandidateHelper.searchStatementByIndex(null, SEARCH_STATEMENT_INDEX);
    }

    @Test
    public void searchIndexOfStatements_success() {
        List<StatementModel> statements = createParentBlockStatement().getStatements();
        StatementModel searchStatement = createBlockSecondStatement();

        Integer index = CandidateHelper.searchIndexOfStatements(statements, searchStatement);
        assertEquals(1, index.intValue());
    }

    @Test
    public void searchIndexOfStatements_success_statementNotFound() {
        List<StatementModel> statements = createParentBlockStatement().getStatements();
        StatementModel searchStatement = StatementModel.statementBuilder()
                .build();

        Integer index = CandidateHelper.searchIndexOfStatements(statements, searchStatement);
        assertEquals(-1, index.intValue());
    }

    @Test
    public void searchIndexOfStatements_success_statementsEmpty() {
        List<StatementModel> statements = Collections.emptyList();
        StatementModel searchStatement = createBlockSecondStatement();

        Integer index = CandidateHelper.searchIndexOfStatements(statements, searchStatement);
        assertEquals(-1, index.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void searchIndexOfStatements_failed_statementsIsNull() {
        StatementModel searchStatement = createBlockSecondStatement();
        CandidateHelper.searchIndexOfStatements(null, searchStatement);
    }

    @Test(expected = NullPointerException.class)
    public void searchIndexOfStatements_failed_statementIsNull() {
        List<StatementModel> statements = createParentBlockStatement().getStatements();
        CandidateHelper.searchIndexOfStatements(statements, null);
    }

    @Test
    public void isMatchRegex_success() {
        String test = "// Create new Database \n" +
                "Integer total = input + output;";
        assertTrue(CandidateHelper.isMatchRegex(test, VariableHelper.OPERATORS_CHARACTERS_REGEX));
    }

    @Test
    public void isMatchRegex_success_notFound() {
        String test = "String name = \"My new Name\"";
        assertTrue(CandidateHelper.isMatchRegex(test, VariableHelper.OPERATORS_CHARACTERS_REGEX));
    }

    @Test(expected = NullPointerException.class)
    public void isMatchRegex_failed_stringIsNull() {
        CandidateHelper.isMatchRegex(null, VariableHelper.OPERATORS_CHARACTERS_REGEX);
    }

    @Test(expected = NullPointerException.class)
    public void isMatchRegex_failed_regexIsNull() {
        String test = "String name = \"My new Name\"";
        CandidateHelper.isMatchRegex(test, null);
    }

    private BlockModel createParentBlockStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createBlockStatements())
                .endOfBlockStatement(createThirdBlockEndStatement())
                .build();
        blockModel.setStatement("if (START.get(this) != null) {");
        blockModel.setIndex(2);
        blockModel.setStartIndex(102);
        blockModel.setEndIndex(131);

        return blockModel;
    }

    private List<StatementModel> createBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createBlockFirstStatement());
        statements.add(createBlockSecondStatement());
        statements.add(createBlockThirdStatement());

        return statements;
    }

    private StatementModel createBlockFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p1 = getPoint(0, 0);")
                .index(3)
                .startIndex(152)
                .endIndex(177)
                .build();
    }

    private StatementModel createBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint(1, 0);")
                .index(4)
                .startIndex(198)
                .endIndex(223)
                .build();
    }

    private StatementModel createBlockThirdStatement() {
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

    private BlockModel createExpectedRemainingBlock() {
        BlockModel parentBlock = createParentBlockStatement();
        parentBlock.setStatements(new ArrayList<>(Arrays.asList(
                createBlockSecondStatement(),
                createBlockThirdStatement()
        )));

        return parentBlock;
    }

    private VariablePropertyModel createLocalVariable() {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(3)
                .build();

        variablePropertyModel.setType("Point");
        variablePropertyModel.setName("p1");

        return variablePropertyModel;
    }

    private List<StatementModel> createExpectedMergeStatements() {
        return Arrays.asList(
                createParentBlockStatement(),
                createBlockFirstStatement(),
                createBlockSecondStatement(),
                createBlockThirdStatement()
        );
    }
}