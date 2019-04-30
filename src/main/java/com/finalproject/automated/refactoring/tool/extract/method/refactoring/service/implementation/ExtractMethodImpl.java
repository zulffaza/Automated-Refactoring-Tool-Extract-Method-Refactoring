package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@Service
public class ExtractMethodImpl implements ExtractMethod {

    @Value("${threshold.min.candidate.statements}")
    private Integer minCandidateStatements;

    @Value("${statement.length.score.constant}")
    private Double statementLengthScoreConstant;

    @Value("${statement.length.score.max}")
    private Double statementLengthScoreMax;

    @Value("${nesting.area.score.constant}")
    private Double nestingAreaScoreConstant;

    @Override
    public void refactoring(@NonNull MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);
        scoringCandidates(methodModel, candidates);

        System.out.println(candidates.size());
        candidates.forEach(System.out::println);
    }

    private List<Candidate> getCandidates(MethodModel methodModel) {
        List<Candidate> candidates = new ArrayList<>();
        BlockModel methodBlock = getMethodBlockStatement(methodModel.getStatements());

        getAllBlocksMethod(methodModel)
                .forEach(statements -> searchCandidates(methodBlock, statements, candidates));

        return candidates;
    }

    private BlockModel getMethodBlockStatement(List<StatementModel> statements) {
        return BlockModel.blockBuilder()
                .statements(new ArrayList<>(statements))
                .build();
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

    private void searchCandidates(BlockModel methodBlock, List<StatementModel> statements,
                                  List<Candidate> candidates) {
        int max = statements.size();

        for (int i = 0; i < max; i++) {
            for (int j = i + 1; j <= max; j++) {
                checkCandidate(methodBlock, statements.subList(i, j), candidates);
            }
        }
    }

    private void checkCandidate(BlockModel methodBlock, List<StatementModel> candidate,
                                List<Candidate> candidates) {
        candidate = new ArrayList<>(candidate);

        if (isCandidateValid(methodBlock, candidate)) {
            saveCandidate(candidate, candidates);
        }
    }

    private Boolean isCandidateValid(BlockModel methodBlock, List<StatementModel> candidate) {
        return isQualityValid(methodBlock, candidate) && isBehaviourPreservationValid(candidate);
    }

    private Boolean isBehaviourPreservationValid(List<StatementModel> candidate) {
        // TODO check behaviour of candidate statements

        return Boolean.TRUE;
    }

    private Boolean isQualityValid(BlockModel methodBlock, List<StatementModel> candidate) {
        BlockModel candidateBlock = getMethodBlockStatement(candidate);
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

    private void saveCandidate(List<StatementModel> candidate, List<Candidate> candidates) {
        Candidate newCandidate = Candidate.builder()
                .statements(candidate)
                .build();

        candidates.add(newCandidate);
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

        int remainingNestingAreaDeviation = methodNestingArea - remainingNestingArea;
        int candidateNestingAreaDeviation = methodNestingArea - candidateNestingArea;

        Double nestingAreaReduction = Double.min(remainingNestingAreaDeviation, candidateNestingAreaDeviation);

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
        return 0D;
    }
}
