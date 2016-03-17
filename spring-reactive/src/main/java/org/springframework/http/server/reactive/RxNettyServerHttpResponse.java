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

package org.springframework.http.server.reactive;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the RxNetty {@link HttpServerResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServerResponse<ByteBuf> response;

	public RxNettyServerHttpResponse(HttpServerResponse<ByteBuf> response) {
		Assert.notNull("'response', response must not be null.");
		this.response = response;
	}


	public HttpServerResponse<?> getRxNettyResponse() {
		return this.response;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		this.response.setStatus(HttpResponseStatus.valueOf(status.value()));
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher) {
		Observable<ByteBuf> content =
				RxJava1ObservableConverter.from(publisher).map(this::toByteBuf);
		Observable<Void> completion = this.response.write(content);
		return RxJava1ObservableConverter.from(completion).after();
	}

	private ByteBuf toByteBuf(DataBuffer buffer) {
		if (buffer instanceof NettyDataBuffer) {
			return ((NettyDataBuffer) buffer).getNativeBuffer();
		}
		else {
			return Unpooled.wrappedBuffer(buffer.asByteBuffer());
		}
	}

	@Override
	protected void writeHeaders() {
		for (String name : getHeaders().keySet()) {
			for (String value : getHeaders().get(name))
				this.response.addHeader(name, value);
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new DefaultCookie(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge(httpCookie.getMaxAge().getSeconds());
				}
				if (httpCookie.getDomain().isPresent()) {
					cookie.setDomain(httpCookie.getDomain().get());
				}
				if (httpCookie.getPath().isPresent()) {
					cookie.setPath(httpCookie.getPath().get());
				}
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
	}

}