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

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.HandlerMapping;

/**
 * Additional interface a {@link HandlerMapping} can implement to expose its
 * request matching algorithm + configuration for use by external frameworks.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see HandlerMappingIntrospector
 */
public interface IntrospectableHandlerMapping {

	/**
	 * Whether the given request matches the request criteria.
	 * @param request the current request
	 * @param info the request matching criteria
	 * @return {@code true} for a match
	 */
	boolean match(HttpServletRequest request, RequestMatchInfo info);

}
