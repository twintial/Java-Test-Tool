package com.hailuo.compile;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RunTimeJavaCompiler {
    private String sourceCode;
    private String publicClassName;

    // 存放编译错误信息
    private DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
    // 存放编译之后的字节码，<类名，字节码>
    private Map<String, ByteJavaFileObject> javaFileObjectMap = new ConcurrentHashMap<>();
    // 定义编译器
    private JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();


    private Map<String, Class<?>> classMap = new ConcurrentHashMap<>();
    private Map<String, List<Method>> classMethodsMap = new ConcurrentHashMap<>();
    private Map<String, List<Constructor<?>>> classConsMap = new ConcurrentHashMap<>();
    /**
     * 构造函数
     *
     * @param sourceCode 要编译的源码
     */
    public RunTimeJavaCompiler(String sourceCode) {
        this.sourceCode = sourceCode;
        this.publicClassName = getPublicClassName(sourceCode);
    }

    public RunTimeJavaCompiler(File javaFile) throws IOException {
        this.sourceCode = getSourceCode(javaFile);
        this.publicClassName = getPublicClassName(sourceCode);
    }

    public RunTimeJavaCompiler(String sourceCode, String publicClassName) {
        this(sourceCode);
        this.publicClassName = publicClassName;
    }

    private String getSourceCode(File javaFile) throws IOException {
        // String path = "src/main/resources/A.java";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s).append("\n");
            }
        }
        return sb.toString();
    }

    private String getSourceCode(String javaFilePath) throws IOException {
        return getSourceCode(new File(javaFilePath));
    }

    /**
     * @return 编译成功返回true，失败false
     */
    public boolean compile() {
//        // 编译参数
//        List<String> options = new ArrayList<>(Arrays.asList("-d", "target/classes"));
        // 自定义内容管理器
        StandardJavaFileManager sfm = cmp.getStandardFileManager(diagnosticCollector, null, StandardCharsets.UTF_8);
        JavaFileManager jfm = new StringJavaFileManage(sfm);
        // java源码对象
        JavaFileObject jfo = new StringJavaFileObject(publicClassName, sourceCode);
        // 编译任务
        JavaCompiler.CompilationTask task = cmp.getTask(null, jfm,
                diagnosticCollector, null, null, Collections.singletonList(jfo));

        Boolean success = task.call();
        // 编译结果处理
        if (success) {
            StringClassLoader loader = new StringClassLoader();
            javaFileObjectMap.forEach((k, v) -> {
                Class<?> aClass = loader.findClass(k);
                classMap.put(aClass.getSimpleName(), aClass);
                // 需要去除lambda表达式
                List<Method> methodList = Arrays.stream(aClass.getDeclaredMethods())
                        .filter(method -> !method.toString().contains("lambda"))
                        .collect(Collectors.toList());
                classMethodsMap.put(aClass.getSimpleName(), methodList);
//                System.out.println(Arrays.toString(aClass.getDeclaredConstructors()));
//                System.out.println(Arrays.toString(aClass.getConstructors()));
                classConsMap.put(aClass.getSimpleName(), Arrays.asList(aClass.getDeclaredConstructors()));
            });
        }
        return success;
    }

    private String getPublicClassName(String sourceCode) {
        // 会存在一些问题，大多数情况下可用
        Pattern pattern = Pattern.compile("\n\\s*public\\s+class\\s+(\\S+)\\s*\\{");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * @return 获得编译错误信息
     */
    public String getCompilerMessage() {
        List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticCollector.getDiagnostics();
        return diagnostics.stream().map(diagnostic -> diagnostic.toString() + "\r\n").collect(Collectors.joining());
    }

    public Map<String, Class<?>> getClassMap() {
        return classMap;
    }

    public Map<String, List<Method>> getClassMethodsMap() {
        return classMethodsMap;
    }

    public Map<String, List<Constructor<?>>> getClassConsMap() {
        return classConsMap;
    }

    /**
     * 控制编译后的字节码输出位置
     */
    private class StringJavaFileManage extends ForwardingJavaFileManager<JavaFileManager> {

        protected StringJavaFileManage(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            ByteJavaFileObject javaFileObject = new ByteJavaFileObject(className, kind);
            javaFileObjectMap.put(className, javaFileObject);
            return javaFileObject;
        }
    }

    private class StringClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) {
            ByteJavaFileObject fileObject = javaFileObjectMap.get(name);
            if (fileObject != null) {
                byte[] bytes = fileObject.getCompiledBytes();
                return defineClass(name, bytes, 0, bytes.length);
            }
            return null;
        }
    }
}

/**
 * 自定义一个字符串的源码对象
 */
class StringJavaFileObject extends SimpleJavaFileObject {

    // 待编译的代码
    private String sourceCode;

    public StringJavaFileObject(String className, String sourceCode) {
        super(URI.create("String:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), Kind.SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return sourceCode;
    }
}

/**
 * 自定义一个编译之后的字节码对象
 */
class ByteJavaFileObject extends SimpleJavaFileObject {

    // 存放编译后的字节码
    private ByteArrayOutputStream outputStream;

    public ByteJavaFileObject(String className, Kind kind) {
        super(URI.create("string:///" + className.replaceAll("\\.", "/") + Kind.SOURCE.extension), kind);
    }

    // StringJavaFileManage 编译之后的字节码输出会调用该方法（该字节码输出到outputStream）
    @Override
    public OutputStream openOutputStream() {
        outputStream = new ByteArrayOutputStream();
        return outputStream;
    }

    // 在类加载加载的时候需要用到
    public byte[] getCompiledBytes() {
        return outputStream.toByteArray();
    }
}

