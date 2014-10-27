package scott.sort.api.exception.model;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.exception.SortException;

public class ProxyCreationException extends SortException {

    private static final long serialVersionUID = 1L;

    public ProxyCreationException() {
        super();
    }

    public ProxyCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProxyCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyCreationException(String message) {
        super(message);
    }

    public ProxyCreationException(Throwable cause) {
        super(cause);
    }
}
