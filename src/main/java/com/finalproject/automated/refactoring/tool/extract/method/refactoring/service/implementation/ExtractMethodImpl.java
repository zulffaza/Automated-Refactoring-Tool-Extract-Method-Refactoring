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
import com.finalproject.automated.refactoring.tool.utils.service.MethodModelHelper;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private VariableHelper variableHelper;

    @Autowired
    private MethodModelHelper methodModelHelper;

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
    private static final String METHOD_CALL_SUFFIX = "();";

    private static final Integer SINGLE_LIST_SIZE = 1;
    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;

    @Override
    public void refactoring(@NonNull String path, @NonNull MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);
        scoringCandidates(methodModel, candidates);
        sortingCandidates(candidates);

        Candidate bestCandidate = candidates.get(FIRST_INDEX);
        MethodModel candidateMethodModel = createMethodModelFromCandidate(methodModel, bestCandidate);

        System.out.println("Best candidate --> " + bestCandidate);
        System.out.println("Best score --> " + bestCandidate.getTotalScore());
        System.out.println();
        System.out.println(methodModelHelper.createMethod(methodModel));
        System.out.println();
        System.out.println(methodModelHelper.createMethod(candidateMethodModel));
        System.out.println();
        System.out.println(methodModelHelper.createMethod(createRemainingMethodModel(methodModel, candidateMethodModel)));
    }

    private List<Candidate> getCandidates(MethodModel methodModel) {
        return getAllBlocksMethod(methodModel)
                .parallelStream()
                .flatMap(this::searchCandidates)
                .filter(candidate -> isCandidateValid(methodModel, candidate))
                .collect(Collectors.toList());
    }

    private List<List<StatementModel>> getAllBlocksMethod(MethodModel methodModel) {
        List<List<StatementModel>> blocks = initializeFirstBlock(methodModel);
        methodModel.getStatements()
                .forEach(statement -> searchBlock(statement, blocks));

        return blocks;
    }

    private List<List<StatementModel>> initializeFirstBlock(MethodModel methodModel) {
        List<List<StatementModel>> blocks = new ArrayList<>();
        blocks.add(new ArrayList<>(methodModel.getStatements()));

        return blocks;
    }

    private void searchBlock(StatementModel statement, List<List<StatementModel>> blocks) {
        if (statement instanceof BlockModel) {
            saveBlock(statement, blocks);
        }
    }

    private void saveBlock(StatementModel statement, List<List<StatementModel>> blocks) {
        BlockModel block = (BlockModel) statement;

        blocks.add(new ArrayList<>(block.getStatements()));
        block.getStatements()
                .forEach(blockStatement -> searchBlock(blockStatement, blocks));
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
                .forEach(statementModel -> readStatement(statementModel, candidate));
    }

    private void readStatement(StatementModel statementModel, Candidate candidate) {
        List<String> variables = variableHelper.readVariable(statementModel.getStatement());
        saveVariables(statementModel, variables, candidate);

        if (statementModel instanceof BlockModel) {
            readBlockStatement(statementModel, candidate);
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

        candidate.getRawVariables().add(variables);
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

    private void readBlockStatement(StatementModel statementModel, Candidate candidate) {
        ((BlockModel) statementModel).getStatements()
                .forEach(blockStatementModel -> readStatement(blockStatementModel, candidate));
    }

    private Boolean isCandidateValid(MethodModel methodModel, Candidate candidate) {
        return isBehaviourPreservationValid(methodModel, candidate) &&
                isQualityValid(methodModel, candidate);
    }

    private Boolean isBehaviourPreservationValid(MethodModel methodModel, Candidate candidate) {
        List<List<String>> rawVariablesContainsAssigment = candidate.getRawVariables()
                .stream()
                .filter(rawVariables -> isContainsAssignment(methodModel, candidate, rawVariables))
                .collect(Collectors.toList());

        return rawVariablesContainsAssigment.size() <= SINGLE_LIST_SIZE &&
                checkAndSaveReturnTypeIndexStatement(candidate, methodModel, rawVariablesContainsAssigment);
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

    private Boolean checkAndSaveReturnTypeIndexStatement(Candidate candidate,
                                                         MethodModel methodModel,
                                                         List<List<String>> rawVariablesContainsAssignment) {
        Boolean isLastStatement = Boolean.TRUE;

        if (!rawVariablesContainsAssignment.isEmpty()) {
            Integer rawVariablesIndex = rawVariablesContainsAssignment.size() - SINGLE_LIST_SIZE;
            Integer lastStatementIndex = methodModel.getStatements().size() - SINGLE_LIST_SIZE;
            List<String> rawVariableContainsAssignment = rawVariablesContainsAssignment.get(rawVariablesIndex);
            Integer returnTypeStatementRawVariableIndex = candidate.getRawVariables()
                    .indexOf(rawVariableContainsAssignment);

            candidate.setReturnTypeStatementRawVariableIndex(returnTypeStatementRawVariableIndex);
            isLastStatement = returnTypeStatementRawVariableIndex.equals(
                    methodModel.getStatements().get(lastStatementIndex).getIndex());
        }

        return isLastStatement;
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

        for (int i = FIRST_INDEX; i < size; i++) {
            replaceBlockMethod(blockMethod, i);
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
            statements.forEach(statement -> searchBlockCandidates(statement, candidates));
        }
    }

    private void searchBlockCandidates(StatementModel statement, List<StatementModel> candidates) {
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
        Integer parameterOut = getReturnType(candidate);

        // TODO create return type statement

        return parameterScoreMax - parameterIn - parameterOut;
    }

    private List<PropertyModel> getCandidateParameters(MethodModel methodModel, Candidate candidate) {
        Stream<PropertyModel> parametersFromLocalVariable = candidate.getGlobalVariables()
                .stream()
                .map(variable -> getMethodVariable(methodModel.getLocalVariables(), variable))
                .filter(Objects::nonNull);

        Stream<PropertyModel> parametersFromMethodParameters = candidate.getGlobalVariables()
                .stream()
                .map(variable -> getMethodVariable(methodModel.getParameters(), variable))
                .filter(Objects::nonNull);

        return Stream.concat(parametersFromLocalVariable, parametersFromMethodParameters)
                .collect(Collectors.toList());
    }

    private PropertyModel getMethodVariable(List variables, String variable) {
        return (PropertyModel) variables.stream()
                .filter(propertyModel -> isLocalVariableNameEquals((PropertyModel) propertyModel, variable))
                .findFirst()
                .orElse(null);
    }

    private Integer getReturnType(Candidate candidate) {
        if (candidate.getReturnTypeStatementRawVariableIndex() != null) {
            return 1;
        } else {
            return 0;
        }
    }

    private PropertyModel createReturnType(MethodModel methodModel, Candidate candidate) {
        Integer returnTypeStatementRawVariableIndex = candidate.getReturnTypeStatementRawVariableIndex();
        return null;
    }

    private void calculateTotalScore(Candidate candidate) {
        candidate.setTotalScore(candidate.getLengthScore() + candidate.getNestingDepthScore() +
                candidate.getNestingAreaScore() + candidate.getParameterScore());
    }

    private void sortingCandidates(List<Candidate> candidates) {
        candidates.sort(Comparator.comparing(Candidate::getTotalScore).reversed());
    }

    private MethodModel createMethodModelFromCandidate(MethodModel methodModel,
                                                       Candidate candidate) {
        String extractedMethodName = methodModel.getName() + EXTRACTED_METHOD_NAME_SUFFIX;

        return MethodModel.builder()
                .keywords(Collections.singletonList(EXTRACTED_METHOD_KEYWORD))
                .returnType("void")
                .name(extractedMethodName)
                .parameters(candidate.getParameters())
                .globalVariables(candidate.getGlobalVariables())
                .localVariables(candidate.getLocalVariables())
                .body(createMethodBody(candidate.getStatements()))
                .statements(candidate.getStatements())
                .build();
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
        for (int i = FIRST_INDEX; i < depth.get(); i++)
            statement.append(TAB);
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
        String statement = extractedMethodModel.getName() + METHOD_CALL_SUFFIX;

        return StatementModel.statementBuilder()
                .index(firstExtractedStatementIndex)
                .statement(statement)
                .build();
    }
}
