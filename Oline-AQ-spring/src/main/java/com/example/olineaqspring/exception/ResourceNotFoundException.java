package com.example.olineaqspring.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String entity, Object id) {
        super(entity + "不存在" + (id != null ? "（id: " + id + "）" : ""));
    }
}
