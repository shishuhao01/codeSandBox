package com.shishuhao;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.shishuhao.model.ExecuteCodeRequest;
import com.shishuhao.model.ExecuteCodeResponse;
import com.shishuhao.model.ExecuteMessage;
import com.shishuhao.model.JudgeInfo;
import com.shishuhao.utils.ProcessUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class JavaNativeCodeSandboxOld implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final List<String> blackList = Arrays.asList("Files","exec");
    private static final String SECURITY_MANAGER_PATH = "D:\\Grade3\\Myself\\java\\SpringCloudProject\\code-SandBox\\src\\main\\resources\\security";
    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

   private static final WordTree WORD_TREE;

   static {
       //校验代码
       WORD_TREE = new WordTree();
       WORD_TREE.addWords(blackList);

   }

    public static void main(String[] args) {
        JavaNativeCodeSandboxOld javaNativeCodeSandbox = new JavaNativeCodeSandboxOld();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("","",""));

//        1.恶意睡眠 || 超时占用服务器资源
//        String code = ResourceUtil.readStr("unsafeCode/Sleep.java", StandardCharsets.UTF_8);


//         2.恶意占用系统存储资源
        String code = ResourceUtil.readStr("unsafeCode/MemoryError.java", StandardCharsets.UTF_8);


//         3.读取系统资源
//        String code = ResourceUtil.readStr("unsafeCode/readFileError.java", StandardCharsets.UTF_8);


//        向系统其他文件写入程序
//        String code = ResourceUtil.readStr("unsafeCode/WriteFileError.java", StandardCharsets.UTF_8);


//        程序中运行其他程序
//        String code = ResourceUtil.readStr("unsafeCode/RunFileError.java", StandardCharsets.UTF_8);


        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");

        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
//        核心思路
//        使用Process进程管理类
//          System.setSecurityManager(new MySecurityManager());
//        1.把用户的代码保存为文件
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        //校验代码
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if (foundWord != null) {
//            System.out.println(foundWord.getFoundWord());
//            System.out.println("包含禁止词语");
//            return null;
//        }

        List<String> result = inputList.stream()
                .map(s -> s.replaceAll(",", " ")) // 去除逗号
                .collect(Collectors.toList());
        System.out.println(result);
        //判断全局代码目录是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            File mkdir = FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        //获取文件路径
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        // 2.编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.RunProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getResponse(e);
        }


        //3.执行代码，得到输出结果
        // 每一条都有一个输出
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : result) {
            //限制内存  限制权限
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath,SECURITY_MANAGER_PATH,SECURITY_MANAGER_CLASS_NAME, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(() -> {
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.RunProcessAndGetMessage(runProcess, "运行");

             executeMessageList.add(executeMessage);
            } catch (Exception e) {
                return getResponse(e);
            }
        }


        //4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        //取最大值，判断是否超时
        long maxTime = 0;

        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getMaxTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        if (outputList.size() == executeMessageList.size()) {
            //正常执行
            executeCodeResponse.setStatus(1);
        }

        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        //最大时间
        judgeInfo.setTime(maxTime);
        //或得当前程序内存
//        judgeInfo.setMemoryLimit();

        executeCodeResponse.setJudgeInfo(judgeInfo);

//        5.文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean a = FileUtil.del(userCodeParentPath);
            System.out.println("删除"+(a?"成功":"失败"));
        }

//        6.错误处理

        return executeCodeResponse;
    }


    private ExecuteCodeResponse getResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage("编译错误");
        judgeInfo.setMemoryLimit(0L);
        judgeInfo.setTime(0L);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }
}
