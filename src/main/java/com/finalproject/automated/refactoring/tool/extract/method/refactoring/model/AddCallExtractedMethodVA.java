package com.finalproject.automated.refactoring.tool.extract.method.refactoring.model;

import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 23 June 2019
 */

@Data
@Builder
public class AddCallExtractedMethodVA {

    private List<StatementModel> statements;

    private MethodModel extractedMethodModel;

    private Integer extractedStatementIndex;

    private List<StatementModel> extractedStatements;

    private Integer statementArrayIndex;
}
