package com.pv.exceptions;

/**
 * Created by sanitizer on 6/13/2016.
 */
public class InvalidHostNameException extends Exception{

    public InvalidHostNameException(){}

    public InvalidHostNameException(String msg){
        super(msg);
    }

    public InvalidHostNameException(Throwable cause){
        super(cause);
    }

    public InvalidHostNameException(String msg, Throwable cause){
        super(msg, cause);
    }

    public InvalidHostNameException(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
        super(msg, cause, enableSuppression, writableStackTrace);
    }

}