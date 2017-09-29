package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Created by tom on 12.06.16.
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

  static KeycloakServerProperties keycloakServerProperties;

  public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {

    super(augmentToRedirectContextPath(context), dispatcher);

    tryCreateMasterRealmAdminUser();
  }

  private void tryCreateMasterRealmAdminUser() {

    KeycloakSession session = getSessionFactory().create();

    ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);

    try {
      session.getTransactionManager().begin();
      if (applianceBootstrap.isNoMasterUser()) {
          String adminUsername = keycloakServerProperties.getAdminUsername();
          String adminPassword = keycloakServerProperties.getAdminPassword();
          applianceBootstrap.createMasterRealmUser(adminUsername, adminPassword);
      } else {
        System.out.println("Admin user present");
      }
      session.getTransactionManager().commit();
    } catch (Exception ex) {
      System.err.println("Couldn't create keycloak master admin user: " + ex.getMessage());
      session.getTransactionManager().rollback();
    }

    session.close();
  }


  static ServletContext augmentToRedirectContextPath(ServletContext servletContext) {

    ClassLoader classLoader = servletContext.getClassLoader();
    Class[] interfaces = {ServletContext.class};

    InvocationHandler invocationHandler = (proxy, method, args) -> {

      if ("getContextPath".equals(method.getName())) {
        return keycloakServerProperties.getContextPath();
      }

      if ("getInitParameter".equals(method.getName()) && args.length == 1 && "keycloak.embedded".equals(args[0])) {
        return "true";
      }

      return method.invoke(servletContext, args);
    };

    return ServletContext.class.cast(Proxy.newProxyInstance(classLoader, interfaces, invocationHandler));
  }
}
