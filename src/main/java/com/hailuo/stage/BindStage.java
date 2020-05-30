package com.hailuo.stage;

import com.hailuo.controller.MainController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class BindStage {

    private Stage stage;

    private Pane parentPane;

    private MainController mc;

    private List<ChoiceBox<String>> consArgsList;
    private List<ChoiceBox<String>> methodArgsList;
    private ChoiceBox<String> expectedResultBox;

    public BindStage(Pane parentPane, MainController mainController) {
        consArgsList = new ArrayList<>();
        methodArgsList = new ArrayList<>();
        stage = new Stage();
        stage.setTitle("参数绑定");
        this.parentPane = parentPane;
        mc = mainController;
        stage.setOnCloseRequest(event -> parentPane.setDisable(false));
    }

    private void setConfirmButton(Pane pane) {
        Button confirm = new Button("确定");
        pane.getChildren().add(confirm);
        confirm.setOnAction(event -> {
            // 绑定
            Integer[] consArgs = new Integer[consArgsList.size()];
            Integer[] methodsArgs = new Integer[methodArgsList.size()];
            for (int i = 0; i < consArgs.length; i++) {
                consArgs[i] = consArgsList.get(i).getSelectionModel().getSelectedIndex();
            }
            mc.setConsArgsIndex(consArgs);

            for (int i = 0; i < methodsArgs.length; i++) {
                methodsArgs[i] = methodArgsList.get(i).getSelectionModel().getSelectedIndex();
            }
            mc.setMethodArgsIndex(methodsArgs);

            mc.setExpectedResultIndex(expectedResultBox.getSelectionModel().getSelectedIndex());

            stage.close();
            parentPane.setDisable(false);
        });
    }

    private ChoiceBox<String> getChoiceBox(String[] contents) {
        ChoiceBox<String> choiceBox = new ChoiceBox<>(FXCollections.observableArrayList(contents));
        return choiceBox;
    }

    public void show(Constructor<?> constructor, Method method, String[] headers) {
        VBox root = new VBox();
        root.setPadding(new Insets(15));
        root.setSpacing(15);

        // 构造函数
        Label consLabel = new Label("构造函数：" + constructor.toString());
        root.getChildren().add(consLabel);
        Parameter[] constructorParameters = constructor.getParameters();
        for (int i = 0; i < constructorParameters.length; i++) {
            Label param = new Label(constructorParameters[i].toString());
            param.setPadding(new Insets(5, 100, 0, 0));
            ChoiceBox<String> headerBox = getChoiceBox(headers);
            // 暂时先这样写
            try {
                headerBox.getSelectionModel().select(mc.getConsArgsIndex()[i]);
            } catch (Exception e) {
                headerBox.getSelectionModel().selectFirst();
            }
            consArgsList.add(headerBox);
            root.getChildren().add(new HBox(param, headerBox));
        }

        // 被测方法
        Label methodLabel = new Label("被测方法：" + method.toString());
        root.getChildren().add(methodLabel);
        Parameter[] methodParameters = method.getParameters();
        for (int i = 0; i < methodParameters.length; i++) {
            Label param = new Label(methodParameters[i].toString());
            param.setPadding(new Insets(5, 100, 0, 0));
            ChoiceBox<String> headerBox = getChoiceBox(headers);
            // 暂时先这样写
            try {
                headerBox.getSelectionModel().select(mc.getMethodArgsIndex()[i]);
            } catch (Exception e) {
                headerBox.getSelectionModel().selectFirst();
            }
            methodArgsList.add(headerBox);
            root.getChildren().add(new HBox(param, headerBox));
        }

        // 预期结果
        Label param = new Label("预期结果：");
        param.setPadding(new Insets(5, 100, 0, 0));
        expectedResultBox = getChoiceBox(headers);
        expectedResultBox.getSelectionModel().select(mc.getExpectedResultIndex());
        root.getChildren().add(new HBox(param, expectedResultBox));

        // 确认按键
        setConfirmButton(root);
        Scene secondScene = new Scene(root);
        stage.setScene(secondScene);
        stage.show();
    }
}
