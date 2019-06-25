package com.finalproject.automated.refactoring.tool.extract.method.refactoring.model;

import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author Faza Zulfika P P
 * @version 1.0.0
 * @since 25 June 2019
 */

@Data
@Builder
public class GetFilteredRawVariablesVA {

    private MethodModel methodModel;

    private BlockModel remainingBlock;

    private Integer indexFilter;

    private List<List<String>> filteredRawVariables;
}
