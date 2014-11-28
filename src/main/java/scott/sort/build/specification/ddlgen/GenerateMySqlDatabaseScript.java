package scott.sort.build.specification.ddlgen;

import java.util.Objects;

import scott.sort.api.specification.NodeSpec;

/*
 * #%L
 * Simple Object Relational Framework
 * %%
 * Copyright (C) 2014 Scott Sinclair <scottysinclair@gmail.com>
 * %%
 * All rights reserved.
 * #L%
 */


public class GenerateMySqlDatabaseScript extends GenerateDatabaseScript {

    @Override
    protected void generateColumnType(NodeSpec nodeSpec, StringBuilder sb) {
        Objects.requireNonNull(nodeSpec.getJdbcType(), "JDBC type should not be null for " + nodeSpec);
        switch (nodeSpec.getJdbcType()) {
            case BIGINT:
                sb.append("BIGINT");
                break;
            case INT:
                sb.append("INTEGER");
                break;
            case NVARCHAR:
                sb.append("NATIONAL CHAR VARYING");
                generateLength(nodeSpec, sb);
                break;
            case TIMESTAMP:
                sb.append("TIMESTAMP");
                break;
            case VARCHAR:
                sb.append("VARCHAR");
                generateLength(nodeSpec, sb);
                break;
            case CHAR:
                sb.append("CHAR");
                generateLength(nodeSpec, sb);
                break;
            case BLOB:
                sb.append("BLOB");
                break;
            default:
                throw new IllegalStateException("Invalid JDBC type: " + nodeSpec.getJdbcType());
        }
    }


}