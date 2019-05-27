package com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.implementation;

import com.finalproject.automated.refactoring.tool.extract.method.refactoring.service.ExtractMethod;
import com.finalproject.automated.refactoring.tool.model.BlockModel;
import com.finalproject.automated.refactoring.tool.model.MethodModel;
import com.finalproject.automated.refactoring.tool.model.StatementModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fazazulfikapp
 * @version 1.0.0
 * @since 26 April 2019
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ExtractMethodImplTest {

    @Autowired
    private ExtractMethod extractMethod;

    @Test
    public void test() {
        MethodModel methodModel = MethodModel.builder()
                .statements(createExpectedStatements())
                .build();

        extractMethod.refactoring(methodModel);
    }

    private List<StatementModel> createExpectedStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createFirstStatement());
        statements.add(createSecondStatement());
        statements.add(createThirdStatement());

        return statements;
    }

    private StatementModel createFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Rectangle2D r = super.getFigDrawBounds();")
                .startIndex(8)
                .endIndex(48) // -3
                .build();
    }

    private StatementModel createSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createSecondBlockStatements())
                .endOfBlockStatement(createSecondBlockEndStatement())
                .build();
        blockModel.setStatement("if (getNodeCount() > 1) {");
        blockModel.setStartIndex(61); // -3
        blockModel.setEndIndex(85); // -5

        return blockModel;
    }

    private List<StatementModel> createSecondBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createSecondBlockFirstStatement());
        statements.add(createSecondBlockSecondStatement());

        return statements;
    }

    private StatementModel createSecondBlockFirstStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createThirdBlockStatements())
                .endOfBlockStatement(createThirdBlockEndStatement())
                .build();
        blockModel.setStatement("if (START.get(this) != null) {");
        blockModel.setStartIndex(102); // -5
        blockModel.setEndIndex(131); // -10

        return blockModel;
    }

    private List<StatementModel> createThirdBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createThirdBlockFirstStatement());
        statements.add(createThirdBlockSecondStatement());
        statements.add(createThirdBlockThirdStatement());

        return statements;
    }

    private StatementModel createThirdBlockFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p1 = getPoint(0, 0);")
                .startIndex(152) // -10
                .endIndex(177) // -11
                .build();
    }

    private StatementModel createThirdBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint(1, 0);")
                .startIndex(198) // -11
                .endIndex(223) // -12
                .build();
    }

    private StatementModel createThirdBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add(START.get(this).getBounds(p1, p2));")
                .startIndex(256) // -12
                .endIndex(284) // -22
                .build();
    }

    private StatementModel createThirdBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(301) // -22
                .endIndex(301) // -22
                .build();
    }

    private StatementModel createSecondBlockSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createFourthBlockStatements())
                .endOfBlockStatement(createFourthBlockEndStatement())
                .build();
        blockModel.setStatement("if (END.get(this) != null) {");
        blockModel.setStartIndex(319); // -22
        blockModel.setEndIndex(346); // -28

        return blockModel;
    }

    private List<StatementModel> createFourthBlockStatements() {
        List<StatementModel> statements = new ArrayList<>();

        statements.add(createFourthBlockFirstStatement());
        statements.add(createFourthBlockSecondStatement());
        statements.add(createFourthBlockThirdStatement());

        return statements;
    }

    private StatementModel createFourthBlockFirstStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p1 = getPoint(getNodeCount() - 1, 0);")
                .startIndex(367) // -28
                .endIndex(409) // -29
                .build();
    }

    private StatementModel createFourthBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint(getNodeCount() - 2, 0);")
                .startIndex(430) // -29
                .endIndex(472) // -30
                .build();
    }

    private StatementModel createFourthBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add(END.get(this).getBounds(p1, p2));")
                .startIndex(493) // -30
                .endIndex(531) // -39
                .build();
    }

    private StatementModel createFourthBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(548) // -39
                .endIndex(548) // -39
                .build();
    }

    private StatementModel createSecondBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(561) // -39
                .endIndex(561) // -39
                .build();
    }

    private StatementModel createThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("return r;")
                .startIndex(575) // -39
                .endIndex(583) // -39
                .build();
    }
}