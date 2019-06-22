package com.finalproject.automated.refactoring.tool.extract.method.refactoring.model;

import lombok.Builder;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 28 May 2019
 */

@Data
@Builder
public class ReadStatementIndexVA {

    private Integer variableStatementIndex;

    @Builder.Default
    private AtomicInteger statementCount = new AtomicInteger();

    private Integer realStatementIndex;
}
