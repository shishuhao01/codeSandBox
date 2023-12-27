package com.shishuhao.security;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());

        List<String> strings = FileUtil.readLines("D:\\Grade3\\Myself\\java\\SpringCloudProject\\code-SandBox\\src\\main\\resources\\application.yml", StandardCharsets.UTF_8);
        System.out.println(strings);

        FileUtil.writeString("aa","Aaa",StandardCharsets.UTF_8);
    }
}
