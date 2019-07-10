package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.AddCallExtractedMethodVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.CheckExceptionVA;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.CandidateScoreAnalysis;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper.CandidateHelper;
import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.helper.TimeStampHelper;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.PropertyModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import com.finalproject.automated.refactoring.tool.utils.model.request.ReplaceFileVA;
import com.finalproject.automated.refactoring.tool.utils.service.MethodModelHelper;
import com.finalproject.automated.refactoring.tool.utils.service.ReplaceFileHelper;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@Service
public class ExtractMethodImpl implements ExtractMethod {

    @Autowired
    private CandidateAnalysis candidateAnalysis;

    @Autowired
    private CandidateScoreAnalysis candidateScoreAnalysis;

    @Autowired
    private MethodModelHelper methodModelHelper;

    @Autowired
    private ReplaceFileHelper replaceFileHelper;

    private static final String AT = "@";
    private static final String CATCH_REGEX = "^(?:catch)+(?:\\s)*(?:\\()+(?:\\s)*";
    private static final String METHOD_PUBLIC_MODIFIER = "public";
    private static final String METHOD_PROTECTED_MODIFIER = "protected";
    private static final String METHOD_PRIVATE_MODIFIER = "private";
    private static final String UNDERSCORE = "_";
    private static final String EXTRACTED_METHOD_NAME_SUFFIX = "Extracted";
    private static final String NEW_LINE = "\n";
    private static final String TAB = "\t";
    private static final String VOID_RETURN_TYPE = "void";
    private static final String METHOD_PARAMETER_PREFIX = "(";
    private static final String COMMA = ", ";
    private static final String METHOD_PARAMETER_SUFFIX = ");";

    private static final Integer FIRST_INDEX = 0;
    private static final Integer SECOND_INDEX = 1;
    private static final Integer INVALID_INDEX = -1;

    private static final List<String> REMOVED_MODIFIERS = Arrays.asList(
            METHOD_PUBLIC_MODIFIER, METHOD_PROTECTED_MODIFIER, METHOD_PRIVATE_MODIFIER
    );

    @Override
    public Boolean refactoring(@NonNull String path, @NonNull MethodModel methodModel) {
        try {
            return doRefactoring(path, methodModel);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    private Boolean doRefactoring(String path, MethodModel methodModel) {
        Candidate bestCandidate = searchBestExtractCandidate(methodModel);
        return replaceFile(path, methodModel, bestCandidate);
    }

    private Candidate searchBestExtractCandidate(MethodModel methodModel) {
        List<Candidate> candidates = candidateAnalysis.analysis(methodModel);
        scoringCandidates(methodModel, candidates);
        pruningCandidates(candidates);
        sortingCandidates(candidates);

        return candidates.get(FIRST_INDEX);
    }

    private void scoringCandidates(MethodModel methodModel, List<Candidate> candidates) {
        candidates.forEach(
                candidate -> candidateScoreAnalysis.analysis(methodModel, candidate));
    }

    private void pruningCandidates(List<Candidate> candidates) {
        List<Integer> indexToRemoves = new ArrayList<>();

        for (int index = FIRST_INDEX; index < candidates.size(); index++) {
            searchCandidatesThatContainsCandidate(index, candidates, indexToRemoves);
        }

        removeCandidates(indexToRemoves, candidates);
    }

    private void searchCandidatesThatContainsCandidate(Integer index, List<Candidate> candidates,
                                                       List<Integer> indexToRemoves) {
        for (int nextIndex = FIRST_INDEX; nextIndex < candidates.size(); nextIndex++) {
            Candidate candidate = candidates.get(nextIndex);
            Candidate candidateToSearch = candidates.get(index);

            if (index.equals(nextIndex)) {
                continue;
            }

            if (isContainsCandidate(candidate, candidateToSearch)) {
                indexToRemoves.add(index);
                break;
            }
        }
    }

    private Boolean isContainsCandidate(Candidate candidate, Candidate candidateToSearch) {
        AtomicBoolean isContains = new AtomicBoolean();
        searchCandidates(candidate.getStatements(), candidateToSearch.getStatements(), isContains);

        return isContains.get() && candidate.getTotalScore() >= candidateToSearch.getTotalScore();
    }

    private void searchCandidates(List<StatementModel> statements,
                                  List<StatementModel> statementsToSearch,
                                  AtomicBoolean isContains) {
        if (statements.containsAll(statementsToSearch)) {
            isContains.set(!isContains.get());
        } else {
            statements.forEach(statementModel ->
                    searchBlockCandidates(statementModel, statementsToSearch, isContains));
        }
    }

    private void searchBlockCandidates(StatementModel statementModel,
                                       List<StatementModel> statementsToSearch,
                                       AtomicBoolean isContains) {
        if (statementModel instanceof BlockModel) {
            searchCandidates(((BlockModel) statementModel).getStatements(), statementsToSearch,
                    isContains);
        }
    }

    private void removeCandidates(List<Integer> indexToRemoves,
                                  List<Candidate> candidates) {
        int firstIndex = indexToRemoves.size() - SECOND_INDEX;

        for (int index = firstIndex; index >= FIRST_INDEX; index--) {
            Integer indexToRemove = indexToRemoves.get(index);
            candidates.remove(indexToRemove.intValue());
        }
    }

    private void sortingCandidates(List<Candidate> candidates) {
        candidates.sort(Comparator.comparing(Candidate::getTotalScore)
                .reversed());
    }

    private MethodModel createMethodModelFromCandidate(MethodModel methodModel,
                                                       Candidate candidate) {
        String extractedMethodName = methodModel.getName() + UNDERSCORE +
                TimeStampHelper.createTimeStamp() + EXTRACTED_METHOD_NAME_SUFFIX;

        return MethodModel.builder()
                .keywords(createKeywords(methodModel.getKeywords()))
                .returnType(createReturnType(candidate))
                .name(extractedMethodName)
                .parameters(candidate.getParameters())
                .globalVariables(candidate.getGlobalVariables())
                .localVariables(candidate.getLocalVariables())
                .exceptions(createExceptions(methodModel, candidate))
                .body(createMethodBody(candidate.getStatements()))
                .statements(candidate.getStatements())
                .build();
    }

    private List<String> createKeywords(List<String> methodKeywords) {
        List<String> keywords = methodKeywords.stream()
                .filter(this::isNotAnnotationAndModifier)
                .collect(Collectors.toList());
        keywords.add(FIRST_INDEX, METHOD_PRIVATE_MODIFIER);

        return keywords;
    }

    private Boolean isNotAnnotationAndModifier(String keywords) {
        return !keywords.startsWith(AT) && !REMOVED_MODIFIERS.contains(keywords);
    }

    private String createReturnType(Candidate candidate) {
        String returnType = VOID_RETURN_TYPE;

        if (candidate.getReturnType() != null) {
            returnType = candidate.getReturnType().getType();
        }

        return returnType;
    }

    private String createMethodBody(List<StatementModel> statements) {
        StringBuilder body = new StringBuilder();
        AtomicInteger depth = new AtomicInteger();

        buildMethodBody(statements, depth, body);

        return body.toString().trim();
    }

    private void buildMethodBody(List<StatementModel> statements,
                                 AtomicInteger depth, StringBuilder body) {
        depth.incrementAndGet();

        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            appendStatementToBody(statements.get(index), depth, body);
        }

        depth.decrementAndGet();
    }

    private void appendStatementToBody(StatementModel statementModel,
                                       AtomicInteger depth, StringBuilder body) {
        String statement = createStatementToAppend(statementModel, depth);
        body.append(statement);

        if (statementModel instanceof BlockModel) {
            appendBlockStatementToBody(statementModel, depth, body);
        }
    }

    private String createStatementToAppend(StatementModel statementModel,
                                           AtomicInteger depth) {
        StringBuilder statement = new StringBuilder();
        appendTabsToStatement(statement, depth);

        statement.append(statementModel.getStatement());
        statement.append(NEW_LINE);

        return statement.toString();
    }

    private void appendTabsToStatement(StringBuilder statement, AtomicInteger depth) {
        for (int i = FIRST_INDEX; i <= depth.get(); i++) {
            statement.append(TAB);
        }
    }

    private void appendBlockStatementToBody(StatementModel statementModel,
                                            AtomicInteger depth, StringBuilder body) {
        BlockModel blockModel = (BlockModel) statementModel;

        buildMethodBody(blockModel.getStatements(), depth, body);
        appendEndOfBlockStatement(blockModel, depth, body);
    }

    private void appendEndOfBlockStatement(BlockModel blockModel, AtomicInteger depth,
                                           StringBuilder body) {
        String statement = createStatementToAppend(blockModel.getEndOfBlockStatement(), depth);
        body.append(statement);
    }

    private List<String> createExceptions(MethodModel methodModel, Candidate candidate) {
        Integer firstCandidateStatementIndex = candidate.getStatements()
                .get(FIRST_INDEX)
                .getIndex();
        List<StatementModel> methodStatements = CandidateHelper.mergeAllStatements(methodModel.getStatements());
        StatementModel statementModel = CandidateHelper.searchStatementByIndex(
                methodModel.getStatements(), firstCandidateStatementIndex);

        return searchException(methodStatements, methodModel, statementModel);
    }

    private List<String> searchException(List<StatementModel> methodStatements,
                                         MethodModel methodModel, StatementModel statementModel) {
        List<String> exceptions = new ArrayList<>(methodModel.getExceptions());
        Integer catchStatementArrayIndex = getCatchStatementArrayIndex(
                methodStatements, methodModel, statementModel);

        if (!catchStatementArrayIndex.equals(INVALID_INDEX)) {
            CheckExceptionVA checkExceptionVA = CheckExceptionVA.builder()
                    .catchStatementArrayIndex(catchStatementArrayIndex)
                    .methodModel(methodModel)
                    .statementModel(statementModel)
                    .methodStatements(methodStatements)
                    .exceptions(exceptions)
                    .build();

            checkException(checkExceptionVA);
        }

        return exceptions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private Integer getCatchStatementArrayIndex(List<StatementModel> methodStatements,
                                                MethodModel methodModel, StatementModel statementModel) {
        int statementIndex = CandidateHelper.searchIndexOfStatements(methodStatements, statementModel) -
                SECOND_INDEX;
        Integer catchStatementArrayIndex = INVALID_INDEX;

        for (int index = statementIndex; index >= FIRST_INDEX; index--) {
            StatementModel methodStatementModel = methodStatements.get(index);

            if (checkContainCatchBlock(methodModel, statementModel, methodStatementModel)) {
                catchStatementArrayIndex = getEndStatementArrayIndex((BlockModel) methodStatementModel,
                        methodModel.getStatements(), methodStatements);
                break;
            }
        }

        return catchStatementArrayIndex;
    }

    private Boolean checkContainCatchBlock(MethodModel methodModel, StatementModel statementModel,
                                           StatementModel methodStatementModel) {
        if (methodStatementModel instanceof BlockModel) {
            Integer endIndex = ((BlockModel) methodStatementModel).getEndOfBlockStatement().getIndex();
            StatementModel endBlockStatement = CandidateHelper.searchStatementByIndex(
                    methodModel.getStatements(), endIndex);

            return isContainValidCatchBlock(endIndex, statementModel, endBlockStatement);
        } else {
            return Boolean.FALSE;
        }
    }

    private Boolean isContainValidCatchBlock(Integer endIndex, StatementModel statementModel,
                                             StatementModel endBlockStatement) {
        return endIndex > statementModel.getIndex() && isCatchBlock(endBlockStatement);
    }

    private Boolean isCatchBlock(StatementModel statementModel) {
        return statementModel.getStatement() != null &&
                CandidateHelper.isMatchRegex(statementModel.getStatement(), CATCH_REGEX);
    }

    private Integer getEndStatementArrayIndex(BlockModel blockModel, List<StatementModel> statements,
                                              List<StatementModel> methodStatements) {
        StatementModel foundStatement = CandidateHelper.searchStatementByIndex(
                statements, blockModel.getEndOfBlockStatement().getIndex());

        if (foundStatement != null) {
            return CandidateHelper.searchIndexOfStatements(methodStatements, foundStatement);
        } else {
            return INVALID_INDEX;
        }
    }

    private void checkException(CheckExceptionVA checkExceptionVA) {
        StatementModel catchBlockStatement = checkExceptionVA.getMethodStatements()
                .get(checkExceptionVA.getCatchStatementArrayIndex());

        saveException(catchBlockStatement, checkExceptionVA);
    }

    private void saveException(StatementModel catchBlockStatement, CheckExceptionVA checkExceptionVA) {
        checkExceptionVA.getMethodModel()
                .getLocalVariables()
                .stream()
                .filter(variablePropertyModel ->
                        variablePropertyModel.getStatementIndex()
                                .equals(catchBlockStatement.getIndex()))
                .forEach(variablePropertyModel ->
                        checkExceptionVA.getExceptions().add(variablePropertyModel.getType()));

        if (checkContainCatchBlock(checkExceptionVA.getMethodModel(),
                checkExceptionVA.getStatementModel(), catchBlockStatement)) {
            Integer endStatementArrayIndex = getEndStatementArrayIndex((BlockModel) catchBlockStatement,
                    checkExceptionVA.getMethodModel().getStatements(), checkExceptionVA.getMethodStatements());
            checkExceptionVA.setCatchStatementArrayIndex(endStatementArrayIndex);
            checkException(checkExceptionVA);
        }
    }

    private MethodModel createRemainingMethodModel(MethodModel methodModel,
                                                   MethodModel extractedMethodModel) {
        BlockModel methodBlock = CandidateHelper.getMethodBlockStatement(methodModel.getStatements());
        BlockModel candidateBlock = CandidateHelper.getMethodBlockStatement(extractedMethodModel.getStatements());
        BlockModel remainingBlock = CandidateHelper.getRemainingBlockModel(methodBlock, candidateBlock);

        addCallExtractedMethodStatement(remainingBlock.getStatements(), extractedMethodModel);

        return MethodModel.builder()
                .keywords(methodModel.getKeywords())
                .returnType(methodModel.getReturnType())
                .name(methodModel.getName())
                .parameters(methodModel.getParameters())
                .exceptions(methodModel.getExceptions())
                .body(createMethodBody(remainingBlock.getStatements()))
                .statements(remainingBlock.getStatements())
                .build();
    }

    private void addCallExtractedMethodStatement(List<StatementModel> statements,
                                                 MethodModel extractedMethodModel) {
        Integer extractedStatementIndex = extractedMethodModel.getStatements()
                .get(FIRST_INDEX)
                .getIndex();

        AddCallExtractedMethodVA addCallExtractedMethodVA = AddCallExtractedMethodVA.builder()
                .statements(statements)
                .extractedMethodModel(extractedMethodModel)
                .extractedStatementIndex(extractedStatementIndex)
                .extractedStatements(statements)
                .statementArrayIndex(INVALID_INDEX)
                .build();

        if (extractedStatementIndex > FIRST_INDEX) {
            addCallExtractedMethodVA.setExtractedStatementIndex(extractedStatementIndex - SECOND_INDEX);
            doAddCallExtractedMethodStatement(addCallExtractedMethodVA);
        }

        writeCallExtractedMethodStatement(addCallExtractedMethodVA);
    }

    private void doAddCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        List<StatementModel> statements = addCallExtractedMethodVA.getStatements();

        for (int index = FIRST_INDEX; index < statements.size(); index++) {
            checkStatementModel(index, statements, addCallExtractedMethodVA);

            if (!addCallExtractedMethodVA.getStatementArrayIndex().equals(INVALID_INDEX)) {
                break;
            }
        }
    }

    private void checkStatementModel(Integer index, List<StatementModel> statements,
                                     AddCallExtractedMethodVA addCallExtractedMethodVA) {
        StatementModel statementModel = statements.get(index);

        if (addCallExtractedMethodVA.getExtractedStatementIndex()
                .equals(statementModel.getIndex())) {
            addCallExtractedMethodVA.setExtractedStatements(statements);
            addCallExtractedMethodVA.setStatementArrayIndex(index);
        }

        if (isSearchBlock(statementModel, addCallExtractedMethodVA)) {
            addCallExtractedMethodVA.setStatements(((BlockModel) statementModel).getStatements());
            doAddCallExtractedMethodStatement(addCallExtractedMethodVA);
        }
    }

    private Boolean isSearchBlock(StatementModel statementModel,
                                  AddCallExtractedMethodVA addCallExtractedMethodVA) {
        return statementModel instanceof BlockModel &&
                addCallExtractedMethodVA.getStatementArrayIndex().equals(INVALID_INDEX);
    }

    private void writeCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        AtomicInteger callExtractedMethodIndex = new AtomicInteger(
                addCallExtractedMethodVA.getStatementArrayIndex() + SECOND_INDEX);
        StatementModel callExtractedMethodStatementModel = createCallExtractedMethodStatement(
                addCallExtractedMethodVA);

        searchParentBlockStatement(callExtractedMethodIndex, addCallExtractedMethodVA);
        appendCallExtractedMethodStatement(callExtractedMethodIndex.get(),
                callExtractedMethodStatementModel, addCallExtractedMethodVA);
    }

    private StatementModel createCallExtractedMethodStatement(AddCallExtractedMethodVA addCallExtractedMethodVA) {
        String statement = addCallExtractedMethodVA.getExtractedMethodModel().getName() +
                createMethodCallParameters(addCallExtractedMethodVA.getExtractedMethodModel()
                        .getParameters());

        return StatementModel.statementBuilder()
                .index(addCallExtractedMethodVA.getExtractedStatementIndex() + SECOND_INDEX)
                .statement(statement)
                .build();
    }

    private String createMethodCallParameters(List<PropertyModel> parameters) {
        Integer maxSize = parameters.size() - SECOND_INDEX;
        StringBuilder parameter = new StringBuilder(METHOD_PARAMETER_PREFIX);

        for (Integer index = FIRST_INDEX; index < parameters.size(); index++) {
            parameter.append(parameters.get(index).getName());

            if (!index.equals(maxSize)) {
                parameter.append(COMMA);
            }
        }

        parameter.append(METHOD_PARAMETER_SUFFIX);

        return parameter.toString();
    }

    private void searchParentBlockStatement(AtomicInteger callExtractedMethodIndex,
                                            AddCallExtractedMethodVA addCallExtractedMethodVA) {
        if (!FIRST_INDEX.equals(callExtractedMethodIndex.get())) {
            StatementModel statementModel = addCallExtractedMethodVA.getExtractedStatements()
                    .get(addCallExtractedMethodVA.getStatementArrayIndex());

            checkIfParentBlockStatement(callExtractedMethodIndex, statementModel, addCallExtractedMethodVA);
        }
    }

    private void checkIfParentBlockStatement(AtomicInteger callExtractedMethodIndex,
                                             StatementModel statementModel,
                                             AddCallExtractedMethodVA addCallExtractedMethodVA) {
        if (statementModel instanceof BlockModel) {
            checkIfFirstStatement(callExtractedMethodIndex, addCallExtractedMethodVA);
            addCallExtractedMethodVA.setExtractedStatements(((BlockModel) statementModel).getStatements());
        }
    }

    private void checkIfFirstStatement(AtomicInteger callExtractedMethodIndex,
                                       AddCallExtractedMethodVA addCallExtractedMethodVA) {
        if (addCallExtractedMethodVA.getExtractedStatementIndex().equals(FIRST_INDEX)) {
            callExtractedMethodIndex.set(FIRST_INDEX);
        }
    }

    private void appendCallExtractedMethodStatement(Integer callExtractedMethodIndex,
                                                    StatementModel callExtractedMethodStatementModel,
                                                    AddCallExtractedMethodVA addCallExtractedMethodVA) {
        List<StatementModel> statements = addCallExtractedMethodVA.getExtractedStatements();

        if (callExtractedMethodIndex > statements.size()) {
            callExtractedMethodIndex = FIRST_INDEX;
        }

        statements.add(callExtractedMethodIndex, callExtractedMethodStatementModel);
    }

    private Boolean replaceFile(String path, MethodModel methodModel, Candidate bestCandidate) {
        MethodModel candidateMethodModel = createMethodModelFromCandidate(methodModel, bestCandidate);
        MethodModel remainingMethodModel = createRemainingMethodModel(methodModel, candidateMethodModel);

        String target = methodModelHelper.createMethodRegex(methodModel);
        String replacement = createReplacementString(candidateMethodModel, remainingMethodModel);

        ReplaceFileVA replaceFileVA = ReplaceFileVA.builder()
                .filePath(path)
                .target(target)
                .replacement(replacement)
                .build();

        return replaceFileHelper.replaceFile(replaceFileVA);
    }

    private String createReplacementString(MethodModel candidateMethodModel,
                                           MethodModel remainingMethodModel) {
        String candidateMethod = methodModelHelper.createMethod(candidateMethodModel);
        String remainingMethod = methodModelHelper.createMethod(remainingMethodModel);

        candidateMethod = normalizeMethodString(candidateMethod);
        remainingMethod = normalizeMethodString(remainingMethod);

        return buildReplacementString(candidateMethod, remainingMethod);
    }

    private String normalizeMethodString(String method) {
        List<String> statements = new ArrayList<>(Arrays.asList(method.split(NEW_LINE)));
        int lastIndex = statements.size() - SECOND_INDEX;

        statements.set(SECOND_INDEX, TAB + statements.get(SECOND_INDEX));
        statements.set(lastIndex, TAB + statements.get(lastIndex));

        return String.join(NEW_LINE, statements);
    }

    private String buildReplacementString(String candidateMethod,
                                          String remainingMethod) {
        String replacementString = remainingMethod;

        replacementString += NEW_LINE;
        replacementString += NEW_LINE;
        replacementString += TAB;
        replacementString += candidateMethod;

        return replacementString;
    }
}
