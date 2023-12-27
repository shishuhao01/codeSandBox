package com.shishuhao.security;
import java.security.Permission;

public class DefaultSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不设置安全"+perm);
    }
}
