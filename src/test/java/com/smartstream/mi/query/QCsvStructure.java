package com.smartstream.mi.query;

/*
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 * 			<scottysinclair@gmail.com>
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

import scott.sort.api.query.JoinType;
import scott.sort.api.query.QProperty;
import scott.sort.api.query.QueryObject;
import com.smartstream.mi.model.CsvStructure;
import com.smartstream.mac.query.QAccessArea;
import com.smartstream.mi.query.QCsvStructureField;

/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class QCsvStructure extends QueryObject<CsvStructure> {
  private static final long serialVersionUID = 1L;
  public QCsvStructure() {
    super(CsvStructure.class);
  }

  public QCsvStructure(QueryObject<?> parent) {
    super(CsvStructure.class, parent);
  }


  public QProperty<Long> id() {
    return new QProperty<Long>(this, "id");
  }

  public QAccessArea joinToAccessArea() {
    QAccessArea accessArea = new QAccessArea();
    addLeftOuterJoin(accessArea, "accessArea");
    return accessArea;
  }

  public QAccessArea joinToAccessArea(JoinType joinType) {
    QAccessArea accessArea = new QAccessArea();
    addJoin(accessArea, "accessArea", joinType);
    return accessArea;
  }

  public QAccessArea existsAccessArea() {
    QAccessArea accessArea = new QAccessArea(this);
    addExists(accessArea, "accessArea");
    return accessArea;
  }

  public QProperty<String> uuid() {
    return new QProperty<String>(this, "uuid");
  }

  public QProperty<Long> modifiedAt() {
    return new QProperty<Long>(this, "modifiedAt");
  }

  public QProperty<String> name() {
    return new QProperty<String>(this, "name");
  }

  public QProperty<Boolean> headerBasedMapping() {
    return new QProperty<Boolean>(this, "headerBasedMapping");
  }

  public QCsvStructureField joinToFields() {
    QCsvStructureField fields = new QCsvStructureField();
    addLeftOuterJoin(fields, "fields");
    return fields;
  }

  public QCsvStructureField joinToFields(JoinType joinType) {
    QCsvStructureField fields = new QCsvStructureField();
    addJoin(fields, "fields", joinType);
    return fields;
  }

  public QCsvStructureField existsFields() {
    QCsvStructureField fields = new QCsvStructureField(this);
    addExists(fields, "fields");
    return fields;
  }
}