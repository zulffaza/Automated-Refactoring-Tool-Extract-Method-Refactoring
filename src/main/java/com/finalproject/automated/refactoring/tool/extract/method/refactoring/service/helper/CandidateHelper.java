package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper;

import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 25 June 2019
 */

public class CandidateHelper {

    private static final Integer FIRST_INDEX = 0;
    private static final Integer INVALID_INDEX = -1;

    public static BlockModel getMethodBlockStatement(List<StatementModel> statements) {
        return BlockModel.blockBuilder()
                .statements(new ArrayList<>(statements))
                .build();
    }

    public static BlockModel getRemainingBlockModel(BlockModel methodBlock, BlockModel candidateBlock) {
        BlockModel remainingBlock = createCopyBlockMethod(methodBlock);
        removeCandidates(remainingBlock, candidateBlock.getStatements());

        return remainingBlock;
    }

    private static BlockModel createCopyBlockMethod(BlockModel blockMethod) {
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

    private static void copyListStatements(BlockModel blockMethod) {
        int size = blockMethod.getStatements().size();

        for (int index = FIRST_INDEX; index < size; index++) {
            replaceBlockMethod(blockMethod, index);
        }
    }

    private static void replaceBlockMethod(BlockModel blockMethod, Integer index) {
        StatementModel statement = blockMethod.getStatements().get(index);

        if (statement instanceof BlockModel) {
            blockMethod.getStatements()
                    .set(index, createCopyBlockMethod(((BlockModel) statement)));
        }
    }

    private static void removeCandidates(BlockModel blockMethod, List<StatementModel> candidates) {
        List<StatementModel> statements = blockMethod.getStatements();

        if (statements.containsAll(candidates)) {
            statements.removeAll(candidates);
        } else {
            statements.forEach(statement -> searchBlockCandidatesToRemove(statement, candidates));
        }
    }

    private static void searchBlockCandidatesToRemove(StatementModel statement, List<StatementModel> candidates) {
        if (statement instanceof BlockModel) {
            removeCandidates((BlockModel) statement, candidates);
        }
    }

    public static Integer getStatementCount(BlockModel blockMethod) {
        AtomicInteger count = new AtomicInteger();
        countBlockStatement(blockMethod, count);

        return count.get();
    }

    private static void countBlockStatement(BlockModel blockMethod, AtomicInteger count) {
        blockMethod.getStatements()
                .forEach(statement -> countStatement(statement, count));
    }

    private static void countStatement(StatementModel statement, AtomicInteger count) {
        count.getAndIncrement();

        if (statement instanceof BlockModel) {
            countBlockStatement((BlockModel) statement, count);
        }
    }

    public static Boolean isLocalVariableNameEquals(PropertyModel localVariable, String variable) {
        return localVariable.getName()
                .equals(variable);
    }

    public static List<StatementModel> mergeAllStatements(List<StatementModel> statements) {
        List<StatementModel> mergeStatements = new ArrayList<>();
        mergeStatements(statements, mergeStatements);

        return mergeStatements;
    }

    private static void mergeStatements(List<StatementModel> statements, List<StatementModel> mergeStatements) {
        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            mergeStatement(statements.get(index), mergeStatements);
        }
    }

    private static void mergeStatement(StatementModel statementModel, List<StatementModel> mergeStatements) {
        mergeStatements.add(statementModel);

        if (statementModel instanceof BlockModel) {
            mergeStatements(((BlockModel) statementModel).getStatements(), mergeStatements);
        }
    }


    public static StatementModel searchStatementByIndex(List<StatementModel> statements, Integer index) {
        StatementModel statementModel = StatementModel.statementBuilder().build();
        findStatementByIndex(statements, statementModel, index);

        return statementModel;
    }

    private static void findStatementByIndex(List<StatementModel> statements,
                                             StatementModel statementModel, Integer indexToFind) {
        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            if (statementModel.getIndex() != null) {
                break;
            }

            checkStatementIndex(statements.get(index), statementModel, indexToFind);
        }
    }

    private static void checkStatementIndex(StatementModel statementModel,
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

    private static void copyStatementModel(StatementModel statementModel, StatementModel tempStatementModel) {
        tempStatementModel.setIndex(statementModel.getIndex());
        tempStatementModel.setStatement(statementModel.getStatement());
        tempStatementModel.setStartIndex(statementModel.getStartIndex());
        tempStatementModel.setEndIndex(statementModel.getEndIndex());
    }

    public static Integer searchIndexOfStatements(List<StatementModel> remainingStatements,
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

    private static Boolean isStatementEquals(StatementModel statementModel,
                                             StatementModel nextStatementModel) {
        return statementModel.getIndex().equals(nextStatementModel.getIndex()) &&
                statementModel.getStatement().equals(nextStatementModel.getStatement()) &&
                statementModel.getStartIndex().equals(nextStatementModel.getStartIndex()) &&
                statementModel.getEndIndex().equals(nextStatementModel.getEndIndex());
    }
}
