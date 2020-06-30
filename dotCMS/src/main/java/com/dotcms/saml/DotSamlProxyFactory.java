package com.dotcms.saml;

import com.dotcms.osgi.OSGIConstants;
import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.apache.felix.framework.OSGIUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This is the proxy to provides the object to interact with the Saml Osgi Bundle
 *
 * @author jsanca
 */
public class DotSamlProxyFactory {

    public static final String SAML_APP_CONFIG_KEY = "dotsaml-config";


    private final MessageObserver    messageObserver    = new DotLoggerMessageObserver();
    private final AppsAPI            appsAPI            = APILocator.getAppsAPI();
    private final IdentityProviderConfigurationFactory identityProviderConfigurationFactory =
            new DotIdentityProviderConfigurationFactoryImpl(this.appsAPI, APILocator.getHostAPI());

    private SamlServiceBuilder samlServiceBuilder                = null;
    private SamlConfigurationService samlConfigurationService    = null;
    private SamlAuthenticationService samlAuthenticationService  = null;

    private static class SingletonHolder {

        private static final DotSamlProxyFactory INSTANCE = new DotSamlProxyFactory();
    }
    /**
     * Get the instance.
     * @return DotSamlFactory
     */
    public static DotSamlProxyFactory getInstance() {

        return DotSamlProxyFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Returns the dotCMS implementation of the identity provider config.
     * This one basically returns (if exists) the configuration for a idp for a host
     * @return IdentityProviderConfigurationFactory
     */
    public IdentityProviderConfigurationFactory identityProviderConfigurationFactory() {

        return identityProviderConfigurationFactory;
    }

    private SamlServiceBuilder samlServiceBuilder() {

        if (null == this.samlServiceBuilder) {

            synchronized (this) {

                if (null == this.samlServiceBuilder) {


                    try {
                        if (!OSGIUtil.getInstance().isInitialized()) {
                            Logger.warn(this.getClass(),
                                    "OSGI Framework not initialized, trying to initialize...");
                            OSGIUtil.getInstance().initializeFramework(Config.CONTEXT);
                        }
                    } catch (Exception e) {

                        Logger.error(this.getClass(), "Unable to initialized OSGI Framework", e);
                    }

                    if (OSGIUtil.getInstance().isInitialized()) {
                        try {

                            this.samlServiceBuilder = OSGIUtil.getInstance().getService(SamlServiceBuilder.class,
                                    OSGIConstants.BUNDLE_NAME_DOTCMS_SAML);

                            Logger.info(this, "SAML Osgi Bundle has been started");
                        } catch (Exception e) {
                            Logger.error(this.getClass(),
                                    String.format("Failure retrieving OSGI Service [%s] in bundle [%s]",
                                            SamlServiceBuilder.class,
                                            OSGIConstants.BUNDLE_NAME_DOTCMS_SAML), e);
                        }
                    } else {

                        Logger.error(this.getClass(), "OSGI Framework is not initialized, SAML couldn't start");
                    }
                }
            }
        }

        return this.samlServiceBuilder;
    }

    /**
     * Returns the dotCMS implementation of the {@link MessageObserver} for the saml osgi bundle
     * @return MessageObserver
     */
    private MessageObserver messageObserver() {

        return this.messageObserver;
    }

    /**
     * Returns the service that helps to retrieve the actual values or default values from the {@link com.dotcms.saml.IdentityProviderConfiguration}
     *
     * @return SamlConfigurationService
     */
    public SamlConfigurationService samlConfigurationService() {

        if (null == this.samlConfigurationService) {

            final SamlServiceBuilder samlServiceBuilder = this.samlServiceBuilder();
            if (null != samlServiceBuilder) {

                synchronized (this) {

                    if (null == this.samlConfigurationService) {

                        this.samlConfigurationService = samlServiceBuilder.buildSamlConfigurationService();
                    }
                }
            } else {

                Logger.error(this.getClass(), "OSGI Framework may be not initialized, couldn't get the Saml Configuration");
            }
        }

        return this.samlConfigurationService;
    }

    /**
     * Retrieve the authentication service, this is the proxy with the SAML Osgi bundle and must exists at least one host configurated with SAML in order to init this service.
     * @return SamlAuthenticationService
     */
    public SamlAuthenticationService samlAuthenticationService() {

        if (this.isAnyHostConfiguredAsSAML()) {

            if (null == this.samlAuthenticationService) {

                final SamlServiceBuilder samlServiceBuilder = this.samlServiceBuilder();

                if (null != samlServiceBuilder) {

                    synchronized (this) {

                        if (null == this.samlAuthenticationService) {

                            this.samlAuthenticationService =
                                    this.samlServiceBuilder.buildAuthenticationService(this.identityProviderConfigurationFactory(),
                                            this.messageObserver(), this.samlConfigurationService());

                            samlAuthenticationService.initService(Collections.emptyMap());
                        }
                    }
                } else {

                    Logger.error(this.getClass(), "OSGI Framework may be not initialized, couldn't get the Saml Configuration");
                }
            }

            return this.samlAuthenticationService;
        }

        throw new SamlException("Not any host has been configured as a SAML");
    }

    /**
     * Returns true is any host is configured as a SAML
     * @return boolean
     */
    public boolean isAnyHostConfiguredAsSAML () {

        boolean isAnyConfigured = false;
        final User user         = APILocator.systemUser();

        final Optional<AppDescriptor> appDescriptorOptional = Try.of(
                ()-> this.appsAPI
                        .getAppDescriptor(SAML_APP_CONFIG_KEY, user)).getOrElseGet(e-> Optional.empty());
        if (appDescriptorOptional.isPresent()) {

            final AppDescriptor appDescriptor = appDescriptorOptional.get();

            final Map<String, Set<String>>  appKeysByHost = Try.of(()-> this.appsAPI.appKeysByHost())
                    .getOrElseGet(e -> Collections.emptyMap());
            final Set<String> sitesWithConfigurations     = this.appsAPI
                    .filterSitesForAppKey(appDescriptor.getKey(), appKeysByHost.keySet(), user);

            isAnyConfigured                   = !sitesWithConfigurations.isEmpty();
        }

        return isAnyConfigured;
    }

}