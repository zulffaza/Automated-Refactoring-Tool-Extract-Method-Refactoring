package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.model.Candidate;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import lombok.NonNull;

import java.util.List;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 25 June 2019
 */

public interface CandidateAnalysis {

    List<Candidate> analysis(@NonNull MethodModel methodModel);
}
