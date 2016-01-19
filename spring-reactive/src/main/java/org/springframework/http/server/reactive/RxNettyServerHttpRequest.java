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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.convert.RxJava1Converter;
import rx.Observable;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpRequest} to the RxNetty {@link HttpServerRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServerRequest<ByteBuf> request;


	public RxNettyServerHttpRequest(HttpServerRequest<ByteBuf> request) {
		Assert.notNull("'request', request must not be null.");
		this.request = request;
	}


	public HttpServerRequest<ByteBuf> getRxNettyRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.request.getHttpMethod().name());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		return new URI(this.request.getUri());
	}

	@Override
	protected void initHeaders(HttpHeaders headers) {
		for (String name : this.request.getHeaderNames()) {
			headers.put(name, this.request.getAllHeaderValues(name));
		}
	}

	@Override
	protected void initCookies(Map<String, List<HttpCookie>> map) {
		for (String name : this.request.getCookies().keySet()) {
			List<HttpCookie> list = map.get(name);
			if (list == null) {
				list = new ArrayList<>();
				map.put(name, list);
			}
			for (Cookie cookie : this.request.getCookies().get(name)) {
				list.add(HttpCookie.clientCookie(name, cookie.value()));
			}
		}
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		Observable<ByteBuffer> content = this.request.getContent().map(ByteBuf::nioBuffer);
		content = content.concatWith(Observable.empty()); // See GH issue #58
		return RxJava1Converter.from(content);
	}

}
