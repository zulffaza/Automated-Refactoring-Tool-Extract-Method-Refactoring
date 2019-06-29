package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.GetFilteredRawVariablesVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.IsBlockCompleteVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.IsIfMissElseVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.ReadStatementIndexVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateVariableAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper.CandidateHelper;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 25 June 2019
 */

@Service
public class CandidateAnalysisImpl implements CandidateAnalysis {

    @Autowired
    private CandidateVariableAnalysis candidateVariableAnalysis;

    @Value("${threshold.min.candidate.statements}")
    private Integer minCandidateStatements;

    private static final String ASSIGMENT_OPERATOR = "=";
    private static final String EQUAL_TO_OPERATOR = "==";
    private static final String NOT_EQUAL_TO_OPERATOR = "!=";
    private static final String INCREMENT_OPERATOR = "++";
    private static final String DECREMENT_OPERATOR = "--";
    private static final String IF_REGEX = "^(?:if)+(?:\\s)*(?:\\()+(?:\\s)*";
    private static final String ELSE_REGEX = "^(?:else)+(?:\\s)*";
    private static final String TRY_REGEX = "^(?:try)+(?:\\s)*(?:[{(])+(?:\\s)*";
    private static final String CATCH_REGEX = "^(?:catch)+(?:\\s)*(?:\\()+(?:\\s)*";
    private static final String FINALLY_REGEX = "^(?:finally)+(?:\\s)*(?:\\{)+(?:\\s)*";
    private static final String DO_REGEX = "^(?:do)+(?:\\s)*(?:\\{)+(?:\\s)*";
    private static final String WHILE_REGEX = "^(?:while)+(?:\\s)*(?:\\()+(?:\\s)*";
    private static final String RETURN_REGEX = "^(?:return)+(?:\\s)*";
    private static final String SWITCH_REGEX = "^(?:switch)+(?:\\s)*(?:\\()+(?:\\s)*";

    private static final Integer SINGLE_LIST_SIZE = 1;
    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;

    @Override
    public List<Candidate> analysis(@NonNull MethodModel methodModel) {
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
        BlockModel methodBlock = CandidateHelper.getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, candidateBlock);

        List<List<String>> rawVariablesContainsAssigment = candidate.getRawVariables()
                .stream()
                .filter(rawVariables -> isContainsAssignment(methodModel, candidate, rawVariables))
                .collect(Collectors.toList());

        return isNoneCandidateVariableUseInRemaining(methodModel, candidate, remainingBlock) &&
                rawVariablesContainsAssigment.isEmpty() &&
                isBlockStatementComplete(methodBlock, candidate);
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

    private List<List<String>> getFilteredRawVariables(MethodModel methodModel,
                                                       Candidate candidate, BlockModel remainingBlock) {
        List<StatementModel> candidateStatements = CandidateHelper.mergeAllStatements(
                candidate.getStatements());
        List<List<String>> filteredRawVariables = new ArrayList<>();

        Integer indexFilter = candidateStatements.get(candidateStatements.size() - SECOND_INDEX).getIndex();

        if (isCandidateIsOutsideBlock(candidateStatements)) {
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

    private Boolean isCandidateIsOutsideBlock(List<StatementModel> candidateStatements) {
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
        List<StatementModel> remainingStatements = CandidateHelper.mergeAllStatements(
                remainingCandidate.getStatements());
        Integer maxIndexFilter = searchMaxIndexFilter(remainingStatements,
                getFilteredRawVariablesVA.getIndexFilter());

        candidateVariableAnalysis.analysis(getFilteredRawVariablesVA.getMethodModel(), remainingCandidate);

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
        StatementModel lastStatementModel = CandidateHelper.searchStatementByIndex(
                remainingStatements, indexFilter + SECOND_INDEX);
        int firstIndex = CandidateHelper.searchIndexOfStatements(
                remainingStatements, lastStatementModel) - SECOND_INDEX;
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

    private Boolean isInsideBlock(StatementModel statementModel, Integer indexFilter) {
        return statementModel instanceof BlockModel &&
                ((BlockModel) statementModel).getEndOfBlockStatement().getIndex() >= indexFilter;
    }

    private Optional<? extends PropertyModel> getMethodVariable(List<? extends PropertyModel> variables,
                                                                String variable) {
        return variables.stream()
                .filter(propertyModel ->
                        CandidateHelper.isLocalVariableNameEquals(propertyModel, variable))
                .findFirst();
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
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(
                candidate.getStatements());

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
                        CandidateHelper.isLocalVariableNameEquals(localVariable, variable));
    }

    private Boolean isValidLocalVariableSearch(VariablePropertyModel variablePropertyModel,
                                               Integer maxIndexStatement) {
        return variablePropertyModel.getStatementIndex() < maxIndexStatement;
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
        return !CandidateHelper.isMatchRegex(variable, VariableHelper.OPERATORS_CHARACTERS_REGEX);
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
        isParentNotSwitchStatement(isBlockCompleteVA);

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
        } else if (isAbnormalBlock(statementModel, isBlockCompleteVA)) {
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
        return (CandidateHelper.isMatchRegex(statementModel.getStatement(), CATCH_REGEX) ||
                CandidateHelper.isMatchRegex(statementModel.getStatement(), FINALLY_REGEX) ||
                CandidateHelper.isMatchRegex(statementModel.getStatement(), ELSE_REGEX)) &&
                isBlockCompleteVA.getBeforeStatementModel() == null;
    }

    private Boolean isBlockNeedClosingStatement(StatementModel statementModel,
                                                IsBlockCompleteVA isBlockCompleteVA) {
        return (CandidateHelper.isMatchRegex(statementModel.getStatement(), TRY_REGEX) ||
                CandidateHelper.isMatchRegex(statementModel.getStatement(), DO_REGEX)) &&
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

        if (CandidateHelper.isMatchRegex(statementModel.getStatement(), IF_REGEX)) {
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
                CandidateHelper.isMatchRegex(nextStatementModel.getStatement(), ELSE_REGEX);
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

    private Boolean isAbnormalBlock(StatementModel statementModel,
                                    IsBlockCompleteVA isBlockCompleteVA) {
        return isDoWhileNeedOpeningStatement(statementModel, isBlockCompleteVA) ||
                CandidateHelper.isMatchRegex(statementModel.getStatement(), RETURN_REGEX);
    }

    private Boolean isDoWhileNeedOpeningStatement(StatementModel statementModel,
                                                  IsBlockCompleteVA isBlockCompleteVA) {
        return CandidateHelper.isMatchRegex(statementModel.getStatement(), WHILE_REGEX) &&
                isBlockCompleteVA.getBeforeStatementModel() == null;
    }

    private void isParentNotSwitchStatement(IsBlockCompleteVA isBlockCompleteVA) {
        Integer firstCandidateIndex = isBlockCompleteVA.getStatements()
                .get(FIRST_INDEX)
                .getIndex();
        StatementModel statementModel = CandidateHelper.searchStatementByIndex(
                isBlockCompleteVA.getMethodBlock().getStatements(), firstCandidateIndex);
        List<StatementModel> methodStatements = CandidateHelper.mergeAllStatements(
                isBlockCompleteVA.getMethodBlock().getStatements());

        searchParentSwitchStatement(statementModel, methodStatements, isBlockCompleteVA);
    }

    private void searchParentSwitchStatement(StatementModel statementModel,
                                             List<StatementModel> methodStatements,
                                             IsBlockCompleteVA isBlockCompleteVA) {
        Integer firstCandidateArrayIndex = CandidateHelper.searchIndexOfStatements(
                methodStatements, statementModel);

        for (int index = firstCandidateArrayIndex; index >= FIRST_INDEX; index--) {
            StatementModel parentStatementModel = methodStatements.get(index);

            if (isParentSwitchBlock(parentStatementModel, statementModel)) {
                flagCandidateToFalse(isBlockCompleteVA);
                break;
            }
        }
    }

    private Boolean isParentSwitchBlock(StatementModel parentStatementModel, StatementModel statementModel) {
        return parentStatementModel instanceof BlockModel &&
                CandidateHelper.isMatchRegex(parentStatementModel.getStatement(), CATCH_REGEX) &&
                ((BlockModel) parentStatementModel).getEndOfBlockStatement().getIndex() >
                        statementModel.getIndex();
    }

    private Boolean isQualityValid(MethodModel methodModel, Candidate candidate) {
        BlockModel methodBlock = CandidateHelper.getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, candidateBlock);

        Integer candidateStatementCount = CandidateHelper.getStatementCount(candidateBlock);
        Integer remainingStatementCount = CandidateHelper.getStatementCount(remainingBlock);

        return (candidateStatementCount >= minCandidateStatements) &&
                (remainingStatementCount >= minCandidateStatements);
    }
}
