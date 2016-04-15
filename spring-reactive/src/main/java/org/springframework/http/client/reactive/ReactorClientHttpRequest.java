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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.util.Collection;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.netty.http.HttpClient;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation for the Reactor Net HTTP client
 *
 * @author Brian Clozel
 * @see HttpClient
 */
public class ReactorClientHttpRequest extends AbstractClientHttpRequest {

	private final DataBufferAllocator allocator;

	private final HttpMethod httpMethod;

	private final URI uri;

	private final HttpClient httpClient;

	private Flux<Buffer> body;


	public ReactorClientHttpRequest(HttpMethod httpMethod, URI uri, HttpClient httpClient, HttpHeaders headers,
			DataBufferAllocator allocator) {
		super(headers);
		this.allocator = allocator;
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.httpClient = httpClient;
	}

	@Override
	public DataBufferAllocator allocator() {
		return this.allocator;
	}

	@Override
	public HttpMethod getMethod() {
		return this.httpMethod;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	/**
	 * Set the body of the message to the given {@link Publisher}.
	 *
	 * <p>Since the HTTP channel is not yet created when this method
	 * is called, the {@code Mono<Void>} return value completes immediately.
	 * For an event that signals that we're done writing the request, check the
	 * {@link #execute()} method.
	 *
	 * @return a publisher that completes immediately.
	 * @see #execute()
	 */
	@Override
	public Mono<Void> setBody(Publisher<DataBuffer> body) {

		this.body = Flux.from(body).map(b -> new Buffer(b.asByteBuffer()));
		return Mono.empty();
	}

	@Override
	public Mono<ClientHttpResponse> execute() {

		return this.httpClient.request(new io.netty.handler.codec.http.HttpMethod(httpMethod.toString()), uri.toString(),
				channel -> {
					// see https://github.com/reactor/reactor-io/pull/8
					if (body == null) {
						channel.removeTransferEncodingChunked();
					}
					return applyBeforeCommit()
							.after(() -> {
								getHeaders().entrySet().stream().forEach(e ->
										channel.headers().set(e.getKey(), e.getValue()));
								getCookies().values().stream().flatMap(Collection::stream).forEach(cookie ->
										channel.addCookie(new DefaultCookie(cookie.getName(), cookie.getValue())));
								return Mono.empty();
							})
							.after(() -> {
								if (body != null) {
									return channel.sendBody(body);
								}
								else {
									return channel.sendHeaders();
								}
							});
				})
				.map(httpChannel -> new ReactorClientHttpResponse(httpChannel, allocator));
	}



}

