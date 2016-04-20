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

package org.springframework.test.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.beans.Mergeable;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.SmartRequestBuilder;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 *
 * @author Rob Winch
 * @since 4.3
 */
public abstract class MockHttpSupport<T extends HttpResult, A extends ResultActions<T>> {
	private final Servlet servlet;

	private final Filter[] filters;

	private final ServletContext servletContext;

	private RequestBuilder defaultRequestBuilder;

	private List<? extends ResultMatcher<T>> defaultResultMatchers = new ArrayList<>();

	private List<? extends ResultHandler<T>> defaultResultHandlers = new ArrayList<>();

	protected MockHttpSupport(Servlet servlet, Filter[] filters, ServletContext servletContext) {
		Assert.notNull(filters, "filters cannot be null");
		Assert.noNullElements(filters, "filters cannot contain null values");
		Assert.notNull(servletContext, "A ServletContext is required");

		this.servlet = servlet;
		this.filters = filters;
		this.servletContext = servletContext;
	}


	/**
	 * Perform a request and return a type that allows chaining further
	 * actions, such as asserting expectations, on the result.
	 *
	 * @param requestBuilder used to prepare the request to execute;
	 * see static factory methods in
	 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders}
	 *
	 * @return an instance of {@link ResultActions}; never {@code null}
	 *
	 * @see org.springframework.test.web.servlet.request.MockMvcRequestBuilders
	 * @see org.springframework.test.web.servlet.result.MockMvcResultMatchers
	 */
	public A perform(RequestBuilder requestBuilder) throws Exception {

		if (this.defaultRequestBuilder != null) {
			if (requestBuilder instanceof Mergeable) {
				requestBuilder = (RequestBuilder) ((Mergeable) requestBuilder).merge(this.defaultRequestBuilder);
			}
		}

		MockHttpServletRequest request = requestBuilder.buildRequest(this.servletContext);
		MockHttpServletResponse response = new MockHttpServletResponse();

		if (requestBuilder instanceof SmartRequestBuilder) {
			request = ((SmartRequestBuilder) requestBuilder).postProcessRequest(request);
		}

		final T result = createResult(request, response);

		RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

		MockFilterChain filterChain = new MockFilterChain(this.servlet, this.filters);
		filterChain.doFilter(request, response);

		if (DispatcherType.ASYNC.equals(request.getDispatcherType()) &&
				request.getAsyncContext() != null & !request.isAsyncStarted()) {

			request.getAsyncContext().complete();
		}

		applyDefaultResultActions(result);

		RequestContextHolder.setRequestAttributes(previousAttributes);

		return createActions(result);
	}

	protected abstract A createActions(T result);

	protected abstract T createResult(MockHttpServletRequest request, MockHttpServletResponse response);

	private void applyDefaultResultActions(T mvcResult) throws Exception {

		for (ResultMatcher<T> matcher : this.defaultResultMatchers) {
			matcher.match(mvcResult);
		}

		for (ResultHandler<T> handler : this.defaultResultHandlers) {
			handler.handle(mvcResult);
		}
	}


	/**
	 * A default request builder merged into every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#defaultRequest(RequestBuilder)
	 */
	protected void setDefaultRequest(RequestBuilder requestBuilder) {
		this.defaultRequestBuilder = requestBuilder;
	}

	/**
	 * Expectations to assert after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysExpect(ResultMatcher)
	 */
	protected void setGlobalResultMatchers(List<? extends ResultMatcher<T>> resultMatchers) {
		Assert.notNull(resultMatchers, "resultMatchers is required");
		this.defaultResultMatchers = resultMatchers;
	}

	/**
	 * General actions to apply after every performed request.
	 * @see org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder#alwaysDo(ResultHandler)
	 */
	protected void setGlobalResultHandlers(List<? extends ResultHandler<T>> resultHandlers) {
		Assert.notNull(resultHandlers, "resultHandlers is required");
		this.defaultResultHandlers = resultHandlers;
	}
}
