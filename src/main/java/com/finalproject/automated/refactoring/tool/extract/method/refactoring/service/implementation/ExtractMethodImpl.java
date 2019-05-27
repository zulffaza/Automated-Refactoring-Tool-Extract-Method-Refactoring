package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private VariableHelper variableHelper;

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
    private static final String ASSIGMENT_OPERATOR = "=";
    private static final String EQUAL_TO_OPERATOR = "==";
    private static final String NOT_EQUAL_TO_OPERATOR = "!=";
    private static final String INCREMENT_OPERATOR = "++";
    private static final String DECREMENT_OPERATOR = "--";

    private static final Integer SINGLE_LIST_SIZE = 1;
    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;

    @Override
    public void refactoring(@NonNull MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);
        scoringCandidates(methodModel, candidates);
        sortingCandidates(candidates);

        candidates.forEach(System.out::println);
        System.out.println();
        System.out.println("Best candidate --> " + candidates.get(0));
        System.out.println("Best score --> " + candidates.get(0).getTotalScore());
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

        for (int i = 0; i < statements.size(); i++) {
            for (int j = i + 1; j <= statements.size(); j++) {
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
        saveVariables(variables, candidate);

        if (statementModel instanceof BlockModel) {
            readBlockStatement(statementModel, candidate);
        }
    }

    private void saveVariables(List<String> variables, Candidate candidate) {
        AtomicBoolean isClass = new AtomicBoolean();

        variables.stream()
                .filter(this::isNotOperators)
                .forEach(variable -> saveVariable(variable, isClass, candidate));

        candidate.getRawVariables().add(variables);
    }

    private Boolean isNotOperators(String variable) {
        return !variable.matches(VariableHelper.OPERATORS_CHARACTERS_REGEX);
    }

    private void saveVariable(String variable, AtomicBoolean isClass, Candidate candidate) {
        if (isPropertyType(variable)) {
            savePropertyType(variable, isClass, candidate);
        } else {
            checkVariableDomain(variable, isClass, candidate);
        }
    }

    private Boolean isPropertyType(String variable) {
        return VariableHelper.PRIMITIVE_TYPES.contains(variable) ||
                variableHelper.isClassName(variable);
    }

    private void savePropertyType(String variable, AtomicBoolean isClass, Candidate candidate) {
        PropertyModel propertyModel = PropertyModel.builder()
                .type(variable)
                .build();

        candidate.getLocalVariables()
                .add(propertyModel);

        isClass.set(Boolean.TRUE);
    }

    private void checkVariableDomain(String variable, AtomicBoolean isClass, Candidate candidate) {
        if (isClass.get()) {
            saveLocalVariable(variable, isClass, candidate);
        } else {
            checkVariable(variable, candidate);
        }
    }

    private void saveLocalVariable(String variable, AtomicBoolean isClass, Candidate candidate) {
        candidate.getLocalVariables()
                .stream()
                .filter(this::isNoNameLocalVariable)
                .forEach(propertyModel -> saveLocalVariableName(propertyModel, variable));

        isClass.set(Boolean.FALSE);
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

    private void saveGlobalVariable(String variable, Candidate candidate) {
        candidate.getGlobalVariables()
                .add(variable);
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
        List<List<String>> collect = candidate.getRawVariables()
                .stream()
                .filter(rawVariables -> isContainsAssignment(methodModel, candidate, rawVariables))
                .collect(Collectors.toList());

        return collect.size() <= SINGLE_LIST_SIZE;
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
            Boolean isLocalVariable = isLocalVariable(candidate.getLocalVariables(), firstVariable);
            Boolean isMethodLocalVariable = isLocalVariable(methodModel.getLocalVariables(), firstVariable);

            isAssigment = !isLocalVariable && isMethodLocalVariable;
        }

        return isAssigment;
    }

    private Boolean isContainsAssignmentOperators(String variable) {
        return variable.contains(ASSIGMENT_OPERATOR) &&
                (!variable.contains(EQUAL_TO_OPERATOR) || !variable.contains(NOT_EQUAL_TO_OPERATOR));
    }

    private Boolean isLocalVariable(List<PropertyModel> localVariables, String variable) {
        return localVariables.stream()
                .anyMatch(localVariable ->
                        isLocalVariableNameEquals(localVariable, variable));
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
                variable = getVariableWhichUses(rawVariables, index);

                Boolean isLocalVariable = isLocalVariable(candidate.getLocalVariables(), variable);
                Boolean isMethodLocalVariable = isLocalVariable(methodModel.getLocalVariables(), variable);

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

    private BlockModel getMethodBlockStatement(List<StatementModel> statements) {
        return BlockModel.blockBuilder()
                .statements(new ArrayList<>(statements))
                .build();
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
        copyBlockMethod.setStartIndex(blockMethod.getStartIndex());
        copyBlockMethod.setEndIndex(blockMethod.getEndIndex());
        copyListStatements(copyBlockMethod);

        return copyBlockMethod;
    }

    private void copyListStatements(BlockModel blockMethod) {
        int size = blockMethod.getStatements().size();

        for (int i = 0; i < size; i++) {
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
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());

        candidates.forEach(
                candidate -> scoringCandidate(methodBlock, candidate));
    }

    private void scoringCandidate(BlockModel methodBlock, Candidate candidate) {
        BlockModel candidateBlock = getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = getRemainingBlockModel(methodBlock, candidateBlock);

        candidate.setLengthScore(calculateStatementLengthScore(candidateBlock, remainingBlock));
        candidate.setNestingDepthScore(calculateNestingDepthScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setNestingAreaScore(calculateNestingAreaScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setParameterScore(calculateParameterScore(methodBlock, candidateBlock, remainingBlock));

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

    private Double calculateParameterScore(BlockModel methodBlock, BlockModel candidateBlock,
                                           BlockModel remainingBlock) {
        Integer parameterIn = 0;
        Integer parameterOut = 0;

        return parameterScoreMax - parameterIn - parameterOut;
    }

    private void calculateTotalScore(Candidate candidate) {
        candidate.setTotalScore(candidate.getLengthScore() + candidate.getNestingDepthScore() +
                candidate.getNestingAreaScore() + candidate.getParameterScore());
    }

    private void sortingCandidates(List<Candidate> candidates) {
        candidates.sort(Comparator.comparing(Candidate::getTotalScore).reversed());
    }
}
