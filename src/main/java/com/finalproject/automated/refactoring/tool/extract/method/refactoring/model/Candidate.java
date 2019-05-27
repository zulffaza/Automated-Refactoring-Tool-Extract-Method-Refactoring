package com.finalproject.automated.refactoring.tool.extract.method.refactoring.model;

import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@Data
@Builder
public class Candidate {

    @Builder.Default
    private List<StatementModel> statements = new ArrayList<>();

    @Builder.Default
    private List<PropertyModel> localVariables = new ArrayList<>();

    @Builder.Default
    private List<String> globalVariables = new ArrayList<>();

    @Builder.Default
    private List<PropertyModel> parameters = new ArrayList<>();

    @Builder.Default
    private List<List<String>> rawVariables = new ArrayList<>();

    private PropertyModel returnType;

    private String returnTypeStatement;

    private Integer returnTypeStatementRawVariableIndex;

    private Double lengthScore;

    private Double nestingDepthScore;

    private Double nestingAreaScore;

    private Double parameterScore;

    private Double totalScore;
}
