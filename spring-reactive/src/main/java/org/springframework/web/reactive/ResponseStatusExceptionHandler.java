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
package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.ResponseStatusException;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebServerExchange;

/**
 * Handle {@link ResponseStatusException} by setting the response status.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionHandler implements WebExceptionHandler {


	@Override
	public Mono<Void> handle(WebServerExchange exchange, Throwable ex) {
		if (ex instanceof ResponseStatusException) {
			exchange.getResponse().setStatusCode(((ResponseStatusException) ex).getHttpStatus());
			return Mono.empty();
		}
		return Mono.error(ex);
	}

}
