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

import org.springframework.util.Assert;

/**
 * Container for request matching criteria used in
 * {@link IntrospectableHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public class RequestMatchInfo {

	private final String[] patterns;


	/**
	 * Create a {@link RequestMatchInfo} with the given patterns.
	 */
	public RequestMatchInfo(String... patterns) {
		Assert.notNull(patterns);
		this.patterns = patterns;
	}


	/**
	 * The patterns to check for a match, never {@code null}.
	 */
	public String[] getPatterns() {
		return this.patterns;
	}

}
