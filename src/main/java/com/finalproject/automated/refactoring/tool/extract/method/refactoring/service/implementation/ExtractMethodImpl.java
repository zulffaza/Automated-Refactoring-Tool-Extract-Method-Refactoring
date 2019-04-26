package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.NonNull;
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

    @Override
    public void refactoring(@NonNull MethodModel methodModel) {
        List<Candidate> candidates = getCandidates(methodModel);

        System.out.println(candidates.size());
        candidates.forEach(System.out::println);
    }

    private List<Candidate> getCandidates(MethodModel methodModel) {
        List<Candidate> candidates = new ArrayList<>();

        getAllBlocksMethod(methodModel)
                .forEach(statements -> searchCandidates(getStatementCount(methodModel),
                        statements, candidates));

        return candidates;
    }

    private List<List<StatementModel>> getAllBlocksMethod(MethodModel methodModel) {
        List<List<StatementModel>> blocks = initializeFirstBlock(methodModel);
        methodModel.getStatements()
                .forEach(statement -> searchBlock(statement, blocks));

        return blocks;
    }

    private List<List<StatementModel>> initializeFirstBlock(MethodModel methodModel) {
        List<List<StatementModel>> blocks = new ArrayList<>();
        blocks.add(methodModel.getStatements());

        return blocks;
    }

    private void searchBlock(StatementModel statement, List<List<StatementModel>> blocks) {
        if (statement instanceof BlockModel) {
            saveBlock(statement, blocks);
        }
    }

    private void saveBlock(StatementModel statement, List<List<StatementModel>> blocks) {
        BlockModel block = (BlockModel) statement;

        blocks.add(block.getStatements());
        block.getStatements()
                .forEach(blockStatement -> searchBlock(blockStatement, blocks));
    }

    private Integer getStatementCount(MethodModel methodModel) {
        AtomicInteger count = new AtomicInteger();
        BlockModel block = BlockModel.blockBuilder()
                .statements(methodModel.getStatements())
                .build();

        countBlockStatement(block, count);

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

    private void searchCandidates(Integer statementCount, List<StatementModel> statements,
                                  List<Candidate> candidates) {
        System.out.println("Statement : " + statementCount);

        for (int i = 0; i < statements.size(); i++) {
            for (int j = i + 1; j < (statements.size() + 1); j++) {
                List<StatementModel> candidate = statements.subList(i, j);

                if (isCandidateValid(statementCount, candidate)) {
                    candidates.add(Candidate.builder()
                            .statements(candidate)
                            .build());
                }
            }
        }
    }

    private Boolean isCandidateValid(Integer statementCount, List<StatementModel> candidate) {
        return isQualityValid(statementCount, candidate) && Boolean.TRUE;
    }

    private Boolean isQualityValid(Integer statementCount, List<StatementModel> candidate) {
        // TODO check quality preconditions

        return Boolean.TRUE;
    }
}
