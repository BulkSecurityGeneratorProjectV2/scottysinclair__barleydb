package scott.barleydb.server.jdbc.persist.audit;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.execution.persist.IllegalPersistStateException;

public final class AuditRecord {
    private final EntityType entityType;
    private final Object entityKey;
    private final List<Change> changes = new LinkedList<>();
    private final Set<Node> nodesChanged;

    public AuditRecord(EntityType entityType, Object entityKey) {
        this.entityType = entityType;
        this.entityKey = entityKey;
        this.nodesChanged = new HashSet<>();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Object getEntityKey() {
        return entityKey;
    }

    public void addChange(Node node, Object oldValue, Object newValue) throws IllegalPersistStateException {
        if (nodesChanged.add(node)) {
            changes.add(new Change(node, oldValue, newValue));
        }
        else {
            throw new IllegalPersistStateException("Already consumed change for node " + node);
        }
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public Iterable<Change> changes() {
        return Collections.unmodifiableList(changes);
    }

    /**
     * Sets the optimistic lock change, assumes the node is not there yet
     * @param entity
     * @param newOptimisticLock
     */
    public void setOptimisticLock(Entity entity, Long newOptimisticLock, boolean isCreatedGroupEntity) {
        ValueNode olNode = entity.getOptimisticLock();
        changes.add(new Change(olNode, isCreatedGroupEntity ? null : olNode.getValue(), newOptimisticLock));
    }

    @Override
    public String toString() {
        return "AuditRecord [entityType=" + entityType + ", entityKey=" + entityKey
                + ", changes=" + changes + "]";
    }

}
