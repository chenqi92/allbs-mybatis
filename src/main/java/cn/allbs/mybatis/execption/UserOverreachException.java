package cn.allbs.mybatis.execption;

/**
 * 类 UserOverreachException
 * 用户越权异常
 *
 * @author ChenQi
 * @date 2023/3/31
 */
public class UserOverreachException extends RuntimeException {

    public UserOverreachException() {
        super("用户非法越权!");
    }

    public UserOverreachException(String msg, Throwable t) {
        super(msg, t);
    }

    public UserOverreachException(String msg) {
        super(msg);
    }

    public UserOverreachException(Throwable cause) {
        super(cause);
    }

    protected UserOverreachException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
