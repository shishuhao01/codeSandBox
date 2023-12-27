package com.shishuhao.utils;

import cn.hutool.core.util.StrUtil;
import com.shishuhao.model.ExecuteMessage;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.*;

public class ProcessUtils {
    public static ExecuteMessage RunProcessAndGetMessage (Process runProcess,String poName) {
        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            int exitValue = runProcess.waitFor();  //得到程序错误码
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {
                System.out.println(poName+"成功");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                String compileOutputLine;
                //逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }

                executeMessage.setMessage(compileOutputStringBuilder.toString());
                System.out.println(compileOutputStringBuilder);
            } else {
                System.out.println(poName+"失败,错误状态:"+exitValue);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compileOutputLine;
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                //逐行读取
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compileOutputLine);
                }
                executeMessage.setErrorMessage(compileOutputStringBuilder.toString());
                System.out.println(compileOutputStringBuilder);
                BufferedReader errorCompileReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorCompileOutputLine;
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                //逐行读取
                while ((errorCompileOutputLine = errorCompileReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                }
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());
                System.out.println(errorCompileOutputStringBuilder);
            }
            stopWatch.stop();
            executeMessage.setMaxTime(stopWatch.getLastTaskTimeMillis());
        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;

    }

    public static ExecuteMessage RunInteractiveProcessAndGetMessage (Process runProcess,String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try{
            InputStream inputStream = runProcess.getInputStream();
            OutputStream outputStream = runProcess.getOutputStream();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            args.split(" ");
            outputStreamWriter.write(StrUtil.join("\n",args) + "\n");
            outputStreamWriter.flush();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            //逐行读取
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            outputStream.close();
            inputStream.close();
            outputStreamWriter.close();
            runProcess.destroy();

        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;

    }
}
