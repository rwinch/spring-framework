/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Central class for Spring MVC handler mapping introspection.
 *
 * <p>At startup detects the configured {@code HandlerMapping}s at much like
 * the {@link DispatcherServlet} and then can iterate to find the one that would
 * actually return a handler for a given request.
 *
 * <p>Provides method to {@link #resolve} the request to an
 * {@link IntrospectableHandlerMapping} which can then be used to check the
 * request against request-matching criteria.
 *
 * <p>Provides method to {@link #getCorsConfiguration find} the CORS
 * configuration for a request based on the matching handler.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class HandlerMappingIntrospector implements ApplicationContextAware, CorsConfigurationSource {

	private List<HandlerMapping> handlerMappings;


	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		initHandlerMappings(context);
	}

	private void initHandlerMappings(ApplicationContext context) {
		this.handlerMappings = null;

		Map<String, HandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		if (!matchingBeans.isEmpty()) {
			this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
			AnnotationAwareOrderComparator.sort(this.handlerMappings);
		}

		if (this.handlerMappings == null) {
			this.handlerMappings = new ExtendedDispatcherServlet().getDefaultHandlerMappings(context);
		}
	}


	/**
	 * Find the {@link HandlerMapping} that would handle the given request and
	 * return it as an {@link IntrospectableHandlerMapping} that can be used to
	 * test request-matching criteria. If the matching HandlerMapping is not an
	 * instance of {@link IntrospectableHandlerMapping}, an
	 * IllegalStateException is raised.
	 *
	 * @param request the current request
	 * @return the resolved matcher, or {@code null}
	 * @throws Exception if any of the HandlerMapping's raise an exception
	 */
	public IntrospectableHandlerMapping resolve(HttpServletRequest request) throws Exception {
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			Object handler = handlerMapping.getHandler(wrapper);
			if (handler == null) {
				continue;
			}
			if (handlerMapping instanceof IntrospectableHandlerMapping) {
				return ((IntrospectableHandlerMapping) handlerMapping);
			}
			throw new IllegalStateException("HandlerMapping is not an IntrospectableHandlerMapping");
		}
		return null;
	}

	@Override
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		HttpServletRequest wrapper = new RequestAttributeChangeIgnoringWrapper(request);
		for (HandlerMapping handlerMapping : this.handlerMappings) {
			HandlerExecutionChain handler = null;
			try {
				handler = handlerMapping.getHandler(wrapper);
			}
			catch (Exception ex) {
				// Ignore
			}
			if (handler == null) {
				continue;
			}
			for (HandlerInterceptor interceptor : handler.getInterceptors()) {
				if (interceptor instanceof CorsConfigurationSource) {
					return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrapper);
				}
			}
			if (handler.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) handler.getHandler()).getCorsConfiguration(wrapper);
			}
		}
		return null;
	}

	/**
	 * Extension of DispatcherServlet that allows us to obtain the default
	 * HandlerMapping strategies when none were configured explicitly.
	 */
	private class ExtendedDispatcherServlet extends DispatcherServlet {

		List<HandlerMapping> getDefaultHandlerMappings(ApplicationContext context) {
			return super.getDefaultStrategies(context, HandlerMapping.class);
		}

	}

	/**
	 * Request wrapper that ignores request attribute changes.
	 */
	private static class RequestAttributeChangeIgnoringWrapper extends HttpServletRequestWrapper {


		private RequestAttributeChangeIgnoringWrapper(HttpServletRequest request) {
			super(request);
		}

		@Override
		public void setAttribute(String name, Object value) {
			// Ignore attribute change
		}
	}

}