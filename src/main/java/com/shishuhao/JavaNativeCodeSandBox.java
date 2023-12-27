package com.shishuhao;

import com.shishuhao.model.ExecuteCodeRequest;
import com.shishuhao.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

@Component
public class JavaNativeCodeSandBox extends JavaCodeSandBoxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
