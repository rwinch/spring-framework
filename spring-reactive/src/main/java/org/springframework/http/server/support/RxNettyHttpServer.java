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

package org.springframework.http.server.support;

import io.netty.buffer.ByteBuf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.http.server.rxnetty.HttpHandlerRequestHandler;


/**
 * @author Rossen Stoyanchev
 */
public class RxNettyHttpServer extends HttpServerSupport implements InitializingBean, HttpServer {

	private HttpHandlerRequestHandler rxNettyHandler;

	private io.reactivex.netty.protocol.http.server.HttpServer<ByteBuf, ByteBuf> rxNettyServer;

	private boolean running;


	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(getHttpHandler());
		this.rxNettyHandler = new HttpHandlerRequestHandler(getHttpHandler());

		this.rxNettyServer = (getPort() != -1 ?
				io.reactivex.netty.protocol.http.server.HttpServer.newServer(getPort()) :
				io.reactivex.netty.protocol.http.server.HttpServer.newServer());
	}


	@Override
	public void start() {
		if (!this.running) {
			this.running = true;
			this.rxNettyServer.start(this.rxNettyHandler);
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			this.running = false;
			this.rxNettyServer.shutdown();
		}
	}

}
