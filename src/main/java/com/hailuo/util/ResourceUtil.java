package com.hailuo.util;

import java.net.URL;

public class ResourceUtil {

    public static URL getFxml (String fileName) {
        return ResourceUtil.class.getClassLoader().getResource(fileName);
    }
}
