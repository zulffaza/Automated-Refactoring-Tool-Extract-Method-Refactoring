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

    @Override
    public void refactoring(@NonNull MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);

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
        BlockModel remainingBlock = createCopyBlockMethod(methodBlock);

        removeCandidates(remainingBlock, candidate);

        Integer candidateStatementCount = getStatementCount(candidateBlock);
        Integer remainingStatementCount = getStatementCount(remainingBlock);

        return (candidateStatementCount >= minCandidateStatements) &&
                (remainingStatementCount >= minCandidateStatements);
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

    private void copyListStatements(BlockModel blockModel) {
        int size = blockModel.getStatements().size();

        for (int i = 0; i < size; i++) {
            replaceBlockMethod(blockModel, i);
        }
    }

    private void replaceBlockMethod(BlockModel blockModel, Integer index) {
        StatementModel statement = blockModel.getStatements().get(index);

        if (statement instanceof BlockModel) {
            blockModel.getStatements()
                    .set(index, createCopyBlockMethod(((BlockModel) statement)));
        }
    }

    private Integer getStatementCount(BlockModel methodBlock) {
        AtomicInteger count = new AtomicInteger();
        countBlockStatement(methodBlock, count);

        return count.get();
    }

    private void countBlockStatement(BlockModel block, AtomicInteger count) {
        block.getStatements()
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

    private void removeCandidates(BlockModel block, List<StatementModel> candidates) {
        List<StatementModel> statements = block.getStatements();

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
}
