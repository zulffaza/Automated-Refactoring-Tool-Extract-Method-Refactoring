package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.ReadStatementIndexVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.SaveVariableVA;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private VariableHelper variableHelper;

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

    private static final String GLOBAL_VARIABLE_PREFIX = "this.";
    private static final String EMPTY_STRING = "";
    private static final String ASSIGMENT_OPERATOR = "=";
    private static final String EQUAL_TO_OPERATOR = "==";
    private static final String NOT_EQUAL_TO_OPERATOR = "!=";
    private static final String INCREMENT_OPERATOR = "++";
    private static final String DECREMENT_OPERATOR = "--";
    private static final String EXTRACTED_METHOD_KEYWORD = "private";
    private static final String EXTRACTED_METHOD_NAME_SUFFIX = "Extracted";
    private static final String NEW_LINE = "\n";
    private static final String TAB = "\t";
    private static final String VOID_RETURN_TYPE = "void";
    private static final String CATCH_BLOCK_REGEX = "}(?:\\s)*catch(?:\\s)*\\((?:\\s)*(?:[a-zA-Z0-9_$<>\\s|])*\\)(?:\\s)*{";
    private static final String METHOD_PARAMETER_PREFIX = "(";
    private static final String COMMA = ", ";
    private static final String METHOD_PARAMETER_SUFFIX = ");";

    private static final Integer SINGLE_LIST_SIZE = 1;
    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;
    private static final Integer EMPTY_SIZE = 0;
    private static final Integer RETURN_TYPE_VALID_SCORE = 1;
    private static final Integer RETURN_TYPE_INVALID_SCORE = 0;

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
        sortingCandidates(candidates);

        return candidates.get(FIRST_INDEX);
    }

    private List<Candidate> getCandidates(MethodModel methodModel) {
        List<List<StatementModel>> blocksMethod = getAllBlocksMethod(methodModel);

        return blocksMethod.parallelStream()
                .flatMap(this::searchCandidates)
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

    private Stream<Candidate> searchCandidates(List<StatementModel> statements) {
        List<Candidate> candidates = new ArrayList<>();

        for (int i = FIRST_INDEX; i < statements.size(); i++) {
            for (int j = i + SECOND_INDEX; j <= statements.size(); j++) {
                Candidate candidate = buildCandidate(statements.subList(i, j));
                candidates.add(candidate);
            }
        }

        return candidates.stream();
    }

    private Candidate buildCandidate(List<StatementModel> candidateStatements) {
        candidateStatements = new ArrayList<>(candidateStatements);

        Candidate candidate = Candidate.builder()
                .statements(candidateStatements)
                .build();

        analysisVariable(candidate);

        return candidate;
    }

    private void analysisVariable(@NonNull Candidate candidate) {
        candidate.getStatements()
                .forEach(statementModel -> analysisStatementVariable(statementModel, candidate));
    }

    private void analysisStatementVariable(StatementModel statementModel, Candidate candidate) {
        List<String> variables = variableHelper.readVariable(statementModel.getStatement());
        saveVariables(statementModel, variables, candidate);

        if (statementModel instanceof BlockModel) {
            analysisBlockStatementVariable(statementModel, candidate);
        }
    }

    private void saveVariables(StatementModel statementModel, List<String> variables,
                               Candidate candidate) {
        SaveVariableVA saveVariableVA = SaveVariableVA.builder()
                .statementModel(statementModel)
                .candidate(candidate)
                .build();

        variables.stream()
                .filter(this::isNotOperators)
                .forEach(variable -> saveVariable(variable, saveVariableVA));

        candidate.getRawVariables()
                .add(variables);
    }

    private Boolean isNotOperators(String variable) {
        return !variable.matches(VariableHelper.OPERATORS_CHARACTERS_REGEX);
    }

    private void saveVariable(String variable, SaveVariableVA saveVariableVA) {
        if (isPropertyType(variable)) {
            savePropertyType(variable, saveVariableVA);
        } else if (checkGlobalVariable(variable)) {
            saveGlobalVariableWithPreProcessing(variable, saveVariableVA.getCandidate());
        } else {
            checkVariableDomain(variable, saveVariableVA);
        }
    }

    private Boolean isPropertyType(String variable) {
        return VariableHelper.PRIMITIVE_TYPES.contains(variable) ||
                variableHelper.isClassName(variable);
    }

    private void savePropertyType(String variable, SaveVariableVA saveVariableVA) {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(saveVariableVA.getStatementModel().getIndex())
                .build();

        variablePropertyModel.setType(variable);

        saveVariableVA.getCandidate()
                .getLocalVariables()
                .add(variablePropertyModel);

        saveVariableVA.getIsClass()
                .set(Boolean.TRUE);
    }

    private Boolean checkGlobalVariable(String variable) {
        return variable.startsWith(GLOBAL_VARIABLE_PREFIX);
    }

    private void saveGlobalVariableWithPreProcessing(String variable, Candidate candidate) {
        variable = variable.replace(GLOBAL_VARIABLE_PREFIX, EMPTY_STRING);
        saveGlobalVariable(variable, candidate);
    }

    private void saveGlobalVariable(String variable, Candidate candidate) {
        candidate.getGlobalVariables()
                .add(variable);
    }

    private void checkVariableDomain(String variable, SaveVariableVA saveVariableVA) {
        if (saveVariableVA.getIsClass().get()) {
            saveLocalVariable(variable, saveVariableVA);
        } else {
            checkVariable(variable, saveVariableVA.getCandidate());
        }
    }

    private void saveLocalVariable(String variable, SaveVariableVA saveVariableVA) {
        saveVariableVA.getCandidate()
                .getLocalVariables()
                .stream()
                .filter(this::isNoNameLocalVariable)
                .forEach(propertyModel ->
                        saveLocalVariableName(propertyModel, variable));

        saveVariableVA.getIsClass()
                .set(Boolean.FALSE);
    }

    private Boolean isNoNameLocalVariable(PropertyModel propertyModel) {
        return propertyModel.getName() == null;
    }

    private void saveLocalVariableName(PropertyModel propertyModel, String variable) {
        propertyModel.setName(variable);
    }

    private void checkVariable(String variable, Candidate candidate) {
        if (isVariableUsed(variable, candidate)) {
            saveGlobalVariable(variable, candidate);
        }
    }

    private Boolean isVariableUsed(String variable, Candidate candidate) {
        return !isParameter(variable, candidate) &&
                !isLocalVariable(variable, candidate) &&
                !isGlobalVariable(variable, candidate);
    }

    private Boolean isParameter(String variable, Candidate candidate) {
        return candidate.getParameters()
                .stream()
                .anyMatch(propertyModel -> isContainsVariable(variable, propertyModel));
    }

    private Boolean isContainsVariable(String variable, PropertyModel propertyModel) {
        return propertyModel.getName().equals(variable);
    }

    private Boolean isLocalVariable(String variable, Candidate candidate) {
        return candidate.getLocalVariables()
                .stream()
                .anyMatch(propertyModel -> isContainsVariable(variable, propertyModel));
    }

    private Boolean isGlobalVariable(String variable, Candidate candidate) {
        String globalVariable = GLOBAL_VARIABLE_PREFIX + variable;

        return candidate.getGlobalVariables().contains(variable) ||
                candidate.getGlobalVariables().contains(globalVariable);
    }

    private void analysisBlockStatementVariable(StatementModel statementModel, Candidate candidate) {
        ((BlockModel) statementModel).getStatements()
                .forEach(blockStatementModel -> analysisStatementVariable(blockStatementModel, candidate));
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

        return isNoneCandidateVariableUseInRemaining(candidate, remainingBlock) &&
                rawVariablesContainsAssigment.isEmpty();
    }

    private Boolean isNoneCandidateVariableUseInRemaining(Candidate candidate, BlockModel remainingBlock) {
        Candidate remainingCandidate = buildCandidate(remainingBlock.getStatements());
        List<StatementModel> candidateStatements = mergeAllStatements(candidate.getStatements());
        List<List<String>> filteredRemainingRawVariables = getFilteredRawVariables(
                remainingCandidate,
                candidateStatements.get(candidateStatements.size() - SECOND_INDEX).getIndex());

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

    private List<List<String>> getFilteredRawVariables(Candidate remainingCandidate, Integer indexFilter) {
        List<List<String>> filteredRawVariables = new ArrayList<>();
        List<StatementModel> remainingStatements = mergeAllStatements(remainingCandidate.getStatements());
        analysisVariable(remainingCandidate);

        Integer maxIndexFilter = searchMaxIndexFilter(remainingStatements, indexFilter);

        for (int index = FIRST_INDEX; index < remainingStatements.size(); index++) {
            StatementModel statementModel = remainingStatements.get(index);

            if (statementModel.getIndex() > indexFilter && statementModel.getIndex() < maxIndexFilter) {
                filteredRawVariables.add(remainingCandidate.getRawVariables().get(index));
            }
        }

        return filteredRawVariables;
    }

    private Integer searchMaxIndexFilter(List<StatementModel> remainingStatements, Integer indexFilter) {
        AtomicInteger maxIndexFilter = new AtomicInteger(remainingStatements.size());

        if (maxIndexFilter.get() > EMPTY_SIZE) {
            doSearchMaxIndexFilter(remainingStatements, maxIndexFilter, indexFilter);
        }

        return maxIndexFilter.get();
    }

    private void doSearchMaxIndexFilter(List<StatementModel> remainingStatements,
                                        AtomicInteger maxIndexFilter, Integer indexFilter) {
        StatementModel lastStatementModel = searchStatementByIndex(remainingStatements,
                indexFilter + SECOND_INDEX);
        Integer firstIndex = remainingStatements.indexOf(lastStatementModel);

        for (int index = firstIndex; index >= FIRST_INDEX; index--) {
            StatementModel statementModel = remainingStatements.get(index);

            if (isInsideBlock(statementModel, indexFilter)) {
                maxIndexFilter.set(index);
                break;
            }
        }
    }

    private Boolean isInsideBlock(StatementModel statementModel, Integer indexFilter) {
        return statementModel instanceof BlockModel &&
                ((BlockModel) statementModel).getEndOfBlockStatement().getIndex() > indexFilter;
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

    private BlockModel getMethodBlockStatement(List<StatementModel> statements) {
        return BlockModel.blockBuilder()
                .statements(new ArrayList<>(statements))
                .build();
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
        Stream<PropertyModel> parametersFromLocalVariable = getParameterCandidates(
                methodModel.getLocalVariables(), candidate.getGlobalVariables());

        Stream<PropertyModel> parametersFromMethodParameters = getParameterCandidates(
                methodModel.getParameters(), candidate.getGlobalVariables());

        return Stream.concat(parametersFromLocalVariable, parametersFromMethodParameters)
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

    private void sortingCandidates(List<Candidate> candidates) {
        candidates.sort(Comparator.comparing(Candidate::getTotalScore)
                .reversed());
    }

    private MethodModel createMethodModelFromCandidate(MethodModel methodModel,
                                                       Candidate candidate) {
        String extractedMethodName = methodModel.getName() + EXTRACTED_METHOD_NAME_SUFFIX;

        return MethodModel.builder()
                .keywords(Collections.singletonList(EXTRACTED_METHOD_KEYWORD))
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
        List<String> exceptions = new ArrayList<>(methodModel.getExceptions());
        Integer firstCandidateStatementIndex = candidate.getStatements()
                .get(FIRST_INDEX)
                .getIndex();
        Integer statementIndex = firstCandidateStatementIndex - SECOND_INDEX;
        StatementModel statementModel = searchStatementByIndex(methodModel.getStatements(), statementIndex);

        searchException(methodModel, statementModel, exceptions);

        return exceptions.stream()
                .distinct()
                .collect(Collectors.toList());
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

    private void searchException(MethodModel methodModel, StatementModel statementModel,
                                 List<String> exceptions) {
        if (isTryCatchBlock(statementModel)) {
            methodModel.getLocalVariables()
                    .stream()
                    .filter(variablePropertyModel ->
                            variablePropertyModel.getStatementIndex().equals(statementModel.getIndex()))
                    .forEach(variablePropertyModel ->
                            exceptions.add(variablePropertyModel.getType()));
        }
    }

    private Boolean isTryCatchBlock(StatementModel statementModel) {
        if (statementModel instanceof BlockModel) {
            return isContainCatchBlock((BlockModel) statementModel);
        } else {
            return Boolean.FALSE;
        }
    }

    private Boolean isContainCatchBlock(BlockModel blockModel) {
        String statement = blockModel.getEndOfBlockStatement().getStatement();
        Pattern pattern = Pattern.compile(Pattern.quote(CATCH_BLOCK_REGEX));
        Matcher matcher = pattern.matcher(statement);

        return matcher.find();
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
        statements.forEach(statementModel ->
                checkStatementModel(statementModel, extractedMethodModel));

        writeCallExtractedMethodStatement(statements, statements.get(FIRST_INDEX), extractedMethodModel);
    }

    private void checkStatementModel(StatementModel statementModel,
                                     MethodModel extractedMethodModel) {
        if (statementModel instanceof BlockModel) {
            BlockModel blockModel = (BlockModel) statementModel;

            writeCallExtractedMethodBlockStatement(blockModel, extractedMethodModel);
            addCallExtractedMethodStatement(blockModel.getStatements(), extractedMethodModel);
        }
    }

    private void writeCallExtractedMethodBlockStatement(BlockModel blockModel,
                                                        MethodModel extractedMethodModel) {
        Integer firstExtractedStatementIndex = extractedMethodModel.getStatements()
                .get(FIRST_INDEX)
                .getIndex();

        if (isNeedWriteCallExtractedMethodStatement(blockModel, firstExtractedStatementIndex)) {
            StatementModel statementModel = createCallExtractedMethodStatement(
                    extractedMethodModel, firstExtractedStatementIndex);
            blockModel.getStatements().add(FIRST_INDEX, statementModel);
        }
    }

    private Boolean isNeedWriteCallExtractedMethodStatement(StatementModel statementModel,
                                                            Integer firstExtractedStatementIndex) {
        return statementModel.getIndex().equals(firstExtractedStatementIndex - SECOND_INDEX);
    }

    private void writeCallExtractedMethodStatement(List<StatementModel> statements,
                                                   StatementModel statementModel,
                                                   MethodModel extractedMethodModel) {
        Integer firstExtractedStatementIndex = extractedMethodModel.getStatements()
                .get(FIRST_INDEX)
                .getIndex();

        if (isWriteOnMethodStatement(statementModel, firstExtractedStatementIndex)) {
            StatementModel newStatementModel = createCallExtractedMethodStatement(
                    extractedMethodModel, firstExtractedStatementIndex);
            statements.add(newStatementModel);
        }
    }

    private Boolean isWriteOnMethodStatement(StatementModel statementModel,
                                             Integer firstExtractedStatementIndex) {
        return !(statementModel instanceof BlockModel) &&
                isNeedWriteCallExtractedMethodStatement(statementModel, firstExtractedStatementIndex);
    }

    private StatementModel createCallExtractedMethodStatement(MethodModel extractedMethodModel,
                                                              Integer firstExtractedStatementIndex) {
        String statement = extractedMethodModel.getName() +
                createMethodCallParameters(extractedMethodModel.getParameters());

        return StatementModel.statementBuilder()
                .index(firstExtractedStatementIndex)
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
