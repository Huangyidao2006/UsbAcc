package com.hj.data_proxy;

/**
 * Created by huangjian at 21-9-8 16:12
 */
public class ErrorCode {
    public static final int SUCCESS			                    = 0;
    public static final int ERROR_FAILED		                = -1;

    public static final int ERROR_NULL_PTR	                	= 10001;
    public static final int ERROR_INVALID_FD                 	= 10002;
    public static final int ERROR_INVALID_OPERATION			    = 10003;
    public static final int ERROR_SELECT						= 10004;
    public static final int ERROR_SOCK_REMOTE_CLOSE        	    = 11001;
    public static final int ERROR_SOCK_RECV                  	= 11002;
    public static final int ERROR_SOCK_SEND                  	= 11003;
    public static final int ERROR_SOCK_ACCEPT                	= 11004;
    public static final int ERROR_SOCK_TOO_MANY_CLIENTS		    = 11005;
    public static final int ERROR_SOCK_LISTEN					= 11006;
    public static final int ERROR_USB_RECV						= 12001;
    public static final int ERROR_USB_SEND						= 12002;
    public static final int ERROR_USB_NO_DEV                    = 12003;
    public static final int ERROR_USB_NO_PERMISSION             = 12004;
    public static final int ERROR_QUEUE_FULL                    = 13001;
    public static final int ERROR_QUEUE_EMPTY                   = 13002;
    public static final int ERROR_NO_SUCH_CONN					= 14001;
}
