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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.util.HttpString;
import org.reactivestreams.Publisher;
import reactor.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Undertow {@link HttpServerExchange}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 */
public class UndertowServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServerExchange exchange;

	private final Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter;


	public UndertowServerHttpResponse(HttpServerExchange exchange,
			Function<Publisher<ByteBuffer>, Mono<Void>> responseBodyWriter) {

		Assert.notNull(exchange, "'exchange' is required.");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.exchange = exchange;
		this.responseBodyWriter = responseBodyWriter;
	}


	public HttpServerExchange getUndertowExchange() {
		return this.exchange;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		Assert.notNull(status);
		getUndertowExchange().setStatusCode(status.value());
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		return this.responseBodyWriter.apply(publisher);
	}

	@Override
	protected void writeHeadersInternal() {
		for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
			HttpString headerName = HttpString.tryFromString(entry.getKey());
			this.exchange.getResponseHeaders().addAll(headerName, entry.getValue());
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getHeaders().getCookies().keySet()) {
			for (HttpCookie httpCookie : getHeaders().getCookies().get(name)) {
				Cookie cookie = new CookieImpl(name, httpCookie.getValue());
				cookie.setDomain(httpCookie.getDomain());
				cookie.setPath(httpCookie.getPath());
				cookie.setMaxAge(httpCookie.getMaxAge() == Long.MIN_VALUE ? null : (int) httpCookie.getMaxAge());
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.exchange.getResponseCookies().putIfAbsent(name, cookie);
			}
		}
	}

}
