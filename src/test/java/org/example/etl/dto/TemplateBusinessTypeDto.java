package org.example.etl.dto;

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

import scott.barleydb.api.dto.BaseDto;


/**
 * Generated from Entity Specification
 *
 * @author scott
 */
public class TemplateBusinessTypeDto extends BaseDto {
  private static final long serialVersionUID = 1L;

  private Long id;
  private TemplateDto template;
  private BusinessTypeDto businessType;

  public TemplateBusinessTypeDto() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public TemplateDto getTemplate() {
    return template;
  }

  public void setTemplate(TemplateDto template) {
    this.template = template;
  }

  public BusinessTypeDto getBusinessType() {
    return businessType;
  }

  public void setBusinessType(BusinessTypeDto businessType) {
    this.businessType = businessType;
  }
}
