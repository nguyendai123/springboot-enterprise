package com.enterprise.app.exception;

public class TokenExpiredException extends AppException {
    public TokenExpiredException() { super("Token has expired"); }
}
