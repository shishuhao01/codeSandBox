package com.shishuhao.controller;

import com.shishuhao.JavaNativeCodeSandBox;
import com.shishuhao.model.ExecuteCodeRequest;
import com.shishuhao.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
public class MainController {
    @Resource
    private JavaNativeCodeSandBox javaNativeCodeSandBox;

    @PostMapping("/executeCode")
    public ExecuteCodeResponse getExecuteCodeResponse (@RequestBody ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandBox.executeCode(executeCodeRequest);
    }
}
