package scott.sort.api.exception.execution.jdbc;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair
 * 			<scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

public class ClosingConnectionException extends SortJdbcException {

    private static final long serialVersionUID = 1L;

    public ClosingConnectionException() {
        super();
    }

    public ClosingConnectionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ClosingConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClosingConnectionException(String message) {
        super(message);
    }

    public ClosingConnectionException(Throwable cause) {
        super(cause);
    }

}