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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit tests for {@link ResponseBodyResultHandler}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {


	@Test
	public void supports() throws NoSuchMethodException {
		ResponseBodyResultHandler handler = createResultHandler(new StringEncoder());
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("notAnnotated"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));
	}

	@Test
	public void defaultOrder() throws Exception {
		ResponseBodyResultHandler handler = createResultHandler(new StringEncoder());
		assertEquals(0, handler.getOrder());
	}


	private ResponseBodyResultHandler createResultHandler(Encoder<?>... encoders) {
		List<HttpMessageConverter<?>> converters = Arrays.stream(encoders)
				.map(encoder -> new CodecHttpMessageConverter<>(encoder, null))
				.collect(Collectors.toList());
		return new ResponseBodyResultHandler(converters, new DefaultConversionService());
	}



	@SuppressWarnings("unused")
	private static class TestController {

		public Publisher<String> notAnnotated() {
			return null;
		}

		@ResponseBody
		public Publisher<String> publisherString() {
			return null;
		}

		@ResponseBody
		public Publisher<Void> publisherVoid() {
			return null;
		}
	}

}
