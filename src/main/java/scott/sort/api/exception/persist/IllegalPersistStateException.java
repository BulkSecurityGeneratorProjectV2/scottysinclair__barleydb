package scott.sort.api.exception.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.server.jdbc.persister.exception.SortPersistException;

public class IllegalPersistStateException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    public IllegalPersistStateException() {
        super();
    }

    public IllegalPersistStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public IllegalPersistStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalPersistStateException(String message) {
        super(message);
    }

    public IllegalPersistStateException(Throwable cause) {
        super(cause);
    }
}