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
package org.springframework.http.server.reactive;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;

/**
 * Common base class for {@link ServerHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

	private URI uri;

	private HttpHeaders headers;


	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				this.uri = initUri();
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	/**
	 * Initialize a URI that represents the request. Invoked lazily on the first
	 * call to {@link #getURI()} and then cached.
	 * @throws URISyntaxException
	 */
	protected abstract URI initUri() throws URISyntaxException;

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders(new HttpCookieInputMap());
			initHeaders(this.headers);
		}
		return this.headers;
	}

	/**
	 * Initialize the headers from the underlying request. Invoked lazily on the
	 * first call to {@link #getHeaders()} and then cached.
	 * @param headers the map to add headers to
	 */
	protected abstract void initHeaders(HttpHeaders headers);

	/**
	 * Initialize the cookies from the underlying request. Invoked lazily on the
	 * first access to cookies via {@link #getHeaders()} and then cached.
	 * @param cookies the map to add cookies to
	 */
	protected abstract void initCookies(Map<String, Set<HttpCookie>> cookies);


	/**
	 * Read-only map of input cookies with lazy initialization.
	 */
	private class HttpCookieInputMap implements Map<String, Set<HttpCookie>> {

		private Map<String, Set<HttpCookie>> cookies;


		private Map<String, Set<HttpCookie>> getCookies() {
			if (this.cookies == null) {
				this.cookies = new LinkedHashMap<>();
				initCookies(this.cookies);
			}
			return this.cookies;
		}

		@Override
		public int size() {
			return getCookies().size();
		}

		@Override
		public boolean isEmpty() {
			return getCookies().isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return getCookies().containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return getCookies().containsValue(value);
		}

		@Override
		public Set<HttpCookie> get(Object key) {
			return getCookies().get(key);
		}

		@Override
		public Set<String> keySet() {
			return getCookies().keySet();
		}

		@Override
		public Collection<Set<HttpCookie>> values() {
			return getCookies().values();
		}

		@Override
		public Set<Entry<String, Set<HttpCookie>>> entrySet() {
			return getCookies().entrySet();
		}

		@Override
		public Set<HttpCookie> put(String key, Set<HttpCookie> value) {
			throw new UnsupportedOperationException("Read-only map of cookies.");
		}

		@Override
		public Set<HttpCookie> remove(Object key) {
			throw new UnsupportedOperationException("Read-only map of cookies.");
		}

		@Override
		public void putAll(Map<? extends String, ? extends Set<HttpCookie>> m) {
			throw new UnsupportedOperationException("Read-only map of cookies.");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Read-only map of cookies.");
		}
	}

}
