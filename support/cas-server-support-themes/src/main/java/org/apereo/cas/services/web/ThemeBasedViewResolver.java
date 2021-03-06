package org.apereo.cas.services.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ThemeBasedViewResolver} is a View Resolver that takes the active theme into account to selectively choose
 * which set of UI views will be used to generate the standard views.
 *
 * @author Daniel Frett
 * @since 5.2.0
 */
@Slf4j
@Setter
@Getter
public class ThemeBasedViewResolver implements ViewResolver, Ordered {

    private final ThemeResolver themeResolver;

    private final ThemeViewResolverFactory viewResolverFactory;

    private final Map<String, ViewResolver> resolvers = new ConcurrentHashMap<>();

    private int order = LOWEST_PRECEDENCE;

    public ThemeBasedViewResolver(final ThemeResolver themeResolver, final ThemeViewResolverFactory viewResolverFactory) {
        this.themeResolver = themeResolver;
        this.viewResolverFactory = viewResolverFactory;
    }

    @Override
    @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    public View resolveViewName(final String viewName, final Locale locale) {
        val theme = Optional.of(RequestContextHolder.currentRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(themeResolver::resolveThemeName);
        try {
            val delegate = theme.map(this::getViewResolver);
            if (delegate.isPresent()) {
                return delegate.get().resolveViewName(viewName, locale);
            }
        } catch (final Exception e) {
            LOGGER.debug("error resolving view [{}] for theme [{}]", viewName, theme.orElse(null), e);
        }
        return null;
    }

    private ViewResolver getViewResolver(final String theme) {
        if (resolvers.containsKey(theme)) {
            return resolvers.get(theme);
        }

        val resolver = viewResolverFactory.create(theme);
        resolvers.put(theme, resolver);
        return resolver;
    }
}
