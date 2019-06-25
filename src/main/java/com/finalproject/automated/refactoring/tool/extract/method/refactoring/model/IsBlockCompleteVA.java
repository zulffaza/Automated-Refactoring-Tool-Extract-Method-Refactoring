package com.finalproject.automated.refactoring.tool.extract.method.refactoring.model;

import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 24 June 2019
 */

@Data
@Builder
public class IsBlockCompleteVA {

    private List<StatementModel> statements;

    private StatementModel beforeStatementModel;

    private StatementModel statementModel;

    private StatementModel nextStatementModel;

    private BlockModel methodBlock;

    private AtomicBoolean isBlockComplete;
}
