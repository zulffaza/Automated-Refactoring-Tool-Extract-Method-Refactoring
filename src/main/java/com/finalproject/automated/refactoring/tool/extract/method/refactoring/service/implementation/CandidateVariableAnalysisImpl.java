package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.SaveVariableVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateVariableAnalysis;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.model.VariablePropertyModel;
import com.finalproject.automated.refactoring.tool.utils.service.VariableHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 23 June 2019
 */

@Service
public class CandidateVariableAnalysisImpl implements CandidateVariableAnalysis {

    @Autowired
    private VariableHelper variableHelper;

    private static final String GLOBAL_VARIABLE_PREFIX = "this.";
    private static final String EMPTY_STRING = "";

    private static final Integer FIRST_INDEX = 0;

    @Override
    public void analysis(@NonNull MethodModel methodModel, @NonNull Candidate candidate) {
        candidate.getStatements()
                .forEach(statementModel -> analysisStatementVariable(statementModel, candidate));

        removeUnusedVariable(methodModel, candidate);
    }

    private void analysisStatementVariable(StatementModel statementModel, Candidate candidate) {
        List<String> variables = variableHelper.readVariable(statementModel.getStatement());
        saveVariables(statementModel, variables, candidate);

        if (statementModel instanceof BlockModel) {
            analysisBlockStatementVariable(statementModel, candidate);
        }
    }

    private void saveVariables(StatementModel statementModel, List<String> variables,
                               Candidate candidate) {
        SaveVariableVA saveVariableVA = SaveVariableVA.builder()
                .statementModel(statementModel)
                .candidate(candidate)
                .build();

        variables.stream()
                .filter(this::isNotOperators)
                .forEach(variable -> saveVariable(variable, saveVariableVA));

        candidate.getRawVariables()
                .add(variables);
    }

    private Boolean isNotOperators(String variable) {
        return !variable.matches(VariableHelper.OPERATORS_CHARACTERS_REGEX);
    }

    private void saveVariable(String variable, SaveVariableVA saveVariableVA) {
        if (isPropertyType(variable)) {
            savePropertyType(variable, saveVariableVA);
        } else if (checkGlobalVariable(variable)) {
            saveGlobalVariableWithPreProcessing(variable, saveVariableVA.getCandidate());
        } else {
            checkVariableDomain(variable, saveVariableVA);
        }
    }

    private Boolean isPropertyType(String variable) {
        return VariableHelper.PRIMITIVE_TYPES.contains(variable) ||
                variableHelper.isClassName(variable);
    }

    private void savePropertyType(String variable, SaveVariableVA saveVariableVA) {
        VariablePropertyModel variablePropertyModel = VariablePropertyModel.variablePropertyBuilder()
                .statementIndex(saveVariableVA.getStatementModel().getIndex())
                .build();

        variablePropertyModel.setType(variable);

        saveVariableVA.getCandidate()
                .getLocalVariables()
                .add(variablePropertyModel);

        saveVariableVA.getIsClass()
                .set(Boolean.TRUE);
    }

    private Boolean checkGlobalVariable(String variable) {
        return variable.startsWith(GLOBAL_VARIABLE_PREFIX);
    }

    private void saveGlobalVariableWithPreProcessing(String variable, Candidate candidate) {
        variable = variable.replace(GLOBAL_VARIABLE_PREFIX, EMPTY_STRING);
        saveGlobalVariable(variable, candidate);
    }

    private void saveGlobalVariable(String variable, Candidate candidate) {
        candidate.getGlobalVariables()
                .add(variable);
    }

    private void checkVariableDomain(String variable, SaveVariableVA saveVariableVA) {
        if (saveVariableVA.getIsClass().get()) {
            saveLocalVariable(variable, saveVariableVA);
        } else {
            checkVariable(variable, saveVariableVA.getCandidate());
        }
    }

    private void saveLocalVariable(String variable, SaveVariableVA saveVariableVA) {
        saveVariableVA.getCandidate()
                .getLocalVariables()
                .stream()
                .filter(this::isNoNameLocalVariable)
                .forEach(propertyModel ->
                        saveLocalVariableName(propertyModel, variable));

        saveVariableVA.getIsClass()
                .set(Boolean.FALSE);
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
        return !isLocalVariable(variable, candidate) &&
                !isGlobalVariable(variable, candidate);
    }

    private Boolean isLocalVariable(String variable, Candidate candidate) {
        return candidate.getLocalVariables()
                .stream()
                .anyMatch(propertyModel -> isContainsVariable(variable, propertyModel));
    }

    private Boolean isContainsVariable(String variable, PropertyModel propertyModel) {
        return propertyModel.getName().equals(variable);
    }

    private Boolean isGlobalVariable(String variable, Candidate candidate) {
        String globalVariable = GLOBAL_VARIABLE_PREFIX + variable;

        return candidate.getGlobalVariables().contains(variable) ||
                candidate.getGlobalVariables().contains(globalVariable);
    }

    private void removeUnusedVariable(MethodModel methodModel, Candidate candidate) {
        List<VariablePropertyModel> localVariables = candidate.getLocalVariables()
                .stream()
                .filter(variable ->
                        isNotMethodParameter(methodModel.getParameters(), variable, candidate))
                .collect(Collectors.toList());

        candidate.setLocalVariables(localVariables);
    }

    private Boolean isNotMethodParameter(List<PropertyModel> parameters, PropertyModel variable,
                                         Candidate candidate) {
        List<String> equalsToParameters = parameters.stream()
                .filter(parameter -> isParameterNameEquals(parameter, variable))
                .map(PropertyModel::getName)
                .collect(Collectors.toList());

        candidate.getGlobalVariables()
                .addAll(FIRST_INDEX, equalsToParameters);

        return equalsToParameters.isEmpty();
    }

    private Boolean isParameterNameEquals(PropertyModel parameter, PropertyModel variable) {
        return parameter.getName()
                .equals(variable.getName());
    }

    private void analysisBlockStatementVariable(StatementModel statementModel, Candidate candidate) {
        ((BlockModel) statementModel).getStatements()
                .forEach(blockStatementModel -> analysisStatementVariable(blockStatementModel, candidate));
    }
}
