package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.AddCallExtractedMethodVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.GetFilteredRawVariablesVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.IsBlockCompleteVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.IsIfMissElseVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.ReadStatementIndexVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateVariableAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import com.finalproject.automated.refactoring.tool.utils.model.request.ReplaceFileVA;
import com.finalproject.automated.refactoring.tool.utils.service.MethodModelHelper;
import com.finalproject.automated.refactoring.tool.utils.service.ReplaceFileHelper;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@Service
public class ExtractMethodImpl implements ExtractMethod {

    @Autowired
    private CandidateVariableAnalysis candidateVariableAnalysis;

    @Autowired
    private MethodModelHelper methodModelHelper;

    @Autowired
    private ReplaceFileHelper replaceFileHelper;

    @Value("${threshold.min.candidate.statements}")
    private Integer minCandidateStatements;

    @Value("${statement.length.score.constant}")
    private Double statementLengthScoreConstant;

    @Value("${statement.length.score.max}")
    private Double statementLengthScoreMax;

    @Value("${nesting.area.score.constant}")
    private Double nestingAreaScoreConstant;

    @Value("${parameter.score.max}")
    private Double parameterScoreMax;

    private static final String ASSIGMENT_OPERATOR = "=";
    private static final String EQUAL_TO_OPERATOR = "==";
    private static final String NOT_EQUAL_TO_OPERATOR = "!=";
    private static final String INCREMENT_OPERATOR = "++";
    private static final String DECREMENT_OPERATOR = "--";
    private static final String AT = "@";
    private static final String IF = "if";
    private static final String ELSE = "else";
    private static final String TRY = "try";
    private static final String CATCH = "catch";
    private static final String FINALLY = "finally";
    private static final String DO = "do";
    private static final String WHILE = "while";
    private static final String METHOD_PUBLIC_MODIFIER = "public";
    private static final String METHOD_PROTECTED_MODIFIER = "protected";
    private static final String METHOD_PRIVATE_MODIFIER = "private";
    private static final String EXTRACTED_METHOD_NAME_SUFFIX = "Extracted";
    private static final String NEW_LINE = "\n";
    private static final String TAB = "\t";
    private static final String VOID_RETURN_TYPE = "void";
    private static final String METHOD_PARAMETER_PREFIX = "(";
    private static final String COMMA = ", ";
    private static final String METHOD_PARAMETER_SUFFIX = ");";

    private static final Integer SINGLE_LIST_SIZE = 1;
    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;
    private static final Integer INVALID_INDEX = -1;
    private static final Integer RETURN_TYPE_VALID_SCORE = 1;
    private static final Integer RETURN_TYPE_INVALID_SCORE = 0;

    private static final List<String> REMOVED_MODIFIERS = Arrays.asList(
            METHOD_PUBLIC_MODIFIER, METHOD_PROTECTED_MODIFIER, METHOD_PRIVATE_MODIFIER
    );

    @Override
    public Boolean refactoring(@NonNull String path, @NonNull MethodModel methodModel) {
        try {
            return doRefactoring(path, methodModel);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    private Boolean doRefactoring(String path, MethodModel methodModel) {
        Candidate bestCandidate = searchBestExtractCandidate(methodModel);
        return replaceFile(path, methodModel, bestCandidate);
    }

    private Candidate searchBestExtractCandidate(MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);
        scoringCandidates(methodModel, candidates);
        pruningCandidates(candidates);
        sortingCandidates(candidates);

        return candidates.get(FIRST_INDEX);
    }

    private List<Candidate> getCandidates(MethodModel methodModel) {
        List<List<StatementModel>> blocksMethod = getAllBlocksMethod(methodModel);

        return blocksMethod.stream()
                .flatMap(statements -> searchCandidates(methodModel, statements))
                .filter(candidate -> isCandidateValid(methodModel, candidate))
                .collect(Collectors.toList());
    }

    private List<List<StatementModel>> getAllBlocksMethod(MethodModel methodModel) {
        List<List<StatementModel>> blocks = initializeFirstBlock(methodModel);
        methodModel.getStatements()
                .forEach(statement -> searchBlockToSave(statement, blocks));

        return blocks;
    }

    private List<List<StatementModel>> initializeFirstBlock(MethodModel methodModel) {
        List<List<StatementModel>> blocks = new ArrayList<>();
        blocks.add(new ArrayList<>(methodModel.getStatements()));

        return blocks;
    }

    private void searchBlockToSave(StatementModel statement, List<List<StatementModel>> blocks) {
        if (statement instanceof BlockModel) {
            saveBlock(statement, blocks);
        }
    }

    private void saveBlock(StatementModel statement, List<List<StatementModel>> blocks) {
        BlockModel block = (BlockModel) statement;

        blocks.add(new ArrayList<>(block.getStatements()));
        block.getStatements()
                .forEach(blockStatement -> searchBlockToSave(blockStatement, blocks));
    }

    private Stream<Candidate> searchCandidates(MethodModel methodModel, List<StatementModel> statements) {
        List<Candidate> candidates = new ArrayList<>();

        for (int i = FIRST_INDEX; i < statements.size(); i++) {
            for (int j = i + SECOND_INDEX; j <= statements.size(); j++) {
                Candidate candidate = buildCandidate(methodModel, statements.subList(i, j));
                candidates.add(candidate);
            }
        }

        return candidates.stream();
    }

    private Candidate buildCandidate(MethodModel methodModel,
                                     List<StatementModel> candidateStatements) {
        candidateStatements = new ArrayList<>(candidateStatements);

        Candidate candidate = Candidate.builder()
                .statements(candidateStatements)
                .build();

        candidateVariableAnalysis.analysis(methodModel, candidate);

        return candidate;
    }

    private Boolean isCandidateValid(MethodModel methodModel, Candidate candidate) {
        return isBehaviourPreservationValid(methodModel, candidate) &&
                isQualityValid(methodModel, candidate);
    }

    private Boolean isBehaviourPreservationValid(MethodModel methodModel, Candidate candidate) {
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = getRemainingBlockModel(methodBlock, candidateBlock);

        List<List<String>> rawVariablesContainsAssigment = candidate.getRawVariables()
                .stream()
                .filter(rawVariables -> isContainsAssignment(methodModel, candidate, rawVariables))
                .collect(Collectors.toList());

        return isNoneCandidateVariableUseInRemaining(methodModel, candidate, remainingBlock) &&
                rawVariablesContainsAssigment.isEmpty() &&
                isBlockStatementComplete(methodBlock, candidate);
    }

    private BlockModel getMethodBlockStatement(List<StatementModel> statements) {
        return BlockModel.blockBuilder()
                .statements(new ArrayList<>(statements))
                .build();
    }

    private Boolean isNoneCandidateVariableUseInRemaining(MethodModel methodModel,
                                                          Candidate candidate, BlockModel remainingBlock) {
        List<List<String>> filteredRemainingRawVariables = getFilteredRawVariables(
                methodModel, candidate, remainingBlock);

        return filteredRemainingRawVariables.stream()
                .flatMap(Collection::stream)
                .noneMatch(rawVariable ->
                        getMethodVariable(candidate.getLocalVariables(), rawVariable).isPresent());
    }

    private List<StatementModel> mergeAllStatements(List<StatementModel> statements) {
        List<StatementModel> mergeStatements = new ArrayList<>();
        mergeStatements(statements, mergeStatements);

        return mergeStatements;
    }

    private void mergeStatements(List<StatementModel> statements, List<StatementModel> mergeStatements) {
        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            mergeStatement(statements.get(index), mergeStatements);
        }
    }

    private void mergeStatement(StatementModel statementModel, List<StatementModel> mergeStatements) {
        mergeStatements.add(statementModel);

        if (statementModel instanceof BlockModel) {
            mergeStatements(((BlockModel) statementModel).getStatements(), mergeStatements);
        }
    }

    private List<List<String>> getFilteredRawVariables(MethodModel methodModel,
                                                       Candidate candidate, BlockModel remainingBlock) {
        List<StatementModel> candidateStatements = mergeAllStatements(candidate.getStatements());
        List<List<String>> filteredRawVariables = new ArrayList<>();

        Integer indexFilter = candidateStatements.get(candidateStatements.size() - SECOND_INDEX).getIndex();

        if (isCandidateIsMultiBlock(candidateStatements)) {
            GetFilteredRawVariablesVA getFilteredRawVariablesVA = GetFilteredRawVariablesVA.builder()
                    .methodModel(methodModel)
                    .remainingBlock(remainingBlock)
                    .indexFilter(indexFilter)
                    .filteredRawVariables(filteredRawVariables)
                    .build();

            doGetFilteredRawVariables(getFilteredRawVariablesVA);
        }

        return filteredRawVariables;
    }

    private Boolean isCandidateIsMultiBlock(List<StatementModel> candidateStatements) {
        if (candidateStatements.get(FIRST_INDEX) instanceof BlockModel) {
            return !((BlockModel) candidateStatements.get(FIRST_INDEX)).getEndOfBlockStatement()
                    .getIndex()
                    .equals(candidateStatements.get(candidateStatements.size() - SECOND_INDEX)
                            .getIndex() + SECOND_INDEX);
        } else {
            return Boolean.TRUE;
        }
    }

    private void doGetFilteredRawVariables(GetFilteredRawVariablesVA getFilteredRawVariablesVA) {
        Candidate remainingCandidate = buildCandidate(getFilteredRawVariablesVA.getMethodModel(),
                getFilteredRawVariablesVA.getRemainingBlock().getStatements());
        List<StatementModel> remainingStatements = mergeAllStatements(remainingCandidate.getStatements());
        candidateVariableAnalysis.analysis(getFilteredRawVariablesVA.getMethodModel(), remainingCandidate);

        Integer maxIndexFilter = searchMaxIndexFilter(remainingStatements,
                getFilteredRawVariablesVA.getIndexFilter());

        for (int index = FIRST_INDEX; index < remainingStatements.size(); index++) {
            StatementModel statementModel = remainingStatements.get(index);

            if (isSearchInRange(statementModel, maxIndexFilter, getFilteredRawVariablesVA)) {
                getFilteredRawVariablesVA.getFilteredRawVariables()
                        .add(remainingCandidate.getRawVariables().get(index));
            }
        }
    }

    private Integer searchMaxIndexFilter(List<StatementModel> remainingStatements, Integer indexFilter) {
        AtomicInteger maxIndexFilter = new AtomicInteger();

        if (!remainingStatements.isEmpty()) {
            doSearchMaxIndexFilter(remainingStatements, maxIndexFilter, indexFilter);
        }

        return maxIndexFilter.get();
    }

    private void doSearchMaxIndexFilter(List<StatementModel> remainingStatements,
                                        AtomicInteger maxIndexFilter, Integer indexFilter) {
        StatementModel lastStatementModel = searchStatementByIndex(remainingStatements,
                indexFilter + SECOND_INDEX);
        Integer firstIndex = searchIndexOfStatements(remainingStatements, lastStatementModel);
        maxIndexFilter.set(remainingStatements.get(remainingStatements.size() - SECOND_INDEX)
                .getIndex());

        for (int index = firstIndex; index >= FIRST_INDEX; index--) {
            StatementModel statementModel = remainingStatements.get(index);

            if (isInsideBlock(statementModel, indexFilter)) {
                maxIndexFilter.set(((BlockModel) statementModel).getEndOfBlockStatement().getIndex());
                break;
            }
        }
    }

    private StatementModel searchStatementByIndex(List<StatementModel> statements, Integer index) {
        StatementModel statementModel = StatementModel.statementBuilder().build();
        findStatementByIndex(statements, statementModel, index);

        return statementModel;
    }

    private void findStatementByIndex(List<StatementModel> statements,
                                      StatementModel statementModel, Integer indexToFind) {
        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            if (statementModel.getIndex() != null) {
                break;
            }

            checkStatementIndex(statements.get(index), statementModel, indexToFind);
        }
    }

    private void checkStatementIndex(StatementModel statementModel,
                                     StatementModel tempStatementModel, Integer indexToFind) {
        if (statementModel.getIndex().equals(indexToFind)) {
            copyStatementModel(statementModel, tempStatementModel);
            return;
        }

        if (statementModel instanceof BlockModel) {
            findStatementByIndex(((BlockModel) statementModel).getStatements(),
                    tempStatementModel, indexToFind);
        }
    }

    private void copyStatementModel(StatementModel statementModel, StatementModel tempStatementModel) {
        tempStatementModel.setIndex(statementModel.getIndex());
        tempStatementModel.setStatement(statementModel.getStatement());
        tempStatementModel.setStartIndex(statementModel.getStartIndex());
        tempStatementModel.setEndIndex(statementModel.getEndIndex());
    }

    private Integer searchIndexOfStatements(List<StatementModel> remainingStatements,
                                            StatementModel statementModel) {
        Integer index = INVALID_INDEX;

        for (int i = FIRST_INDEX; i < remainingStatements.size(); i++) {
            StatementModel remainingStatementModel = remainingStatements.get(i);

            if (isStatementEquals(remainingStatementModel, statementModel)) {
                index = i;
                break;
            }
        }

        return index;
    }

    private Boolean isStatementEquals(StatementModel statementModel,
                                      StatementModel nextStatementModel) {
        return statementModel.getIndex().equals(nextStatementModel.getIndex()) &&
                statementModel.getStatement().equals(nextStatementModel.getStatement()) &&
                statementModel.getStartIndex().equals(nextStatementModel.getStartIndex()) &&
                statementModel.getEndIndex().equals(nextStatementModel.getEndIndex());
    }

    private Boolean isInsideBlock(StatementModel statementModel, Integer indexFilter) {
        return statementModel instanceof BlockModel &&
                ((BlockModel) statementModel).getEndOfBlockStatement().getIndex() > indexFilter;
    }

    private Boolean isSearchInRange(StatementModel statementModel, Integer maxIndexFilter,
                                    GetFilteredRawVariablesVA getFilteredRawVariablesVA) {
        return statementModel.getIndex() > getFilteredRawVariablesVA.getIndexFilter() &&
                statementModel.getIndex() <= maxIndexFilter;
    }

    private Boolean isContainsAssignment(MethodModel methodModel, Candidate candidate,
                                         List<String> rawVariables) {
        return isAssignment(methodModel, candidate, rawVariables) ||
                isIncrementOrDecrement(methodModel, candidate, rawVariables);
    }

    private Boolean isAssignment(MethodModel methodModel, Candidate candidate,
                                 List<String> rawVariables) {
        Boolean isAssigment = Boolean.FALSE;

        if (rawVariables.size() > SINGLE_LIST_SIZE) {
            isAssigment = searchAssigment(methodModel, candidate, rawVariables);
        }

        return isAssigment;
    }

    private Boolean searchAssigment(MethodModel methodModel, Candidate candidate,
                                    List<String> rawVariables) {
        Boolean isAssigment = Boolean.FALSE;

        String firstVariable = rawVariables.get(FIRST_INDEX);
        String secondVariable = rawVariables.get(SECOND_INDEX);

        if (isContainsAssignmentOperators(secondVariable)) {
            Integer variableStatementIndex = getRawVariablesStatementIndex(candidate, rawVariables);

            Boolean isLocalVariable = isLocalVariable(candidate.getLocalVariables(),
                    firstVariable, variableStatementIndex);
            Boolean isMethodLocalVariable = isLocalVariable(methodModel.getLocalVariables(),
                    firstVariable, variableStatementIndex);

            isAssigment = !isLocalVariable && isMethodLocalVariable;
        }

        return isAssigment;
    }

    private Integer getRawVariablesStatementIndex(Candidate candidate, List<String> rawVariables) {
        Integer variableStatementIndex = candidate.getRawVariables()
                .indexOf(rawVariables);

        return getRealStatementIndex(candidate, variableStatementIndex);
    }

    private Integer getRealStatementIndex(Candidate candidate, Integer variableStatementIndex) {
        ReadStatementIndexVA readStatementIndexVA = ReadStatementIndexVA.builder()
                .variableStatementIndex(variableStatementIndex)
                .build();
        BlockModel candidateBlock = getMethodBlockStatement(candidate.getStatements());

        readBlockStatementIndex(candidateBlock, readStatementIndexVA);

        return readStatementIndexVA.getRealStatementIndex();
    }

    private void readBlockStatementIndex(BlockModel blockModel,
                                         ReadStatementIndexVA readStatementIndexVA) {
        blockModel.getStatements()
                .forEach(statementModel -> readStatementIndex(statementModel, readStatementIndexVA));
    }

    private void readStatementIndex(StatementModel statementModel,
                                    ReadStatementIndexVA readStatementIndexVA) {
        Integer statementCount = readStatementIndexVA.getStatementCount()
                .getAndIncrement();

        if (statementCount.equals(readStatementIndexVA.getVariableStatementIndex())) {
            readStatementIndexVA.setRealStatementIndex(statementModel.getIndex());
        }

        if (statementModel instanceof BlockModel) {
            readBlockStatementIndex((BlockModel) statementModel, readStatementIndexVA);
        }
    }

    private Boolean isContainsAssignmentOperators(String variable) {
        return variable.contains(ASSIGMENT_OPERATOR) &&
                (!variable.contains(EQUAL_TO_OPERATOR) || !variable.contains(NOT_EQUAL_TO_OPERATOR));
    }

    private Boolean isLocalVariable(List<VariablePropertyModel> localVariables,
                                    String variable, Integer maxIndexStatement) {
        return localVariables.stream()
                .filter(variablePropertyModel ->
                        isValidLocalVariableSearch(variablePropertyModel, maxIndexStatement))
                .anyMatch(localVariable ->
                        isLocalVariableNameEquals(localVariable, variable));
    }

    private Boolean isValidLocalVariableSearch(VariablePropertyModel variablePropertyModel,
                                               Integer maxIndexStatement) {
        return variablePropertyModel.getStatementIndex() < maxIndexStatement;
    }

    private Boolean isLocalVariableNameEquals(PropertyModel localVariable, String variable) {
        return localVariable.getName()
                .equals(variable);
    }

    private Boolean isIncrementOrDecrement(MethodModel methodModel, Candidate candidate,
                                           List<String> rawVariables) {
        Boolean isIncrementOrDecrement = Boolean.FALSE;

        for (Integer index = FIRST_INDEX; index < rawVariables.size(); index++) {
            String variable = rawVariables.get(index);

            if (containsIncrementOrDecrement(variable)) {
                Integer variableStatementIndex = getRawVariablesStatementIndex(candidate, rawVariables);
                variable = getVariableWhichUses(rawVariables, index);

                Boolean isLocalVariable = isLocalVariable(candidate.getLocalVariables(),
                        variable, variableStatementIndex);
                Boolean isMethodLocalVariable = isLocalVariable(methodModel.getLocalVariables(),
                        variable, variableStatementIndex);

                isIncrementOrDecrement = !isLocalVariable && isMethodLocalVariable;
            }
        }

        return isIncrementOrDecrement;
    }

    private String getVariableWhichUses(List<String> rawVariables, Integer index) {
        Integer nextIndex = getNextIndex(rawVariables, index);
        String nextVariable = rawVariables.get(nextIndex);

        if (!isNotOperators(nextVariable)) {
            nextIndex = index - SECOND_INDEX;
            nextVariable = rawVariables.get(nextIndex);
        }

        return nextVariable;
    }

    private Boolean isNotOperators(String variable) {
        return !variable.matches(VariableHelper.OPERATORS_CHARACTERS_REGEX);
    }

    private Integer getNextIndex(List<String> rawVariables, Integer index) {
        Integer nextIndex = index + SECOND_INDEX;
        Integer maxIndex = rawVariables.size() - SECOND_INDEX;

        if (index.equals(FIRST_INDEX)) {
            nextIndex = SECOND_INDEX;
        }

        if (index.equals(maxIndex)) {
            nextIndex = maxIndex - SECOND_INDEX;
        }

        return nextIndex;
    }

    private Boolean containsIncrementOrDecrement(String variable) {
        return variable.equals(INCREMENT_OPERATOR) || variable.equals(DECREMENT_OPERATOR);
    }

    private Boolean isBlockStatementComplete(BlockModel methodBlock, Candidate candidate) {
        List<StatementModel> candidateStatements = candidate.getStatements();
        IsBlockCompleteVA isBlockCompleteVA = IsBlockCompleteVA.builder()
                .statements(candidateStatements)
                .methodBlock(methodBlock)
                .isBlockComplete(new AtomicBoolean(Boolean.TRUE))
                .build();

        isBlockComplete(isBlockCompleteVA);

        return isBlockCompleteVA.getIsBlockComplete().get();
    }

    private void isBlockComplete(IsBlockCompleteVA isBlockCompleteVA) {
        List<StatementModel> candidateStatements = isBlockCompleteVA.getStatements();

        for (int index = FIRST_INDEX; index < candidateStatements.size(); index++) {
            if (!isBlockCompleteVA.getIsBlockComplete().get()) {
                break;
            }

            doIsBlockComplete(candidateStatements, index, isBlockCompleteVA);
        }
    }

    private void doIsBlockComplete(List<StatementModel> candidateStatements,
                                   Integer index, IsBlockCompleteVA isBlockCompleteVA) {
        StatementModel beforeStatementModel = getStatementModel(candidateStatements, index - SECOND_INDEX);
        StatementModel statementModel = getStatementModel(candidateStatements, index);
        StatementModel nextStatementModel = getStatementModel(candidateStatements, index + SECOND_INDEX);

        isBlockCompleteVA.setBeforeStatementModel(beforeStatementModel);
        isBlockCompleteVA.setStatementModel(statementModel);
        isBlockCompleteVA.setNextStatementModel(nextStatementModel);
        checkBlockCompletion(isBlockCompleteVA);
    }

    private StatementModel getStatementModel(List<StatementModel> candidateStatements,
                                             Integer index) {
        try {
            return candidateStatements.get(index);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private void checkBlockCompletion(IsBlockCompleteVA isBlockCompleteVA) {
        StatementModel statementModel = isBlockCompleteVA.getStatementModel();

        if (statementModel instanceof BlockModel) {
            if (isBlockNotComplete(statementModel, isBlockCompleteVA)) {
                flagCandidateToFalse(isBlockCompleteVA);
            }

            isBlockCompleteVA.setStatements(((BlockModel) statementModel).getStatements());
            isBlockComplete(isBlockCompleteVA);
        } else if (isDoWhileNeedOpeningStatement(statementModel, isBlockCompleteVA)) {
            flagCandidateToFalse(isBlockCompleteVA);
        }
    }

    private Boolean isBlockNotComplete(StatementModel statementModel,
                                       IsBlockCompleteVA isBlockCompleteVA) {
        return isBlockNeedOpeningStatement(statementModel, isBlockCompleteVA) ||
                isBlockNeedClosingStatement(statementModel, isBlockCompleteVA) ||
                isIfMissElseBlock(statementModel, isBlockCompleteVA);
    }

    private Boolean isBlockNeedOpeningStatement(StatementModel statementModel,
                                                IsBlockCompleteVA isBlockCompleteVA) {
        return (statementModel.getStatement().startsWith(CATCH) || statementModel.getStatement().startsWith(FINALLY) ||
                statementModel.getStatement().startsWith(ELSE)) &&
                isBlockCompleteVA.getBeforeStatementModel() == null;
    }

    private Boolean isBlockNeedClosingStatement(StatementModel statementModel,
                                                IsBlockCompleteVA isBlockCompleteVA) {
        return (statementModel.getStatement().startsWith(TRY) || statementModel.getStatement().startsWith(DO)) &&
                isBlockCompleteVA.getNextStatementModel() == null;
    }

    private Boolean isIfMissElseBlock(StatementModel statementModel,
                                      IsBlockCompleteVA isBlockCompleteVA) {
        IsIfMissElseVA isIfMissElseVA = IsIfMissElseVA.builder()
                .statements(isBlockCompleteVA.getMethodBlock().getStatements())
                .ifStatementModel(statementModel)
                .nextIfStatementModel(isBlockCompleteVA.getNextStatementModel())
                .isIfMissElseBlock(new AtomicBoolean(Boolean.FALSE))
                .build();

        if (statementModel.getStatement().startsWith(IF)) {
            searchIfStatementLocation(isIfMissElseVA);
        }

        return isIfMissElseVA.getIsIfMissElseBlock().get();
    }

    private void searchIfStatementLocation(IsIfMissElseVA isIfMissElseVA) {
        List<StatementModel> statements = isIfMissElseVA.getStatements();

        if (statements.contains(isIfMissElseVA.getIfStatementModel())) {
            checkIfStatementElse(statements, isIfMissElseVA);
        } else {
            statements.forEach(statementModel ->
                    checkIfStatementLocation(statementModel, isIfMissElseVA));
        }
    }

    private void checkIfStatementElse(List<StatementModel> statements,
                                      IsIfMissElseVA isIfMissElseVA) {
        Integer indexOfIfStatement = statements.indexOf(isIfMissElseVA.getIfStatementModel());
        StatementModel nextStatementModel = getStatementModel(statements, indexOfIfStatement + SECOND_INDEX);

        if (isIfNeedChecked(nextStatementModel, isIfMissElseVA)) {
            isIfMissElseVA.getIsIfMissElseBlock()
                    .set(Boolean.TRUE);
        }
    }

    private Boolean isIfNeedChecked(StatementModel nextStatementModel, IsIfMissElseVA isIfMissElseVA) {
        return isIfMissElseVA.getNextIfStatementModel() == null &&
                nextStatementModel instanceof BlockModel &&
                nextStatementModel.getStatement().startsWith(ELSE);
    }

    private void checkIfStatementLocation(StatementModel statementModel,
                                          IsIfMissElseVA isIfMissElseVA) {
        if (isNeedToCheckInsideBlock(statementModel, isIfMissElseVA)) {
            isIfMissElseVA.setStatements(((BlockModel) statementModel).getStatements());
            searchIfStatementLocation(isIfMissElseVA);
        }
    }

    private Boolean isNeedToCheckInsideBlock(StatementModel statementModel,
                                             IsIfMissElseVA isIfMissElseVA) {
        return statementModel instanceof BlockModel &&
                !isIfMissElseVA.getIsIfMissElseBlock().get();
    }

    private void flagCandidateToFalse(IsBlockCompleteVA isBlockCompleteVA) {
        isBlockCompleteVA.getIsBlockComplete()
                .set(Boolean.FALSE);
    }

    private Boolean isDoWhileNeedOpeningStatement(StatementModel statementModel,
                                                  IsBlockCompleteVA isBlockCompleteVA) {
        return statementModel.getStatement().startsWith(WHILE) &&
                isBlockCompleteVA.getBeforeStatementModel() == null;
    }

    private Boolean isQualityValid(MethodModel methodModel, Candidate candidate) {
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = getRemainingBlockModel(methodBlock, candidateBlock);

        Integer candidateStatementCount = getStatementCount(candidateBlock);
        Integer remainingStatementCount = getStatementCount(remainingBlock);

        return (candidateStatementCount >= minCandidateStatements) &&
                (remainingStatementCount >= minCandidateStatements);
    }

    private BlockModel getRemainingBlockModel(BlockModel methodBlock, BlockModel candidateBlock) {
        BlockModel remainingBlock = createCopyBlockMethod(methodBlock);
        removeCandidates(remainingBlock, candidateBlock.getStatements());

        return remainingBlock;
    }

    private BlockModel createCopyBlockMethod(BlockModel blockMethod) {
        BlockModel copyBlockMethod = BlockModel.blockBuilder()
                .statements(new ArrayList<>(blockMethod.getStatements()))
                .endOfBlockStatement(blockMethod.getEndOfBlockStatement())
                .build();
        copyBlockMethod.setStatement(blockMethod.getStatement());
        copyBlockMethod.setIndex(blockMethod.getIndex());
        copyBlockMethod.setStartIndex(blockMethod.getStartIndex());
        copyBlockMethod.setEndIndex(blockMethod.getEndIndex());
        copyListStatements(copyBlockMethod);

        return copyBlockMethod;
    }

    private void copyListStatements(BlockModel blockMethod) {
        int size = blockMethod.getStatements().size();

        for (int index = FIRST_INDEX; index < size; index++) {
            replaceBlockMethod(blockMethod, index);
        }
    }

    private void replaceBlockMethod(BlockModel blockMethod, Integer index) {
        StatementModel statement = blockMethod.getStatements().get(index);

        if (statement instanceof BlockModel) {
            blockMethod.getStatements()
                    .set(index, createCopyBlockMethod(((BlockModel) statement)));
        }
    }

    private Integer getStatementCount(BlockModel blockMethod) {
        AtomicInteger count = new AtomicInteger();
        countBlockStatement(blockMethod, count);

        return count.get();
    }

    private void countBlockStatement(BlockModel blockMethod, AtomicInteger count) {
        blockMethod.getStatements()
                .forEach(statement -> countStatement(statement, count));
    }

    private void countStatement(StatementModel statement, AtomicInteger count) {
        count.getAndIncrement();

        if (statement instanceof BlockModel) {
            countBlockStatement((BlockModel) statement, count);
        }
    }

    private void removeCandidates(BlockModel blockMethod, List<StatementModel> candidates) {
        List<StatementModel> statements = blockMethod.getStatements();

        if (statements.containsAll(candidates)) {
            statements.removeAll(candidates);
        } else {
            statements.forEach(statement -> searchBlockCandidatesToRemove(statement, candidates));
        }
    }

    private void searchBlockCandidatesToRemove(StatementModel statement, List<StatementModel> candidates) {
        if (statement instanceof BlockModel) {
            removeCandidates((BlockModel) statement, candidates);
        }
    }

    private void scoringCandidates(MethodModel methodModel, List<Candidate> candidates) {
        candidates.forEach(
                candidate -> scoringCandidate(methodModel, candidate));
    }

    private void scoringCandidate(MethodModel methodModel, Candidate candidate) {
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = getRemainingBlockModel(methodBlock, candidateBlock);

        candidate.setLengthScore(calculateStatementLengthScore(candidateBlock, remainingBlock));
        candidate.setNestingDepthScore(calculateNestingDepthScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setNestingAreaScore(calculateNestingAreaScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setParameterScore(calculateParameterScore(methodModel, candidate));

        calculateTotalScore(candidate);
    }

    private Double calculateStatementLengthScore(BlockModel candidateBlock, BlockModel remainingBlock) {
        Integer candidateStatementCount = getStatementCount(candidateBlock);
        Integer remainingStatementCount = getStatementCount(remainingBlock);
        Integer minStatementCount = Math.min(candidateStatementCount, remainingStatementCount);

        return Math.min(statementLengthScoreConstant * minStatementCount, statementLengthScoreMax);
    }

    private Double calculateNestingDepthScore(BlockModel methodBlock, BlockModel candidateBlock,
                                              BlockModel remainingBlock) {
        Integer methodMaxNestingDepth = getNestingDepth(methodBlock);
        Integer candidateMaxNestingDepth = getNestingDepth(candidateBlock);
        Integer remainingMaxNestingDepth = getNestingDepth(remainingBlock);

        int remainingMaxNestingDepthDeviation = methodMaxNestingDepth - remainingMaxNestingDepth;
        int candidateMaxNestingDepthDeviation = methodMaxNestingDepth - candidateMaxNestingDepth;

        return Double.min(remainingMaxNestingDepthDeviation, candidateMaxNestingDepthDeviation);
    }

    private Integer getNestingDepth(BlockModel blockMethod) {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();

        searchMaxNestingDepth(blockMethod, count, max);

        return max.decrementAndGet();
    }

    private void searchMaxNestingDepth(BlockModel blockMethod, AtomicInteger count,
                                       AtomicInteger max) {
        count.getAndIncrement();

        blockMethod.getStatements()
                .forEach(statement -> countNestingDepth(statement, count, max));

        saveMaxNestingDepth(count, max);
    }

    private void countNestingDepth(StatementModel statement, AtomicInteger count,
                                   AtomicInteger max) {
        if (statement instanceof BlockModel) {
            searchMaxNestingDepth((BlockModel) statement, count, max);
        }
    }

    private void saveMaxNestingDepth(AtomicInteger count, AtomicInteger max) {
        int newMax = count.getAndDecrement();

        if (newMax > max.get()) {
            max.set(newMax);
        }
    }

    private Double calculateNestingAreaScore(BlockModel methodBlock, BlockModel candidateBlock,
                                             BlockModel remainingBlock) {
        Integer methodMaxNestingDepth = getNestingDepth(methodBlock);

        Integer methodNestingArea = getNestingArea(methodBlock);
        Integer candidateNestingArea = getNestingArea(candidateBlock);
        Integer remainingNestingArea = getNestingArea(remainingBlock);

        int candidateNestingAreaDeviation = methodNestingArea - candidateNestingArea;
        int remainingNestingAreaDeviation = methodNestingArea - remainingNestingArea;

        Double nestingAreaReduction = Double.min(candidateNestingAreaDeviation, remainingNestingAreaDeviation);

        return nestingAreaScoreConstant * methodMaxNestingDepth * (nestingAreaReduction / methodNestingArea);
    }

    private Integer getNestingArea(BlockModel blockMethod) {
        AtomicInteger count = new AtomicInteger();
        searchNestingArea(blockMethod, count);

        return count.get();
    }

    private void searchNestingArea(BlockModel blockMethod, AtomicInteger count) {
        blockMethod.getStatements()
                .forEach(statement -> countNestingArea(statement, count));
    }

    private void countNestingArea(StatementModel statement, AtomicInteger count) {
        AtomicInteger countNestingDepth = new AtomicInteger();
        AtomicInteger maxNestingDepth = new AtomicInteger();

        if (statement instanceof BlockModel) {
            searchMaxNestingDepth((BlockModel) statement, countNestingDepth, maxNestingDepth);
        }

        count.set(count.get() + maxNestingDepth.get());
    }

    private Double calculateParameterScore(MethodModel methodModel, Candidate candidate) {
        candidate.setParameters(getCandidateParameters(methodModel, candidate));

        Integer parameterIn = candidate.getParameters().size();
        Integer parameterOut = getReturnTypeScore(candidate);

        return parameterScoreMax - parameterIn - parameterOut;
    }

    private List<PropertyModel> getCandidateParameters(MethodModel methodModel, Candidate candidate) {
        Stream<PropertyModel> parametersFromMethodParameters = getParameterCandidates(
                methodModel.getParameters(), candidate.getGlobalVariables());

        Stream<PropertyModel> parametersFromLocalVariable = getParameterCandidates(
                methodModel.getLocalVariables(), candidate.getGlobalVariables());

        return Stream.concat(parametersFromMethodParameters, parametersFromLocalVariable)
                .collect(Collectors.toList());
    }

    private Stream<PropertyModel> getParameterCandidates(List<? extends PropertyModel> methodVariables,
                                                         List<String> candidateGlobalVariables) {
        return candidateGlobalVariables.stream()
                .map(variable -> getMethodVariable(methodVariables, variable))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<? extends PropertyModel> getMethodVariable(List<? extends PropertyModel> variables,
                                                                String variable) {
        return variables.stream()
                .filter(propertyModel -> isLocalVariableNameEquals(propertyModel, variable))
                .findFirst();
    }

    private Integer getReturnTypeScore(Candidate candidate) {
        if (candidate.getReturnTypeStatementRawVariableIndex() != null) {
            return RETURN_TYPE_VALID_SCORE;
        } else {
            return RETURN_TYPE_INVALID_SCORE;
        }
    }

    private void calculateTotalScore(Candidate candidate) {
        candidate.setTotalScore(candidate.getLengthScore() + candidate.getNestingDepthScore() +
                candidate.getNestingAreaScore() + candidate.getParameterScore());
    }

    private void pruningCandidates(List<Candidate> candidates) {
        List<Integer> indexToRemoves = new ArrayList<>();

        for (int index = FIRST_INDEX; index < candidates.size(); index++) {
            searchCandidatesThatContainsCandidate(index, candidates, indexToRemoves);
        }

        removeCandidates(indexToRemoves, candidates);
    }

    private void searchCandidatesThatContainsCandidate(Integer index, List<Candidate> candidates,
                                                       List<Integer> indexToRemoves) {
        for (int nextIndex = FIRST_INDEX; nextIndex < candidates.size(); nextIndex++) {
            Candidate candidate = candidates.get(nextIndex);
            Candidate candidateToSearch = candidates.get(index);

            if (index.equals(nextIndex)) {
                continue;
            }

            if (isContainsCandidate(candidate, candidateToSearch)) {
                indexToRemoves.add(index);
                break;
            }
        }
    }

    private Boolean isContainsCandidate(Candidate candidate, Candidate candidateToSearch) {
        AtomicBoolean isContains = new AtomicBoolean();
        searchCandidates(candidate.getStatements(), candidateToSearch.getStatements(), isContains);

        return isContains.get();
    }

    private void searchCandidates(List<StatementModel> statements,
                                  List<StatementModel> statementsToSearch,
                                  AtomicBoolean isContains) {
        if (statements.containsAll(statementsToSearch)) {
            isContains.set(!isContains.get());
        } else {
            statements.forEach(statementModel ->
                    searchBlockCandidates(statementModel, statementsToSearch, isContains));
        }
    }

    private void searchBlockCandidates(StatementModel statementModel,
                                       List<StatementModel> statementsToSearch,
                                       AtomicBoolean isContains) {
        if (statementModel instanceof BlockModel) {
            searchCandidates(((BlockModel) statementModel).getStatements(), statementsToSearch,
                    isContains);
        }
    }

    private void removeCandidates(List<Integer> indexToRemoves,
                                  List<Candidate> candidates) {
        Integer firstIndex = indexToRemoves.size() - SECOND_INDEX;

        for (int index = firstIndex; index >= FIRST_INDEX; index--) {
            Integer indexToRemove = indexToRemoves.get(index);
            candidates.remove(indexToRemove.intValue());
        }
    }

    private void sortingCandidates(List<Candidate> candidates) {
        candidates.sort(Comparator.comparing(Candidate::getTotalScore)
                .reversed());
    }

    private MethodModel createMethodModelFromCandidate(MethodModel methodModel,
                                                       Candidate candidate) {
        String extractedMethodName = methodModel.getName() + EXTRACTED_METHOD_NAME_SUFFIX;

        return MethodModel.builder()
                .keywords(createKeywords(methodModel.getKeywords()))
                .returnType(createReturnType(candidate))
                .name(extractedMethodName)
                .parameters(candidate.getParameters())
                .globalVariables(candidate.getGlobalVariables())
                .localVariables(candidate.getLocalVariables())
                .exceptions(createExceptions(methodModel, candidate))
                .body(createMethodBody(candidate.getStatements()))
                .statements(candidate.getStatements())
                .build();
    }

    private List<String> createKeywords(List<String> methodKeywords) {
        List<String> keywords = methodKeywords.stream()
                .filter(this::isNotAnnotationAndModifier)
                .collect(Collectors.toList());
        keywords.add(FIRST_INDEX, METHOD_PRIVATE_MODIFIER);

        return keywords;
    }

    private Boolean isNotAnnotationAndModifier(String keywords) {
        return !keywords.startsWith(AT) && !REMOVED_MODIFIERS.contains(keywords);
    }

    private String createReturnType(Candidate candidate) {
        String returnType = VOID_RETURN_TYPE;

        if (candidate.getReturnType() != null) {
            returnType = candidate.getReturnType().getType();
        }

        return returnType;
    }

    private String createMethodBody(List<StatementModel> statements) {
        StringBuilder body = new StringBuilder();
        AtomicInteger depth = new AtomicInteger();

        buildMethodBody(statements, depth, body);

        return body.toString().trim();
    }

    private void buildMethodBody(List<StatementModel> statements,
                                 AtomicInteger depth, StringBuilder body) {
        depth.incrementAndGet();

        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            appendStatementToBody(statements.get(index), depth, body);
        }

        depth.decrementAndGet();
    }

    private void appendStatementToBody(StatementModel statementModel,
                                       AtomicInteger depth, StringBuilder body) {
        String statement = createStatementToAppend(statementModel, depth);
        body.append(statement);

        if (statementModel instanceof BlockModel) {
            appendBlockStatementToBody(statementModel, depth, body);
        }
    }

    private String createStatementToAppend(StatementModel statementModel,
                                           AtomicInteger depth) {
        StringBuilder statement = new StringBuilder();
        appendTabsToStatement(statement, depth);

        statement.append(statementModel.getStatement());
        statement.append(NEW_LINE);

        return statement.toString();
    }

    private void appendTabsToStatement(StringBuilder statement, AtomicInteger depth) {
        for (int i = FIRST_INDEX; i <= depth.get(); i++) {
            statement.append(TAB);
        }
    }

    private void appendBlockStatementToBody(StatementModel statementModel,
                                            AtomicInteger depth, StringBuilder body) {
        BlockModel blockModel = (BlockModel) statementModel;

        buildMethodBody(blockModel.getStatements(), depth, body);
        appendEndOfBlockStatement(blockModel, depth, body);
    }

    private void appendEndOfBlockStatement(BlockModel blockModel, AtomicInteger depth,
                                           StringBuilder body) {
        String statement = createStatementToAppend(blockModel.getEndOfBlockStatement(), depth);
        body.append(statement);
    }

    private List<String> createExceptions(MethodModel methodModel, Candidate candidate) {
        Integer firstCandidateStatementIndex = candidate.getStatements()
                .get(FIRST_INDEX)
                .getIndex();
        List<StatementModel> methodStatements = mergeAllStatements(methodModel.getStatements());
        StatementModel statementModel = searchStatementByIndex(
                methodModel.getStatements(), firstCandidateStatementIndex);

        return searchException(methodStatements, methodModel, statementModel);
    }

    private List<String> searchException(List<StatementModel> methodStatements,
                                         MethodModel methodModel, StatementModel statementModel) {
        List<String> exceptions = new ArrayList<>(methodModel.getExceptions());
        Integer statementIndex = searchIndexOfStatements(methodStatements, statementModel);

        for (int index = statementIndex; index >= FIRST_INDEX; index--) {
            StatementModel methodStatementModel = methodStatements.get(index);

            if (checkContainCatchBlock(methodModel, methodStatementModel, exceptions)) {
                break;
            }
        }

        return exceptions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private Boolean checkContainCatchBlock(MethodModel methodModel, StatementModel statementModel,
                                           List<String> exceptions) {
        if (statementModel instanceof BlockModel) {
            StatementModel endBlockStatement = searchStatementByIndex(methodModel.getStatements(),
                    ((BlockModel) statementModel).getEndOfBlockStatement().getIndex());
            Boolean isCatch = endBlockStatement.getStatement().startsWith(CATCH);

            if (isCatch) {
                saveException(methodModel, endBlockStatement, exceptions);
            }

            return isCatch;
        } else {
            return Boolean.FALSE;
        }
    }

    private void saveException(MethodModel methodModel, StatementModel endBlockStatement,
                               List<String> exceptions) {
        methodModel.getLocalVariables()
                .stream()
                .filter(variablePropertyModel ->
                        variablePropertyModel.getStatementIndex().equals(endBlockStatement.getIndex()))
                .forEach(variablePropertyModel ->
                        exceptions.add(variablePropertyModel.getType()));
    }

    private MethodModel createRemainingMethodModel(MethodModel methodModel,
                                                   MethodModel extractedMethodModel) {
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = getMethodBlockStatement(extractedMethodModel.getStatements());
        BlockModel remainingBlock = getRemainingBlockModel(methodBlock, candidateBlock);

        addCallExtractedMethodStatement(remainingBlock.getStatements(), extractedMethodModel);

        return MethodModel.builder()
                .keywords(methodModel.getKeywords())
                .returnType(methodModel.getReturnType())
                .name(methodModel.getName())
                .parameters(methodModel.getParameters())
                .exceptions(methodModel.getExceptions())
                .body(createMethodBody(remainingBlock.getStatements()))
                .statements(remainingBlock.getStatements())
                .build();
    }

    private void addCallExtractedMethodStatement(List<StatementModel> statements,
                                                 MethodModel extractedMethodModel) {
        Integer extractedStatementIndex = extractedMethodModel.getStatements()
                .get(FIRST_INDEX)
                .getIndex();

        AddCallExtractedMethodVA addCallExtractedMethodVA = AddCallExtractedMethodVA.builder()
                .statements(statements)
                .extractedMethodModel(extractedMethodModel)
                .extractedStatementIndex(extractedStatementIndex)
                .extractedStatements(statements)
                .statementArrayIndex(INVALID_INDEX)
                .build();

        if (extractedStatementIndex > FIRST_INDEX) {
            addCallExtractedMethodVA.setExtractedStatementIndex(extractedStatementIndex - SECOND_INDEX);
            doAddCallExtractedMethodStatement(addCallExtractedMethodVA);
        }

        writeCallExtractedMethodStatement(addCallExtractedMethodVA);
    }

    private void doAddCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        List<StatementModel> statements = addCallExtractedMethodVA.getStatements();

        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            checkStatementModel(index, statements, addCallExtractedMethodVA);
        }
    }

    private void checkStatementModel(Integer index, List<StatementModel> statements,
                                     AddCallExtractedMethodVA addCallExtractedMethodVA) {
        StatementModel statementModel = statements.get(index);

        if (addCallExtractedMethodVA.getExtractedStatementIndex()
                .equals(statementModel.getIndex())) {
            addCallExtractedMethodVA.setExtractedStatements(statements);
            addCallExtractedMethodVA.setStatementArrayIndex(index);
        }

        if (statementModel instanceof BlockModel) {
            addCallExtractedMethodVA.setStatements(((BlockModel) statementModel).getStatements());
            doAddCallExtractedMethodStatement(addCallExtractedMethodVA);
        }
    }

    private void writeCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        Integer callExtractedMethodIndex = addCallExtractedMethodVA.getStatementArrayIndex() + SECOND_INDEX;
        StatementModel statementModel = null;
        StatementModel callExtractedMethodStatementModel = createCallExtractedMethodStatement(
                addCallExtractedMethodVA);

        if (!callExtractedMethodIndex.equals(FIRST_INDEX)) {
            statementModel = addCallExtractedMethodVA.getExtractedStatements()
                    .get(addCallExtractedMethodVA.getStatementArrayIndex());
        }

        if (statementModel instanceof BlockModel) {
            ((BlockModel) statementModel).getStatements()
                    .add(callExtractedMethodStatementModel);
        } else {
            addCallExtractedMethodVA.getExtractedStatements()
                    .add(callExtractedMethodIndex, callExtractedMethodStatementModel);
        }
    }

    private StatementModel createCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        String statement = addCallExtractedMethodVA.getExtractedMethodModel().getName() +
                createMethodCallParameters(addCallExtractedMethodVA.getExtractedMethodModel()
                        .getParameters());

        return StatementModel.statementBuilder()
                .index(addCallExtractedMethodVA.getExtractedStatementIndex())
                .statement(statement)
                .build();
    }

    private String createMethodCallParameters(List<PropertyModel> parameters) {
        Integer maxSize = parameters.size() - SECOND_INDEX;
        StringBuilder parameter = new StringBuilder(METHOD_PARAMETER_PREFIX);

        for (Integer index = FIRST_INDEX; index < parameters.size(); index++) {
            parameter.append(parameters.get(index).getName());

            if (!index.equals(maxSize)) {
                parameter.append(COMMA);
            }
        }

        parameter.append(METHOD_PARAMETER_SUFFIX);

        return parameter.toString();
    }

    private Boolean replaceFile(String path, MethodModel methodModel, Candidate bestCandidate) {
        MethodModel candidateMethodModel = createMethodModelFromCandidate(methodModel, bestCandidate);
        MethodModel remainingMethodModel = createRemainingMethodModel(methodModel, candidateMethodModel);

        String target = methodModelHelper.createMethodRegex(methodModel);
        String replacement = createReplacementString(candidateMethodModel, remainingMethodModel);

        ReplaceFileVA replaceFileVA = ReplaceFileVA.builder()
                .filePath(path)
                .target(target)
                .replacement(replacement)
                .build();

        return replaceFileHelper.replaceFile(replaceFileVA);
    }

    private String createReplacementString(MethodModel candidateMethodModel,
                                           MethodModel remainingMethodModel) {
        String candidateMethod = methodModelHelper.createMethod(candidateMethodModel);
        String remainingMethod = methodModelHelper.createMethod(remainingMethodModel);

        candidateMethod = normalizeMethodString(candidateMethod);
        remainingMethod = normalizeMethodString(remainingMethod);

        return buildReplacementString(candidateMethod, remainingMethod);
    }

    private String buildReplacementString(String candidateMethod,
                                          String remainingMethod) {
        String replacementString = remainingMethod;

        replacementString += NEW_LINE;
        replacementString += NEW_LINE;
        replacementString += TAB;
        replacementString += candidateMethod;

        return replacementString;
    }

    private String normalizeMethodString(String method) {
        List<String> statements = new ArrayList<>(Arrays.asList(method.split(NEW_LINE)));
        Integer lastIndex = statements.size() - SECOND_INDEX;

        statements.set(SECOND_INDEX, TAB + statements.get(SECOND_INDEX));
        statements.set(lastIndex, TAB + statements.get(lastIndex));

        return String.join(NEW_LINE, statements);
    }
}
