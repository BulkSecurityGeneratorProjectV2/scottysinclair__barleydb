package scott.barleydb.test;

/*-
 * #%L
 * BarleyDB
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2017 Scott Sinclair
 *       <scottysinclair@gmail.com>
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

import org.example.acl.dto.AccessAreaDto;
import org.example.acl.dto.UserDto;
import org.example.etl.EtlServices;
import org.example.etl.dto.XmlMappingDto;
import org.example.etl.dto.XmlStructureDto;
import org.example.etl.dto.XmlSyntaxModelDto;
import org.example.etl.model.SyntaxType;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.exception.execution.query.SortQueryException;

public class TestDtoServices extends TestBase {

  private static final Logger LOG = LoggerFactory.getLogger(TestDtoServices.class);

  private EtlServices etlServices;

  private AccessAreaDto root;
  private UserDto scott;

  @Before
  public void init() {
    etlServices = new EtlServices(env, namespace);

    root = new AccessAreaDto();
    root.setName("root");
    root.getChildren().setFetched(false);

    scott = new UserDto();
    scott.setAccessArea(root);
    scott.setName("Scott");
    scott.setUuid("");
  }

  @Test
  public void testSaveXmlSyntax() throws SortServiceProviderException, SortPersistException, SortQueryException {

    long start = System.currentTimeMillis();

    XmlStructureDto structure = new XmlStructureDto();
    structure.setAccessArea(root);
    structure.setUuid("");
    structure.setName("MT940 -structure");


    XmlSyntaxModelDto syntax = new XmlSyntaxModelDto();
    syntax.setName("MT940");
    syntax.setAccessArea( root );
    syntax.setUser( scott );
    syntax.setSyntaxType(SyntaxType.ROOT);
    syntax.setUuid("");
    syntax.getMappings().setFetched(true);

    XmlMappingDto mapping = new XmlMappingDto();
    mapping.setXpath("/root");
    mapping.setTargetFieldName("name");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("/root2");
    mapping.setTargetFieldName("name2");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("/sub");
    mapping.setTargetFieldName("name3");
    mapping.setSyntax(syntax);
    syntax.getMappings().add(mapping);

    XmlSyntaxModelDto subSyntax = new XmlSyntaxModelDto();
    mapping.setSubSyntax(subSyntax);
    subSyntax.setName("MT940 - sub");
    subSyntax.setAccessArea( root );
    subSyntax.setUser( scott );
    subSyntax.setSyntaxType(SyntaxType.SUBSYNTAX);
    subSyntax.setUuid("");
    subSyntax.getMappings().setFetched(true);

    mapping = new XmlMappingDto();
    mapping.setXpath("sub1");
    mapping.setTargetFieldName("name2");
    mapping.setSyntax(subSyntax);
    subSyntax.getMappings().add(mapping);

    mapping = new XmlMappingDto();
    mapping.setXpath("sub2");
    mapping.setTargetFieldName("name3");
    mapping.setSyntax(subSyntax);
    subSyntax.getMappings().add(mapping);

    XmlSyntaxModelDto subSubSyntax = new XmlSyntaxModelDto();
    mapping.setSubSyntax(subSubSyntax);
    subSubSyntax.setName("MT940 - sub - sub");
    subSubSyntax.setAccessArea( root );
    subSubSyntax.setUser( scott );
    subSubSyntax.setSyntaxType(SyntaxType.SUBSYNTAX);
    subSubSyntax.setUuid("");
    subSubSyntax.getMappings().setFetched(true);

    syntax.setStructure(structure);
    subSyntax.setStructure(structure);
    subSubSyntax.setStructure(structure);

    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax ----------------------------");
    etlServices.saveSyntax(syntax);
    System.out.println("Saved syntax " + syntax.getName() + " with ID " + syntax.getId());

    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax again ----------------------------");
    etlServices.saveSyntax(syntax);


    syntax.setName("An improved MT940");
    mapping.setTargetFieldName("name3.1");

    LOG.debug("=====================================================================================================================");
    LOG.debug("Saving Syntax again ----------------------------");
    etlServices.saveSyntax(syntax);


    syntax = etlServices.loadFullXmlSyntax(syntax.getId());
    LOG.debug("Loaded syntax " + syntax.getId() + " with name " + syntax.getName());
    LOG.debug("Finished in " + (System.currentTimeMillis() - start) + " milli seconds");
  }

}
