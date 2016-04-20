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

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 *
 * @author rwinch
 * @since 4.3
 */
final class DefaultHttpResult implements HttpResult {

	private final MockHttpServletRequest request;

	private final MockHttpServletResponse response;

	/**
	 * @param request
	 * @param response
	 */
	public DefaultHttpResult(MockHttpServletRequest request,
			MockHttpServletResponse response) {
		super();
		this.request = request;
		this.response = response;
	}

	/**
	 * @return the request
	 */
	public MockHttpServletRequest getRequest() {
		return request;
	}

	/**
	 * @return the response
	 */
	public MockHttpServletResponse getResponse() {
		return response;
	}

}
