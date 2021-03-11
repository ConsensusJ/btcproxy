package foundation.omni.proxy.analysis;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * OmniProxy Configuration. For now only config is enable/disable.
 */
@ConfigurationProperties("omniproxyd")
public interface OmniProxyConfiguration {
    boolean getEnabled();
}
