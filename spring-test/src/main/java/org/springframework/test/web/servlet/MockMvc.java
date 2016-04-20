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

package org.springframework.test.web.servlet;

import java.util.List;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.MockHttpSupport;

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
public final class MockMvc extends MockHttpSupport<MvcResult, ResultActions> {

	static String MVC_RESULT_ATTRIBUTE = MockMvc.class.getName().concat(".MVC_RESULT_ATTRIBUTE");

	/**
	 * Private constructor, not for direct instantiation.
	 * @see org.springframework.test.web.servlet.setup.MockMvcBuilders
	 */
	MockMvc(TestDispatcherServlet servlet, Filter[] filters, ServletContext servletContext) {
		super(servlet, filters, servletContext);
	}



	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#perform(org.springframework.test.web.servlet.RequestBuilder)
	 */
	@Override
	public ResultActions perform(RequestBuilder requestBuilder)
			throws Exception {
		ResultActions perform = super.perform(requestBuilder);
		return perform;
	}



	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#createResult(org.springframework.mock.web.MockHttpServletRequest, org.springframework.mock.web.MockHttpServletResponse)
	 */
	@Override
	protected MvcResult createResult(MockHttpServletRequest request,
			MockHttpServletResponse response) {
		final MvcResult mvcResult = new DefaultMvcResult(request, response);
		request.setAttribute(MVC_RESULT_ATTRIBUTE, mvcResult);
		return mvcResult;
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#createActions(org.springframework.test.web.HttpResult)
	 */
	@Override
	protected ResultActions createActions(final MvcResult result) {
		return new ResultActions() {

			@Override
			public MvcResult andReturn() {
				return result;
			}

			@Override
			public ResultActions andExpect(ResultMatcher matcher)
					throws Exception {
				matcher.match(result);
				return this;
			}

			@Override
			public ResultActions andDo(ResultHandler handler)
					throws Exception {
				handler.handle(result);
				return this;
			}

			@Override
			public org.springframework.test.web.ResultActions<MvcResult> andExpect(
					org.springframework.test.web.ResultMatcher<MvcResult> matcher)
							throws Exception {
				matcher.match(result);
				return this;
			}

			@Override
			public org.springframework.test.web.ResultActions<MvcResult> andDo(
					org.springframework.test.web.ResultHandler<MvcResult> handler)
							throws Exception {
				handler.handle(result);
				return this;
			}

		};
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#setDefaultRequest(org.springframework.test.web.servlet.RequestBuilder)
	 */
	@Override
	protected void setDefaultRequest(RequestBuilder requestBuilder) {
		super.setDefaultRequest(requestBuilder);
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#setGlobalResultMatchers(java.util.List)
	 */
	@Override
	protected void setGlobalResultMatchers(List<? extends org.springframework.test.web.ResultMatcher<MvcResult>> resultMatchers) {
		super.setGlobalResultMatchers(resultMatchers);
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.web.MockHttpSupport#setGlobalResultHandlers(java.util.List)
	 */
	@Override
	protected void setGlobalResultHandlers(List<? extends org.springframework.test.web.ResultHandler<MvcResult>> resultHandlers) {
		super.setGlobalResultHandlers(resultHandlers);
	}
}
