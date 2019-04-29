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
                .statement("Rectangle2D r = super . getFigDrawBounds ();")
                .startIndex(8)
                .endIndex(51)
                .build();
    }

    private StatementModel createSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createSecondBlockStatements())
                .endOfBlockStatement(createSecondBlockEndStatement())
                .build();
        blockModel.setStatement("if ( getNodeCount () > 1) {");
        blockModel.setStartIndex(64);
        blockModel.setEndIndex(90);

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
        blockModel.setStatement("if ( START .get( this ) != null ) {");
        blockModel.setStartIndex(107);
        blockModel.setEndIndex(141);

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
                .statement("Point p1 = getPoint (0, 0);")
                .startIndex(162)
                .endIndex(188)
                .build();
    }

    private StatementModel createThirdBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2 = getPoint (1, 0);")
                .startIndex(209)
                .endIndex(235)
                .build();
    }

    private StatementModel createThirdBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add ( START .get ( this ). getBounds (p1 , p2 ));")
                .startIndex(256)
                .endIndex(306)
                .build();
    }

    private StatementModel createThirdBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(323)
                .endIndex(323)
                .build();
    }

    private StatementModel createSecondBlockSecondStatement() {
        BlockModel blockModel = BlockModel.blockBuilder()
                .statements(createFourthBlockStatements())
                .endOfBlockStatement(createFourthBlockEndStatement())
                .build();
        blockModel.setStatement("if ( END .get ( this ) != null ) {");
        blockModel.setStartIndex(341);
        blockModel.setEndIndex(374);

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
                .statement("Point p1= getPoint ( getNodeCount ()-1 , 0);")
                .startIndex(395)
                .endIndex(438)
                .build();
    }

    private StatementModel createFourthBlockSecondStatement() {
        return StatementModel.statementBuilder()
                .statement("Point p2= getPoint ( getNodeCount ()-2 , 0);")
                .startIndex(459)
                .endIndex(502)
                .build();
    }

    private StatementModel createFourthBlockThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("r.add (END. get ( this ). getBounds (p1 , p2 ));")
                .startIndex(523)
                .endIndex(570)
                .build();
    }

    private StatementModel createFourthBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(587)
                .endIndex(587)
                .build();
    }

    private StatementModel createSecondBlockEndStatement() {
        return StatementModel.statementBuilder()
                .statement("}")
                .startIndex(600)
                .endIndex(600)
                .build();
    }

    private StatementModel createThirdStatement() {
        return StatementModel.statementBuilder()
                .statement("return r;")
                .startIndex(614)
                .endIndex(622)
                .build();
    }
}