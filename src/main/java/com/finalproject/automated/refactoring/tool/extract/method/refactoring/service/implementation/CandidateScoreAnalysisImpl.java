package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateScoreAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper.CandidateHelper;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 27 June 2019
 */

@Service
public class CandidateScoreAnalysisImpl implements CandidateScoreAnalysis {

    @Value("${statement.length.score.constant}")
    private Double statementLengthScoreConstant;

    @Value("${statement.length.score.max}")
    private Double statementLengthScoreMax;

    @Value("${nesting.area.score.constant}")
    private Double nestingAreaScoreConstant;

    @Value("${parameter.score.max}")
    private Double parameterScoreMax;

    private static final Integer RETURN_TYPE_VALID_SCORE = 1;
    private static final Integer RETURN_TYPE_INVALID_SCORE = 0;

    @Override
    public void analysis(@NonNull MethodModel methodModel, @NonNull Candidate candidate) {
        BlockModel methodBlock = CandidateHelper.getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(candidate.getStatements());
        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, candidateBlock);

        candidate.setLengthScore(calculateStatementLengthScore(candidateBlock, remainingBlock));
        candidate.setNestingDepthScore(calculateNestingDepthScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setNestingAreaScore(calculateNestingAreaScore(methodBlock, candidateBlock, remainingBlock));
        candidate.setParameterScore(calculateParameterScore(methodModel, candidate));

        calculateTotalScore(candidate);
    }

    private Double calculateStatementLengthScore(BlockModel candidateBlock, BlockModel remainingBlock) {
        Integer candidateStatementCount = CandidateHelper.getStatementCount(candidateBlock);
        Integer remainingStatementCount = CandidateHelper.getStatementCount(remainingBlock);
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

        int parameterIn = candidate.getParameters().size();
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
                .filter(propertyModel -> CandidateHelper.isLocalVariableNameEquals(propertyModel, variable))
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
}
