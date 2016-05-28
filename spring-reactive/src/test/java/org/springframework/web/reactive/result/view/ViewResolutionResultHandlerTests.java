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

package org.springframework.web.reactive.result.view;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;
import rx.Single;

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ViewResolutionResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private MockServerHttpResponse response;

	private ModelMap model;


	@Before
	public void setUp() throws Exception {
		this.model = new ExtendedModelMap().addAttribute("id", "123");
	}


	@Test
	public void supportsWithNullReturnValue() throws Exception {
		testSupports("handleString", true);
		testSupports("handleView", true);
		testSupports("handleMonoString", true);
		testSupports("handleMonoView", true);
		testSupports("handleSingleString", true);
		testSupports("handleSingleView", true);
		testSupports("handleModel", true);
		testSupports("handleMap", true);
		testSupports("handleModelAttributeAnnotation", true);
		testSupports("handleTestBean", true);
		testSupports("handleInteger", false);
	}

	@Test
	public void order() throws Exception {
		TestViewResolver resolver1 = new TestViewResolver();
		TestViewResolver resolver2 = new TestViewResolver();
		resolver1.setOrder(2);
		resolver2.setOrder(1);

		assertEquals(Arrays.asList(resolver2, resolver1), new ViewResolutionResultHandler(
				Arrays.asList(resolver1, resolver2), new DefaultConversionService())
				.getViewResolvers());
	}

	@Test
	public void viewReference() throws Exception {
		Object value = new TestView("account");
		handle("/path", value, "handleView");

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewReferenceInMono() throws Exception {
		Object value = Mono.just(new TestView("account"));
		handle("/path", value, "handleMonoView");

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewName() throws Exception {
		Object value = "account";
		handle("/path", value, "handleString", new TestViewResolver("account"));

		TestSubscriber<DataBuffer> subscriber = new TestSubscriber<>();
		subscriber.bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameInMono() throws Exception {
		Object value = Mono.just("account");
		handle("/path", value, "handleMonoString", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameWithMultipleResolvers() throws Exception {
		String value = "profile";
		handle("/path", value, "handleString",
				new TestViewResolver("account"), new TestViewResolver("profile"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("profile: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameUnresolved() throws Exception {
		handle("/path", "account", "handleString")
				.assertErrorMessage("Could not resolve view with name 'account'.");
	}

	@Test
	public void viewNameIsNull() throws Exception {
		ViewResolver resolver = new TestViewResolver("account");

		handle("/account", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));

		handle("/account/", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));

		handle("/account.123", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameIsEmptyMono() throws Exception {
		Object value = Mono.empty();
		handle("/account", value, "handleMonoString", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void model() throws Exception {
		Model value = new ExtendedModelMap().addAttribute("name", "Joe");
		handle("/account", value, "handleModel", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}", asString(buf)));
	}

	@Test
	public void map() throws Exception {
		Map<String, String> value = Collections.singletonMap("name", "Joe");
		handle("/account", value, "handleMap", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}", asString(buf)));
	}

	@Test
	public void modelAttributeAnnotation() throws Exception {
		String value = "Joe";
		handle("/account", value, "handleModelAttributeAnnotation", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}", asString(buf)));
	}

	@Test
	public void testBean() throws Exception {
		Object value = new TestBean("Joe");
		handle("/account", value, "handleTestBean", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, testBean=TestBean[name=Joe]}", asString(buf)));
	}


	private void testSupports(String methodName, boolean supports) throws NoSuchMethodException {
		Method method = TestController.class.getMethod(methodName);
		ResolvableType returnType = ResolvableType.forMethodParameter(method, -1);
		HandlerResult result = new HandlerResult(new Object(), null, returnType, this.model);
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ConfigurableConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new ReactiveStreamsToRxJava1Converter());
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, conversionService);
		if (supports) {
			assertTrue(handler.supports(result));
		}
		else {
			assertFalse(handler.supports(result));
		}
	}

	private TestSubscriber<Void> handle(String path, Object value, String methodName,
			ViewResolver... resolvers) throws Exception {

		List<ViewResolver> resolverList = Arrays.asList(resolvers);
		ConversionService conversionService = new DefaultConversionService();
		HandlerResultHandler handler = new ViewResolutionResultHandler(resolverList, conversionService);
		Method method = TestController.class.getMethod(methodName);
		HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		HandlerResult handlerResult = new HandlerResult(handlerMethod, value, type, this.model);

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		this.response = new MockServerHttpResponse();
		WebSessionManager sessionManager = new DefaultWebSessionManager();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, this.response, sessionManager);

		Mono<Void> mono = handler.handleResult(exchange, handlerResult);

		TestSubscriber<Void> subscriber = new TestSubscriber<>();
		return subscriber.bindTo(mono).await(Duration.ofSeconds(1));
	}

	private static DataBuffer asDataBuffer(String value) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(value.getBytes(Charset.forName("UTF-8")));
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}

	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, Charset.forName("UTF-8"));
	}


	private static class TestViewResolver implements ViewResolver, Ordered {

		private final Map<String, View> views = new HashMap<>();

		private int order = Ordered.LOWEST_PRECEDENCE;


		public TestViewResolver(String... viewNames) {
			Arrays.stream(viewNames).forEach(name -> this.views.put(name, new TestView(name)));
		}

		public void setOrder(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			View view = this.views.get(viewName);
			return Mono.justOrEmpty(view);
		}

	}

	public static final class TestView implements View {

		private final String name;


		public TestView(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return null;
		}

		@Override
		public Flux<DataBuffer> render(HandlerResult result, MediaType mediaType, ServerWebExchange exchange) {
			String value = this.name + ": " + result.getModel().toString();
			assertNotNull(value);
			return Flux.just(asDataBuffer(value));
		}
	}

	@SuppressWarnings("unused")
	private static class TestController {

		public String handleString() {
			return null;
		}

		public Mono<String> handleMonoString() {
			return null;
		}

		public Single<String> handleSingleString() {
			return null;
		}

		public View handleView() {
			return null;
		}

		public Mono<View> handleMonoView() {
			return null;
		}

		public Single<View> handleSingleView() {
			return null;
		}

		public Model handleModel() {
			return null;
		}

		public Map<String, String> handleMap() {
			return null;
		}

		@ModelAttribute("name")
		public String handleModelAttributeAnnotation() {
			return null;
		}

		public TestBean handleTestBean() {
			return null;
		}

		public int handleInteger() {
			return 0;
		}
	}

	private static class TestBean {

		private final String name;

		public TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "TestBean[name=" + this.name + "]";
		}
	}

}