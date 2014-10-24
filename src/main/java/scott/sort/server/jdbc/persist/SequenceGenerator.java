package scott.sort.server.jdbc.persist;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */

import scott.sort.api.config.EntityType;
import scott.sort.api.exception.execution.persist.SortPersistException;

public interface SequenceGenerator {

    public Object getNextKey(EntityType entityType) throws SortPersistException;

}