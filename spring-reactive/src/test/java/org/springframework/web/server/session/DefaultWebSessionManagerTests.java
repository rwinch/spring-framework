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
package org.springframework.web.server.session;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.DefaultWebServerExchange;
import org.springframework.web.server.WebServerExchange;
import org.springframework.web.server.WebSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Rossen Stoyanchev
 */
public class DefaultWebSessionManagerTests {

	private DefaultWebSessionManager manager;

	private TestWebSessionIdResolver idResolver;

	private DefaultWebServerExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.idResolver = new TestWebSessionIdResolver();
		this.manager = new DefaultWebSessionManager();
		this.manager.setSessionIdResolver(this.idResolver);

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultWebServerExchange(request, response, this.manager);
	}


	@Test
	public void getSessionPassive() throws Exception {
		this.idResolver.setIdToResolve(Optional.empty());
		WebSession session = this.manager.getSession(this.exchange).get();

		assertNotNull(session);
		assertFalse(session.isStarted());
		assertFalse(session.isExpired());

		session.save();

		assertFalse(this.idResolver.getId().isPresent());
		assertNull(this.manager.getSessionStore().retrieveSession(session.getId()).get());
	}

	@Test
	public void getSessionForceCreate() throws Exception {
		this.idResolver.setIdToResolve(Optional.empty());
		WebSession session = this.manager.getSession(this.exchange).get();
		session.start();
		session.save();

		String id = session.getId();
		assertTrue(this.idResolver.getId().isPresent());
		assertEquals(id, this.idResolver.getId().get());
		assertSame(session, this.manager.getSessionStore().retrieveSession(id).get());
	}

	@Test
	public void getSessionAddAttribute() throws Exception {
		this.idResolver.setIdToResolve(Optional.empty());
		WebSession session = this.manager.getSession(this.exchange).get();
		session.getAttributes().put("foo", "bar");
		session.save();

		assertTrue(this.idResolver.getId().isPresent());
	}

	@Test
	public void getSessionExisting() throws Exception {
		DefaultWebSession existing = new DefaultWebSession("1", Clock.systemDefaultZone());
		this.manager.getSessionStore().storeSession(existing);

		this.idResolver.setIdToResolve(Optional.of("1"));
		WebSession actual = this.manager.getSession(this.exchange).get();
		assertSame(existing, actual);
	}

	@Test
	public void getSessionExistingExpired() throws Exception {
		Clock clock = Clock.systemDefaultZone();
		DefaultWebSession existing = new DefaultWebSession("1", clock);
		existing.start();
		existing.setLastAccessTime(Instant.now(clock).minus(Duration.ofMinutes(31)));
		this.manager.getSessionStore().storeSession(existing);

		this.idResolver.setIdToResolve(Optional.of("1"));
		WebSession actual = this.manager.getSession(this.exchange).get();
		assertNotSame(existing, actual);
	}


	private static class TestWebSessionIdResolver implements WebSessionIdResolver {

		private Optional<String> idToResolve = Optional.empty();

		private Optional<String> id = Optional.empty();


		public void setIdToResolve(Optional<String> idToResolve) {
			this.idToResolve = idToResolve;
		}

		public Optional<String> getId() {
			return this.id;
		}

		@Override
		public Optional<String> resolveSessionId(WebServerExchange exchange) {
			return this.idToResolve;
		}

		@Override
		public void setSessionId(WebServerExchange exchange, Optional<String> sessionId) {
			this.id = sessionId;
		}
	}

}
