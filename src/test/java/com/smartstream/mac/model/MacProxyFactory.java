package com.smartstream.mac.model;
import scott.sort.api.core.entity.Entity;
import scott.sort.api.core.proxy.ProxyFactory;
import scott.sort.api.exception.model.ProxyCreationException;

public class MacProxyFactory implements ProxyFactory {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  public <T> T newProxy(Entity entity) throws ProxyCreationException {
    if (entity.getEntityType().getInterfaceName().equals(AccessArea.class.getName())) {
      return (T) new AccessArea(entity);
    }
    if (entity.getEntityType().getInterfaceName().equals(User.class.getName())) {
      return (T) new User(entity);
    }
    return null;
  }
}
