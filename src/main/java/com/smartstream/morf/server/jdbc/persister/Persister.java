package com.smartstream.morf.server.jdbc.persister;

import java.util.*;
import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.smartstream.morf.api.core.*;
import com.smartstream.morf.api.core.entity.Entity;
import com.smartstream.morf.api.core.entity.EntityContext;
import com.smartstream.morf.api.core.entity.EntityState;
import com.smartstream.morf.api.core.entity.Node;
import com.smartstream.morf.api.core.entity.RefNode;
import com.smartstream.morf.api.core.entity.ToManyNode;
import com.smartstream.morf.api.core.entity.ValueNode;
import com.smartstream.morf.server.jdbc.persister.audit.AuditInformation;
import com.smartstream.morf.server.jdbc.persister.audit.AuditRecord;
import com.smartstream.morf.server.jdbc.persister.audit.Change;
import com.smartstream.morf.server.jdbc.persister.exception.*;

public class Persister {

    private static final Logger LOG = LoggerFactory.getLogger(Persister.class);

    private final Environment env;
    private final String namespace;

    public Persister(Environment env, String namespace) {
        this.env = env;
        this.namespace = namespace;
    }

    /**
     * persists the data in the persist request accordingly.
     */
    public void persist(PersistRequest persistRequest) throws Exception {
        if (persistRequest.isEmpty()) {
            return;
        }
        try {
            logStep("Executing perist request " + persistRequest);
            persistRequest.getEntityContext().beginSaving();
            doPersist(persistRequest);
            logStep("Successfully executed perist request " + persistRequest);
            logStep("");
        } catch (Exception x) {
            logStep("Exception while executing perist request " + persistRequest);
            throw x;
        } finally {
            persistRequest.getEntityContext().endSaving();
        }
    }

    private void doPersist(PersistRequest persistRequest) throws Exception {
        logStep("Analysing request");
        PersistAnalyser analyser = new PersistAnalyser(persistRequest.getEntityContext());
        analyser.analyse(persistRequest);
        LOG.debug(analyser.report());

        PersistAnalyser originalAnalyser = analyser;
        analyser = analyser.deepCopy();
        try {
            persist(analyser);

            //we have performed the JDBC commit so copy the state back across
            analyser.applyChanges(originalAnalyser.getEntityContext());
        } catch (OptimisticLockMismatchException x) {
            //make the exception refer to the original entities
            throw switchEntities(persistRequest.getEntityContext(), x);
        }
    }

    /**
     * Persists based on the analyser data
     * @param analyser
     * @throws Exception
     */
    public void persist(PersistAnalyser analyser) throws Exception {
        try {
            doPersist(analyser);
        } catch (Exception x) {
            Connection connection = (Connection) env.getThreadLocalResource(Connection.class.getName(), false);
            if (connection != null) {
                connection.rollback();
            }
            throw x;
        }
    }

    private void doPersist(PersistAnalyser analyser) throws Exception {
        LOG.debug("PRINTING REPORT AGAIN FROM COPY\n" + analyser.report());

        DatabaseDataSet databaseDataSet = new DatabaseDataSet(env, namespace);
        loadAndValidate(databaseDataSet, analyser.getUpdateGroup(), analyser.getDeleteGroup(), analyser.getDependsOnGroup());

        setPrimaryKeys(analyser.getCreateGroup());

        Long newOptimisticLockTime = System.currentTimeMillis();

        /*
         * Because the optimistic lock changes have NOT been applied to the entities yet, the
         * audit information will not contain entries for them yet.
         * This is good as we will use the audit information to find out what entities have changes
         * and therefore require sending to the database.
         *
         * The missing audit info will be added later
         */
        logStep("Generating audit logs");
        AuditInformation audit = new AuditInformation();
        audit.add(auditCreate(analyser.getCreateGroup()));
        audit.add(auditUpdate(databaseDataSet, analyser.getUpdateGroup()));
        audit.add(auditDelete(databaseDataSet, analyser.getDeleteGroup()));

        /*
         * analyse what entities require an update based on the audit information we just gathered
         * and also on the owning relationships.
         * ie a syntax OL should be updated if it's mapping changes since a syntax own's it's mapping and mappings
         * don't have optimistic locks.
         */
        Set<Entity> updateRequired = analyseRequiredUpdates(audit, analyser.getUpdateGroup());

        logStep("Filter out entities which won't change from the update batch group");
        filterOutUnchangedEntities(updateRequired, analyser.getUpdateGroup());

        /*
         * Add audit records for the optimistic lock columns.
         * The real entities still contain the original OL values, we are keeping them for the where clauses
         * in the update and delete statements.
         * This is why we have to manually apply the OL audit information, it was not automatically detected.
         */
        setNewOptimisticLockOnAuditRecords(audit, analyser.getCreateGroup(), analyser.getUpdateGroup(), newOptimisticLockTime);

        /*
         * Optimize the operation groups so that we can perform jdbc batch operations
         */
        logStep("Optimising operation order to enable batching");
        analyser = analyser.optimizedCopy();
        LOG.debug(analyser.report());

        verifyAccessRights(analyser.getCreateGroup(), analyser.getUpdateGroup(), analyser.getDeleteGroup());

        /*
         * helpful for testing
         */
        preJdbcWorkHook();

        /*
         * We always insert before we update, in-case a pending update depends on a created record
         */
        insert(analyser.getCreateGroup(), newOptimisticLockTime);

        /*
         * We always update before we delete, in-case a delete depends on a FK removal.
         */
        update(analyser.getUpdateGroup(), newOptimisticLockTime);

        delete(analyser.getDeleteGroup());

        insert(audit);

        /*
         * perform the commit
         */
        Connection connection = (Connection) env.getThreadLocalResource(Connection.class.getName(), true);
        connection.commit();

        /*
         * updates the optimistic lock nodes for all created and updated entities
         */
        updateOptimisticLocks(newOptimisticLockTime, analyser.getCreateGroup(), analyser.getUpdateGroup());

        /*
         * Clear out the deleted items
         */
        cleanDeletedItems(analyser.getEntityContext(), analyser.getDeleteGroup());

        /*
         * Clear added or deleted reference tracking
         */
        clearRefsForUpdatedEntities(analyser.getUpdateGroup());

        /*
         * All created entities should have the state loaded
         * and all of their ToMany refs should be fetched.
         */
        setLoadedAndFetchedForCreatedEntities(analyser.getCreateGroup());
    }

    public void postCommit() {

    }

    protected void preJdbcWorkHook() {}

    private void logStep(String message) {
        LOG.debug("----------------------------------------------");
        LOG.debug(message + "...");
    }

    /**
     * Loads update and delete entities fresh from the data from the database.
     * Each entity must exist and have the correct optimistic lock.
     * @param databaseDataSet
     * @param updateGroup
     * @param deleteGroup
     * @param dependsOnGroup
     * @throws Exception
     */
    private void loadAndValidate(DatabaseDataSet databaseDataSet, OperationGroup updateGroup, OperationGroup deleteGroup, OperationGroup dependsOnGroup) throws Exception {
        logStep("Loading dataset from database");
        databaseDataSet.loadEntities(updateGroup, deleteGroup, dependsOnGroup);
        for (Entity entity : iterable(updateGroup, deleteGroup, dependsOnGroup)) {
            Entity databaseEntity = databaseDataSet.getEntity(entity.getEntityType(), entity.getKey().getValue());
            if (databaseEntity == null) {
                throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
            }
            verifyOptimisticLock(entity, databaseEntity);
        }
    }

    /**
     * Add audit records for create entities
     * We only audit changes, so from null to some value
     * if a node of the entity has a null value then we don't create a record for it
     * @param createGroup
     * @return
     */
    private List<AuditRecord> auditCreate(OperationGroup createGroup) {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : createGroup.getEntities()) {
            AuditRecord auditRecord = null;
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    if (((ValueNode) node).getValue() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, null, ((ValueNode) node).getValue());
                    }
                }
                else if (node instanceof RefNode) {
                    if (auditRecord == null) {//lazy init
                        auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                    }
                    if (((RefNode) node).getEntityKey() != null) {
                        auditRecord.addChange(node, null, ((RefNode) node).getEntityKey());
                    }
                }
            }
            if (auditRecord != null) {
                LOG.debug("Changes found for " + entity);
                records.add(auditRecord);
            }
        }
        return records;
    }

    /**
     * Audit entities which are updated
     * again only changes are audited
     * @param databaseDataSet
     * @param updateGroup
     * @return
     */
    private List<AuditRecord> auditUpdate(DatabaseDataSet databaseDataSet, OperationGroup updateGroup) {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : updateGroup.getEntities()) {
            AuditRecord auditRecord = null;
            Entity originalEntity = databaseDataSet.getEntity(entity.getEntityType(), entity.getKey().getValue());
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    ValueNode updatedNode = (ValueNode) node;
                    ValueNode origNode = originalEntity.getChild(node.getName(), ValueNode.class);
                    if (!Objects.equals(origNode.getValue(), updatedNode.getValue())) {
                        if (auditRecord == null) {
                            //lazy init of audit record
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, origNode.getValue(), updatedNode.getValue());
                    }
                }
                else if (node instanceof RefNode) {
                    RefNode origNode = originalEntity.getChild(node.getName(), RefNode.class);
                    RefNode updatedNode = (RefNode) node;
                    if (!Objects.equals(origNode.getEntityKey(), updatedNode.getEntityKey())) {
                        if (auditRecord == null) {
                            //lazy init of audit record
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, origNode.getEntityKey(), updatedNode.getEntityKey());
                    }
                }

            }
            if (auditRecord != null) {
                records.add(auditRecord);
                LOG.debug("Changes found for " + entity);
            }
            else {
                LOG.debug("No changes found for " + entity);
            }
        }
        return records;
    }

    /**
     * Only changes are audited, so fields which were null are not included
     * @param databaseDataSet
     * @param deleteGroup
     * @return
     */
    private List<AuditRecord> auditDelete(DatabaseDataSet databaseDataSet, OperationGroup deleteGroup) {
        List<AuditRecord> records = new LinkedList<>();
        for (Entity entity : deleteGroup.getEntities()) {
            AuditRecord auditRecord = null;
            for (Node node : entity.getChildren()) {
                if (node instanceof ValueNode) {
                    if (((ValueNode) node).getValue() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, ((ValueNode) node).getValue(), null);
                    }
                }
                else if (node instanceof RefNode) {
                    if (((RefNode) node).getEntityKey() != null) {
                        if (auditRecord == null) {//lazy init
                            auditRecord = new AuditRecord(entity.getEntityType(), entity.getKey().getValue());
                        }
                        auditRecord.addChange(node, ((RefNode) node).getEntityKey(), null);
                    }
                }
            }
            if (auditRecord != null) {
                LOG.debug("Changes found for " + entity);
                records.add(auditRecord);
            }
        }
        return records;
    }

    private void setPrimaryKeys(OperationGroup createGroup) {
        logStep("Setting primary keys");
        for (Entity entity : createGroup.getEntities()) {
            Object value = env.getSequenceGenerator().getNextKey(entity.getEntityType());
            LOG.debug("Setting key for " + entity + " to " + value);
            entity.getKey().setValue(value);
        }
    }

    /**
     * calculates the set of entities which require their OL to be updated
     * @param audit
     * @param updateGroup
     * @return
     */
    private Set<Entity> analyseRequiredUpdates(AuditInformation audit, OperationGroup updateGroup) {
        Set<Entity> updateRequired = new HashSet<>();
        Set<Entity> updateNotRequired = new HashSet<>();
        for (Entity entity : updateGroup.getEntities()) {
            analyseEntityRequiresUpdate(audit, entity, updateRequired, updateNotRequired);
        }
        return updateRequired;
    }

    /**
     * analyses the entity and it's dependents adding it and them to the relevant sets
     * @param audit
     * @param entity
     * @param updateRequired
     * @param updateNotRequired
     */
    private boolean analyseEntityRequiresUpdate(AuditInformation audit, Entity entity, Set<Entity> updateRequired, Set<Entity> updateNotRequired) {
        if (updateRequired.contains(entity)) {
            return true;
        }
        if (updateNotRequired.contains(entity)) {
            return false;
        }
        //we have an audit record for this entity, it is being directly modified, so tag it as required
        if (audit.contains(entity)) {
            updateRequired.add(entity);
            return true;
        }
        for (RefNode refNode : entity.getChildren(RefNode.class)) {
            //an added entity ref would be detected in the audit information since this directly affects a property of the entity
            //a removed entity ref would be detected in the audit information since this directly affects a property of the entity

            //the reference is null and was always null otherwise the audit check above would catch the change
            if (refNode.getReference() == null) {
                continue;
            }
            //if it has not been loaded from the db (and therefore not modified) then there is no update required.
            if (!refNode.getReference().isLoaded()) {
                continue;
            }
            //we don't own the node, so no changes reflect on us
            if (!refNode.getNodeDefinition().isOwns()) {
                continue;
            }
            //the ref has it's own optimistic lock, so we don't need to touch ours
            if (refNode.getEntityType().supportsOptimisticLocking()) {
                continue;
            }
            //if we get so far then we own an entity which doesn't have it's own optimistic lock
            //check if it requires an update
            boolean ownedEntityRequiresUpdate = analyseEntityRequiresUpdate(audit, refNode.getReference(), updateRequired, updateNotRequired);
            if (ownedEntityRequiresUpdate) {
                LOG.debug("Update required to optimistic lock for " + entity + " due to ref to " + refNode.getReference());
                updateRequired.add(entity);
                return true;
            }
        }
        for (ToManyNode toManyNode : entity.getChildren(ToManyNode.class)) {
            //the to many relation was never fetched, so no changes occurred.
            if (!toManyNode.isFetched()) {
                continue;
            }
            //we don't own the node, so no changes reflect on us
            if (!toManyNode.getNodeDefinition().isOwns()) {
                continue;
            }
            //the entity has it's own optimistic lock so we don't need to touch ours
            if (toManyNode.getEntityType().supportsOptimisticLocking()) {
                continue;
            }
            //check if any of our tomany entities have been modified
            for (Entity toManyEntity : toManyNode.getList()) {
                boolean ownedEntityRequiresUpdate = false;
                //the tomany entity which we own was not loaded and is therefore new
                //meaning we need to update.
                if (!toManyEntity.isLoaded()) {
                    ownedEntityRequiresUpdate = true;
                }
                else {
                    ownedEntityRequiresUpdate = analyseEntityRequiresUpdate(audit, toManyEntity, updateRequired, updateNotRequired);
                }
                if (ownedEntityRequiresUpdate) {
                    LOG.debug("Update required to optimistic lock for " + entity + " due to ref to " + toManyEntity);
                    updateRequired.add(entity);
                    return true;
                }
            }
        }
        /*
         * got to the end, so no update required
         */
        updateNotRequired.add(entity);
        return false;
    }

    private void setNewOptimisticLockOnAuditRecords(AuditInformation audit, OperationGroup createGroup, OperationGroup updateGroup, Long newOptimisticLockTime) {
        for (Entity entity : createGroup.mergedCopy(updateGroup).getEntities()) {
            if (entity.getEntityType().supportsOptimisticLocking()) {
                AuditRecord auditRecord = audit.getOrCreateRecord(entity);
                if (auditRecord != null) {
                    auditRecord.setOptimisticLock(entity, newOptimisticLockTime);
                }
            }
        }
    }

    /**
     * Sets the optimistic lock values on the entities.
     * @param optimisticLockTime
     * @param createGroup
     * @param updateGroup
     */
    private void updateOptimisticLocks(Long optimisticLockTime, OperationGroup createGroup, OperationGroup updateGroup) {
        logStep("Updating optimistic locks");
        updateOptimisticLocks(optimisticLockTime, createGroup.getEntities());
        updateOptimisticLocks(optimisticLockTime, updateGroup.getEntities());
    }

    /**
     * Sets the optimistic lock values on the entities.
     * @param optimisticLockTime
     * @param entities
     */
    private void updateOptimisticLocks(Long optimisticLockTime, List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.getEntityType().supportsOptimisticLocking()) {
                entity.getOptimisticLock().setValue(optimisticLockTime);
            }
        }
    }

    /**
     * Removes entities from the updateGroup which are not in the updateRequired set.
     * @param updateRequired
     * @param updateGroup
     */
    private void filterOutUnchangedEntities(Set<Entity> updateRequired, OperationGroup updateGroup) {
        for (Iterator<Entity> it = updateGroup.getEntities().iterator(); it.hasNext();) {
            Entity entity = it.next();
            if (!updateRequired.contains(entity)) {
                it.remove();
                LOG.debug("Filtered out " + entity);
            }
        }
    }

    /**
     * Verifies the access rights for the operation groups.
     * @param createGroup
     * @param updateGroup
     * @param deleteGroup
     */
    private void verifyAccessRights(OperationGroup createGroup, OperationGroup updateGroup, OperationGroup deleteGroup) {
        logStep("Verifying access rights");
        for (Entity entity : createGroup.getEntities()) {
            verifyCreateRight(entity);
        }
        for (Entity entity : updateGroup.getEntities()) {
            verifyUpdateRight(entity);
        }
        for (Entity entity : deleteGroup.getEntities()) {
            verifyDeleteRight(entity);
        }
    }

    private void insert(OperationGroup createGroup, final Long optimisticLockTime) throws Exception {
        logStep("Performing inserts");
        BatchExecuter batchExecuter = new BatchExecuter(createGroup, "insert") {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementCache psCache, Entity entity) throws SQLException {
                return psCache.prepareInsertStatement(entity, optimisticLockTime);
            }

            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws Exception {
                handleInsertFailure(entity, throwable);
            }
        };
        batchExecuter.execute(env.getDefinitions(namespace));
    }

    private void update(OperationGroup updateGroup, final Long newOptimisticLockTime) throws Exception {
        logStep("Performing updates");
        BatchExecuter batchExecuter = new BatchExecuter(updateGroup, "update") {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementCache psCache, Entity entity) throws SQLException {
                return psCache.prepareUpdateStatement(entity, newOptimisticLockTime);
            }

            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws Exception {
                handleUpdateFailure(entity, throwable);
            }
        };
        batchExecuter.execute(env.getDefinitions(namespace));
    }

    private void delete(OperationGroup deleteGroup) throws Exception {
        logStep("Performing deletes");
        BatchExecuter batchExecuter = new BatchExecuter(deleteGroup, "delete") {
            @Override
            protected PreparedStatement prepareStatement(PreparedStatementCache psCache, Entity entity) throws SQLException {
                return psCache.prepareDeleteStatement(entity);
            }

            @Override
            protected void handleFailure(Entity entity, Throwable throwable) throws Exception {
                handleDeleteFailure(entity, throwable);
            }
        };
        batchExecuter.execute(env.getDefinitions(namespace));
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyCreateRight(Entity entity) {
        LOG.debug("VERIFYING CREATE RIGHT FOR " + entity);
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyUpdateRight(Entity entity) {
        LOG.debug("VERIFYING UPDATE RIGHT FOR " + entity);
    }

    /**
     * Checks access control to verify the right
     */
    private void verifyDeleteRight(Entity entity) {
        LOG.debug("VERIFYING DELETE RIGHT FOR " + entity);
    }

    private void insert(AuditInformation audit) {
        logStep("Inserting audit records");
        for (AuditRecord auditRecord : audit.getRecords()) {
            for (Change change : auditRecord.changes()) {
                System.out.format("AUDIT %1$-30s %2$-30s %3$-30s %4$-30s\n", auditRecord.getEntityType().getTableName(), change.node.getNodeDefinition().getColumnName(), change.oldValue, change.newValue);
            }
        }
    }

    private void verifyOptimisticLock(Entity entity, Entity databaseEntity) throws OptimisticLockMismatchException {
        if (entity.getEntityType().supportsOptimisticLocking()) {
            if (entity.compareOptimisticLocks(databaseEntity) != 0) {
                LOG.debug("Optimistic lock mismatch: ours={" + entity + "," + entity.getOptimisticLock().getValue() + "}, database={" + databaseEntity + "," + databaseEntity.getOptimisticLock().getValue() + "}");
                throw new OptimisticLockMismatchException(entity, databaseEntity);
            }
            LOG.debug("Optimistic lock verified: " + entity + " " + entity.getOptimisticLock().getValue());
        }
    }

    /**
     * Created objects are now in the database
     * So their to many are all considered fetched
     * If there are to-many references then they were also inserted
     * IF there are not to-many references then they were not inserted
     * either way the entity state matches the DB and a fetch is not required
     * @param group
     */
    private void setLoadedAndFetchedForCreatedEntities(OperationGroup createGroup) {
        for (Entity en : createGroup.getEntities()) {
            en.setEntityState(EntityState.LOADED);
            for (ToManyNode toManyNode : en.getChildren(ToManyNode.class)) {
                toManyNode.setFetched(true);
            }
            //clear our ref states back to normal, the updated or deleted refs will be removed
            en.clear();
            en.refresh();
        }
    }

    /**
     * The RefNode updated state can be cleared
     * The ToMany node updated state can be clear
     *
     * This should be done after the deleted entity keys are set to null
     * so that the ToMany refresh will remove them and collect the right ones
     *
     * @param updateGroup
     */
    private void clearRefsForUpdatedEntities(OperationGroup updateGroup) {
        logStep("updating references for updated entities");
        for (Entity en : updateGroup.getEntities()) {
            en.clear();
            en.refresh();
        }
    }

    private void cleanDeletedItems(EntityContext entityContext, OperationGroup deleteGroup) {
        logStep("cleaning deleted items from node context");
        for (Entity entity : deleteGroup.getEntities()) {
            entity.getKey().setValue(null);
            entityContext.remove(entity);
        }
    }

    private Iterable<Entity> iterable(OperationGroup... groups) {
        List<Entity> list = new LinkedList<>();
        for (OperationGroup og : groups) {
            list.addAll(og.getEntities());
        }
        return list;
    }

    private void handleInsertFailure(Entity entity, Throwable throwable) throws PersistException {
        EntityContext tempCtx = new EntityContext(env, namespace);
        Entity loadedEntity = tempCtx.getOrLoad(entity.getEntityType(), entity.getKey().getValue());
        if (loadedEntity != null) {
            throw new PrimaryKeyExistsException(entity.getEntityType(), entity.getKey().getValue());
        }
        else {
            throw new PersistException("Could not insert entity: " + entity, throwable);
        }
    }

    private void handleUpdateFailure(Entity entity, Throwable throwable) throws PersistException {
        EntityContext tempCtx = new EntityContext(env, namespace);
        Entity loadedEntity = tempCtx.getOrLoad(entity.getEntityType(), entity.getKey().getValue());
        if (loadedEntity == null) {
            throw new EntityMissingException(entity.getEntityType(), entity.getKey().getValue());
        }
        else if (!Objects.equals(loadedEntity.getOptimisticLock().getValue(), entity.getOptimisticLock().getValue())) {
            throw new OptimisticLockMismatchException(entity, loadedEntity);
        }
        else {
            throw new PersistException("Could not update entity: " + entity, throwable);
        }
    }

    private void handleDeleteFailure(Entity entity, Throwable throwable) throws PersistException {
        handleUpdateFailure(entity, throwable);
    }

    /**
     * Switches the entities on the exception, used when we copy the entity context before persisting.
     * @param entityContext
     * @param x
     * @return
     */
    private OptimisticLockMismatchException switchEntities(EntityContext entityContext, OptimisticLockMismatchException x) {
        Entity oe = entityContext.getEntityByUuid(x.getEntity().getUuid(), true);
        OptimisticLockMismatchException xnew = new OptimisticLockMismatchException(oe, x.getDatabaseEntity());
        xnew.setStackTrace(x.getStackTrace());
        return xnew;
    }
}
