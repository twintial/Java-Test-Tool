package com.hailuo.util;

import javax.management.RuntimeErrorException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TypeConvert {

    static public Object convertStr(String srt, Type type) throws Exception {
        String typeName = type.getTypeName();
        switch (typeName) {
            case "java.lang.Character":
            case "char":
                return srt.charAt(0);
            case "java.lang.Integer":
            case "int":
                return Integer.parseInt(srt);
            case "java.lang.Long":
            case "long":
                return Long.parseLong(srt);
            case "java.lang.Short":
            case "short":
                return Short.parseShort(srt);
            case "java.lang.Float":
            case "float":
                return Float.parseFloat(srt);
            case "java.lang.Double":
            case "double":
                return Double.parseDouble(srt);
            case "java.lang.Boolean":
            case "boolean":
                return Boolean.parseBoolean(srt);
            case "java.util.Date":
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                return format.parse(srt);
        }
        throw new Exception("无法解析的类型，只支持8个基本类型和Date类型");
    }
}
