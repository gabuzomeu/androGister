package eu.ttbox.androgister.config.web;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;

import me.prettyprint.cassandra.service.ThriftCluster;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import com.yammer.metrics.reporting.AdminServlet;
import com.yammer.metrics.web.DefaultWebappMetricsFilter;

import eu.ttbox.androgister.config.ApplicationConfiguration;
import eu.ttbox.androgister.config.Constants;
import eu.ttbox.androgister.config.cassandra.CassandraConfiguration;

public class WebConfigurer implements ServletContextListener {

    private final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        log.info("Web application configuration");

        log.debug("Configuring Spring root application context");
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(ApplicationConfiguration.class);
        rootContext.refresh();

        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootContext);

        EnumSet<DispatcherType> disps = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);

        log.debug("Configuring Spring Web application context");
        AnnotationConfigWebApplicationContext dispatcherServletConfig = new AnnotationConfigWebApplicationContext();
        dispatcherServletConfig.setParent(rootContext);
        dispatcherServletConfig.register(DispatcherServletConfig.class);

        log.debug("Registering Spring MVC Servlet");
        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet("dispatcher", new DispatcherServlet(dispatcherServletConfig));
         dispatcherServlet.addMapping("/rest/*");
//        dispatcherServlet.addMapping("/*");
        dispatcherServlet.setLoadOnStartup(2);

        log.debug("Registering Spring Security Filter");
        DelegatingFilterProxy delegatingFilterProxy = new DelegatingFilterProxy();
//        delegatingFilterProxy.setTargetBeanName("authenticationFilter");
        FilterRegistration.Dynamic springSecurityFilter = servletContext.addFilter("springSecurityFilterChain", delegatingFilterProxy);

        Environment env = rootContext.getBean(Environment.class);
        if (env.acceptsProfiles(Constants.SPRING_PROFILE_METRICS)) {
            log.debug("Setting Metrics profile for the Web ApplicationContext");

            log.debug("Registering Metrics Filter");
            FilterRegistration.Dynamic metricsFilter = servletContext.addFilter("webappMetricsFilter", new DefaultWebappMetricsFilter());
            metricsFilter.addMappingForUrlPatterns(disps, true, "/*");

            log.debug("Registering Metrics Admin Servlet");
            ServletRegistration.Dynamic metricsAdminServlet = servletContext.addServlet("metricsAdminServlet", new AdminServlet());
            metricsAdminServlet.addMapping("/metrics/*");
            dispatcherServlet.setLoadOnStartup(3);

            springSecurityFilter.addMappingForServletNames(disps, true, "dispatcher", "atmosphereServlet", "metricsAdminServlet");
        } else {
            springSecurityFilter.addMappingForServletNames(disps, true, "dispatcher", "atmosphereServlet");
        }

        log.debug("Web application fully configured");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Destroying Web application");
        WebApplicationContext ac = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
        AnnotationConfigWebApplicationContext gwac = (AnnotationConfigWebApplicationContext) ac;
        gwac.close();
        log.debug("Web application destroyed");
    }

}
