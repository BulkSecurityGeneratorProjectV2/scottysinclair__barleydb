package scott.sort.api.exception.execution.persist;

/*
 * #%L
 * BarleyDB
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.entity.EntityContext;

public class OptimisticLockMismatchException extends SortPersistException {

    private static final long serialVersionUID = 1L;

    private final Entity entity;

    private final Entity databaseEntity;

    public OptimisticLockMismatchException(Entity entity, Entity databaseEntity) {
        super("Optimistic locks don't match for entity '" + entity + "' and database entity '" + databaseEntity + "'", null);
        this.entity = entity;
        this.databaseEntity = databaseEntity;
    }

    /**
     * Tries to switch the entity to one in the new context and then throw a new exception.<br/>
     * Otherwise throws this exception.
     *
     * @param entityContext
     * @throws OptimisticLockMismatchException
     */
    public void switchEntitiesAndThrow(EntityContext entityContext) throws OptimisticLockMismatchException {
        Entity switched = getCorrespondingEntity(entityContext, entity);
        if (switched == null) {
            throw this;
        }
        else {
            OptimisticLockMismatchException x = new OptimisticLockMismatchException(switched, databaseEntity);
            x.setStackTrace(getStackTrace());
            throw x;
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public Entity getDatabaseEntity() {
        return databaseEntity;
    }

}
