package com.shishuhao;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.shishuhao.model.ExecuteCodeRequest;
import com.shishuhao.model.ExecuteCodeResponse;
import com.shishuhao.model.ExecuteMessage;
import com.shishuhao.model.JudgeInfo;
import com.shishuhao.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
public abstract class JavaCodeSandBoxTemplate implements CodeSandBox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    private static final long TIME_OUT = 3000;
    private static final List<String> blackList = Arrays.asList("Files", "exec","delete");
    private static final WordTree WORD_TREE;
    static {
        //校验代码
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);

    }
//    private static final String SECURITY_MANAGER_PATH = "D:\\Grade3\\Myself\\java\\SpringCloudProject\\code-SandBox\\src\\main\\resources\\security";
//    private static final String SECURITY_MANAGER_CLASS_NAME = "MySecurityManager";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

//        0.获取题目请求信息,判断代码操作是否合法
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        boolean legal = isLegal(code);
        if (!legal) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setMessage("不合法");
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("危险操作");
            executeCodeResponse.setJudgeInfo(judgeInfo);
            return executeCodeResponse;
        }


        //1.保存用户代码
        File userCodeFile = saveCodeFile(code);

        //2.对题目的输入进行处理成可编译的参数
        List<String> result = executeCharacter(inputList);

        //对用户代码进行编译
        //3.编译代码，得到class文件
        ExecuteMessage compileFileMessage = compileFile(userCodeFile);
        if (compileFileMessage.getMessage().equals("编译错误")) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("编译错误");
            executeCodeResponse.setJudgeInfo(judgeInfo);
            return executeCodeResponse;
        }


        //3.执行代码，得到输出结果列表
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, result);
        ExecuteMessage executeMessage = executeMessageList.get(executeMessageList.size() - 1);
        if (executeMessage.getMessage().equals("超时")) {
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            JudgeInfo judgeInfo = new JudgeInfo();
            judgeInfo.setMessage("程序运行时异常");
            executeCodeResponse.setJudgeInfo(judgeInfo);
            return executeCodeResponse;
        }


        //4.收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);


        //5.删除文件
        boolean b = deleteFile(userCodeFile);
        if (b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }

        //6.返回结果
        return outputResponse;
    }

    public boolean isLegal (String code) {
        FoundWord foundWord = WORD_TREE.matchWord(code);
        if (foundWord != null) {
            return false;
        }
        return true;
    }


    /**
     * 0.对用户输入用例进行标准化
     * @param inputList
     * @return
     */
    public List<String> executeCharacter(List<String> inputList) {
        List<String> result = inputList.stream()
                .map(s -> s.replaceAll(",", " ")) // 去除逗号
                .collect(Collectors.toList());
        return result;
    }
    /**
     * 1.保存用户代码
     *
     * @param code 用户代码
     * @return
     */
    private File saveCodeFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        //判断全局代码目录是否存在
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }
        //把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        //获取文件路径
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译代码，获取编译后的信息
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.RunProcessAndGetMessage(compileProcess, "编译");
            if (executeMessage.getExitValue() != 0) {
                executeMessage.setMessage("编译错误");
            } else {
                executeMessage.setMessage("编译成功");
            }
            return executeMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 3.执行文件，获得执行列表文件
     *
     * @param inputList
     * @return
     */
    private List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {

        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();

        for (String inputArgs : inputList) {
            //限制内存  限制权限
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        //超时控制，超时
                        throw new RuntimeException("超时");
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.RunProcessAndGetMessage(runProcess, "运行");
                if (executeMessage.getExitValue() != 0) {
                    executeMessage.setMessage("超时");
                    executeMessageList.add(executeMessage);
                    return executeMessageList;
                }
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("程序执行异常", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.获得输出结果，
     * @param executeMessageList
     * @return
     */
    private ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
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

        executeCodeResponse.setOutputList(outputList);
        if (outputList.size() == executeMessageList.size()) {
            //正常执行
            executeCodeResponse.setStatus(1);
        }

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage("成功");
        //最大时间
        judgeInfo.setTime(maxTime);
        //或得当前程序内存,todo 还未开发，需要完善
        judgeInfo.setMemoryLimit(maxTime + 100);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }

    /**
     * 5.判断文件是否删除成功
     * @param userCodeFile
     * @return
     */
    private boolean deleteFile(File userCodeFile) {
        //        5.文件清理
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
        }
        return true;
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
