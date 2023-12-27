package com.shishuhao;


import com.shishuhao.model.ExecuteCodeRequest;
import com.shishuhao.model.ExecuteCodeResponse;

public interface CodeSandBox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
