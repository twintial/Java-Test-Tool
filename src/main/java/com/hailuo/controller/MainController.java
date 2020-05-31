package com.hailuo.controller;

import com.hailuo.compile.RunTimeJavaCompiler;
import com.hailuo.stage.BindStage;
import com.hailuo.stage.DialogStage;
import com.hailuo.util.CSVUtil;
import com.hailuo.util.TypeConvert;
import com.sun.deploy.util.StringUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    // 上传测试文件按键
    public Button uploadCSVFile;
    // 上传源代码文件
    public Button uploadJavaFile;
    // 开始测试按键
    public Button testButton;
    // 选择输出文件夹目录按键
    public Button chooseDir;

    // 选择的类
    public ChoiceBox<String> classes;
    // 选择的构造函数
    public ChoiceBox<Constructor<?>> constructors;
    // 选择的方法
    public ChoiceBox<Method> methods;

    // 代码文件路径
    public TextField uploadFilePath;
    // 测试文件路径
    public TextField testFilePath;
    // 输出到的文件夹目录
    public TextField outputDirPath;
    // 输出文件名
    public TextField outputFileName;

    public Button bindButton;

    // 统计数据
    public Label pass;
    public Label fail;
    public Label passRate;

    private Stage stage;
    public Pane root;

    private RunTimeJavaCompiler compiler;
    private String[] classStrs;

    // 构造函数的测试数据绑定索引
    private Integer[] consArgsIndex;
    // 被测方法的构造函数绑定索引
    private Integer[] methodArgsIndex;
    // 测试预期结果的绑定索引
    private int expectedResultIndex;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO (don't really need to do anything here).
        classes.getSelectionModel().selectedIndexProperty()
                .addListener(new ChangeListener<Number>() {
                    public void changed(ObservableValue ov, Number value, Number new_value) {
                        if (new_value.intValue() >= 0) {
                            // 构造函数
                            List<Constructor<?>> constructorList = compiler.getClassConsMap().get(classStrs[new_value.intValue()]);
                            constructors.setItems(FXCollections.observableArrayList(constructorList));
                            constructors.getSelectionModel().selectFirst();
                            // 方法
                            List<Method> methodList = compiler.getClassMethodsMap().get(classStrs[new_value.intValue()]);
                            methods.setItems(FXCollections.observableArrayList(methodList));
                            methods.getSelectionModel().selectFirst();
                        }
                    }
                });

    }

    private Stage getStage() {
        if (stage == null) {
            stage = (Stage) root.getScene().getWindow();
        }
        return stage;
    }

    // 上传代码文件
    public void uploadJavaFile(ActionEvent event) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择源码文件（java）");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Java File", "*.java");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(getStage());
        // 若选择了文件
        if (file != null) {
            // 编译
            compiler = new RunTimeJavaCompiler(file, file.getName().replace(".java", ""));
            boolean success = compiler.compile();
            if (success) {
                uploadFilePath.setText(file.toString());
                classStrs = compiler.getClassMap().keySet().toArray(new String[0]);
                classes.setItems(FXCollections.observableArrayList(classStrs));
                classes.getSelectionModel().selectFirst();
                uploadCSVFile.setDisable(false);
            } else {
                System.out.println(compiler.getCompilerMessage());
                DialogStage.showAlertDialog("编译错误", "编译错误", compiler.getCompilerMessage());
            }
        }
    }

    // 上传测试文件
    public void uploadTestFile(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择测试文件（csv）");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV File", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(getStage());
        if (file != null) {
            testFilePath.setText(file.toString());
            bindButton.setDisable(false);
            testButton.setDisable(false);

            // 弹出绑定窗口，如果直接关闭绑定窗口会产出bug，将来解决
            Constructor<?> constructor = constructors.getSelectionModel().getSelectedItem();
            Method method = methods.getSelectionModel().getSelectedItem();
            String[] headers = CSVUtil.getHeaders(testFilePath.getText());
            BindStage bindStage = new BindStage(root, this);
            bindStage.show(constructor, method, headers);
            root.setDisable(true);
        }
    }

    // 选择输出目录
    public void chooseOutputDir(ActionEvent event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("选择输出路径");
        File file = fileChooser.showDialog(getStage());
        if (file != null) {
            outputDirPath.setText(file.toString());
        }
    }

    // 开始测试
    public void startTest(ActionEvent event) {
        // 生成测试数据
        if (consArgsIndex == null || methodArgsIndex == null) {
            System.out.println("请绑定参数");
        }
        Constructor<?> testCons = constructors.getSelectionModel().getSelectedItem();
        Method testMethod = methods.getSelectionModel().getSelectedItem();
        testMethod.setAccessible(true);

        List<String[]> contents = CSVUtil.getContents(testFilePath.getText());
        if (contents == null) {
            System.out.println("测试文件不存在");
            return;
        }
        List<List<String>> results = new ArrayList<>();
        // 设置头部
        String[] headers = CSVUtil.getHeaders(testFilePath.getText());
        List<String> resultHeaders = new ArrayList<>(Arrays.asList(headers));
        resultHeaders.add("test_result");
        resultHeaders.add("is_pass");
        results.add(resultHeaders);
        // 调用函数进行测试，需要添加调用错误的提醒
        try {
            for (String[] row : contents) {
                Object[] consArgs = getArgsWithIndex(row, consArgsIndex, testCons.getGenericParameterTypes());
                Object[] methodArgs = getArgsWithIndex(row, methodArgsIndex, testMethod.getGenericParameterTypes());
                Object cons = testCons.newInstance(consArgs);
                Object testResult = testMethod.invoke(cons, methodArgs);
                // 处理结果
                List<String> new_row = new ArrayList<>(Arrays.asList(row));
                // 加入测试结果
                new_row.add(testResult.toString());
                // 判断是否通过
                Object expectedResult = TypeConvert.convertStr(row[expectedResultIndex], testMethod.getGenericReturnType());
                if (testResult.equals(expectedResult)) {
                    new_row.add("pass");
                } else {
                    new_row.add("fail");
                }
                results.add(new_row);
            }
        } catch (Exception e) {
            e.printStackTrace();
            DialogStage.showAlertDialog("测试出错", "测试出错", e.toString());
            return;
        }
        // 统计, 减1是减去头部
        long passNum = results.stream().filter(list -> list.get(list.size() - 1).equals("pass")).count();
        long failNum = results.size() - 1 - passNum;
        double rate = ((double) passNum)/(results.size() - 1);
        pass.setText(String.valueOf(passNum));
        fail.setText(String.valueOf(failNum));
        passRate.setText(String.format("%.2f", rate * 100) + "%");
        // 暂定
        if (!outputDirPath.getText().equals("")) {
            String fileName;
            if (outputFileName.getText().equals("")) {
                LocalDateTime now = LocalDateTime.now();
                fileName = now.format(DateTimeFormatter.ISO_DATE_TIME) + ".csv";
            } else {
                fileName = outputFileName.getText() + ".csv";
            }
            CSVUtil.writeContents(outputDirPath.getText() + File.separator + fileName, results);
            DialogStage.showInfoDialog("测试成功", "测试成功", String.format("结果文件%s已生成"
                    , outputDirPath.getText() + File.separator + fileName));
        } else {
            DialogStage.showInfoDialog("测试成功", "测试成功", "未选择输出目录，没有生成结果文件");
        }
    }
    private Object[] getArgsWithIndex(String[] content, Integer[] indexes, Type[] types) throws Exception {
        Object[] args = new Object[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            args[i] = TypeConvert.convertStr(content[indexes[i]], types[i]);
        }
        return args;
    }

    // 绑定参数映射
    public void bindArgs(ActionEvent event) {

        Constructor<?> constructor = constructors.getSelectionModel().getSelectedItem();
        if (constructor == null) {
            System.out.println("选择cons");
            return;
        }
        Method method = methods.getSelectionModel().getSelectedItem();
        if (method == null) {
            System.out.println("选择method");
            return;
        }

        String[] headers = CSVUtil.getHeaders(testFilePath.getText());
        BindStage bindStage = new BindStage(root, this);
        bindStage.show(constructor, method, headers);
        root.setDisable(true);
    }

    public Integer[] getConsArgsIndex() {
        return consArgsIndex;
    }

    public Integer[] getMethodArgsIndex() {
        return methodArgsIndex;
    }

    public void setConsArgsIndex(Integer[] consArgsIndex) {
        this.consArgsIndex = consArgsIndex;
    }

    public void setMethodArgsIndex(Integer[] methodArgsIndex) {
        this.methodArgsIndex = methodArgsIndex;
    }

    public Integer getExpectedResultIndex() {
        return expectedResultIndex;
    }

    public void setExpectedResultIndex(Integer expectedResultIndex) {
        this.expectedResultIndex = expectedResultIndex;
    }
}
