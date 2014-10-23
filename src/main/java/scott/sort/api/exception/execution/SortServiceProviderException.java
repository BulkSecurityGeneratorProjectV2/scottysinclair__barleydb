package scott.sort.api.exception.execution;

import scott.sort.api.exception.SortException;

public class SortServiceProviderException extends SortException {

    private static final long serialVersionUID = 1L;

    public SortServiceProviderException() {
        super();
    }

    public SortServiceProviderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SortServiceProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public SortServiceProviderException(String message) {
        super(message);
    }

    public SortServiceProviderException(Throwable cause) {
        super(cause);
    }

}
