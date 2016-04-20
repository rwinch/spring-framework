/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * <strong>Main entry point for server-side Spring MVC test support.</strong>
 *
 * <h3>Example</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 *     .andExpect(status().isOk())
 *     .andExpect(content().mimeType("text/html"))
 *     .andExpect(forwardedUrl("/WEB-INF/layouts/main.jsp"));
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Sam Brannen
 * @since 3.2
 */
public final class MockHttp extends MockHttpSupport<HttpResult,ResultActions<HttpResult>> {

	/**
	 * Private constructor, not for direct instantiation.
	 * @see org.springframework.test.web.servlet.setup.MockMvcBuilders
	 */
	MockHttp(Servlet servlet, Filter[] filters, ServletContext servletContext) {
		super(servlet, filters, servletContext);
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#createResult(org.springframework.mock.web.MockHttpServletRequest, org.springframework.mock.web.MockHttpServletResponse)
	 */
	@Override
	protected HttpResult createResult(MockHttpServletRequest request,
			MockHttpServletResponse response) {
		return new DefaultHttpResult(request, response);
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#createActions(org.springframework.test.web.HttpResult)
	 */
	@Override
	protected ResultActions<HttpResult> createActions(final HttpResult result) {
		return new ResultActions<HttpResult>() {

			@Override
			public HttpResult andReturn() {
				return result;
			}

			@Override
			public ResultActions<HttpResult> andExpect(ResultMatcher<HttpResult> matcher)
					throws Exception {
				matcher.match(result);
				return this;
			}

			@Override
			public ResultActions<HttpResult> andDo(ResultHandler<HttpResult> handler)
					throws Exception {
				handler.handle(result);
				return this;
			}
		};
	}


}
