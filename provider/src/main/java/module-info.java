/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
module dev.resteasy.providers.jackson {
    // Jakarta EE APIs
    requires jakarta.ws.rs;

    // Third-party dependencies
    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.jakarta.rs.json;
    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;
    requires static tools.jackson.module.jakarta.xmlbind;
    requires transitive tools.jackson.jakarta.rs.base;

    // RESTEasy modules
    requires org.jboss.resteasy.core;
    requires org.jboss.resteasy.tracing.api;

    // Exports
    exports dev.resteasy.providers.jackson;

    // Opens
    opens dev.resteasy.providers.jackson to org.jboss.resteasy.core;
    opens dev.resteasy.providers.jackson.patch to org.jboss.resteasy.core;
    opens dev.resteasy.providers.jackson.tracing to org.jboss.resteasy.core;

    // Provides
    provides jakarta.ws.rs.core.Feature with dev.resteasy.providers.jackson.JacksonFeature;
    provides org.jboss.resteasy.tracing.api.RESTEasyTracingInfo with
            dev.resteasy.providers.jackson.tracing.JacksonJsonFormatRESTEasyTracingInfo;

}
