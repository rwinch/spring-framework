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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;


/**
 * {@code HandlerResultHandler} that handles return values from methods annotated
 * with {@code @ResponseBody} writing to the body of the request or response with
 * an {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class ResponseBodyResultHandler implements HandlerResultHandler, Ordered {

	private static final MediaType MEDIA_TYPE_APPLICATION_ALL = new MediaType("application");

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ConversionService conversionService;

	private final RequestedContentTypeResolver contentTypeResolver;

	private final List<MediaType> supportedMediaTypes;

	private int order = 0;


	/**
	 * Constructor with message converters and a {@code ConversionService} only
	 * and creating a {@link HeaderContentTypeResolver}, i.e. using Accept header
	 * to determine the requested content type.
	 *
	 * @param converters converters for writing the response body with
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> converters,
			ConversionService conversionService) {

		this(converters, conversionService, new HeaderContentTypeResolver());
	}

	/**
	 * Constructor with message converters, a {@code ConversionService}, and a
	 * {@code RequestedContentTypeResolver}.
	 *
	 * @param messageConverters converters for writing the response body with
	 * @param conversionService for converting to Flux and Mono from other reactive types
	 */
	public ResponseBodyResultHandler(List<HttpMessageConverter<?>> messageConverters,
			ConversionService conversionService, RequestedContentTypeResolver contentTypeResolver) {

		Assert.notEmpty(messageConverters, "At least one message converter is required.");
		Assert.notNull(conversionService, "'conversionService' is required.");
		Assert.notNull(contentTypeResolver, "'contentTypeResolver' is required.");

		this.messageConverters = messageConverters;
		this.conversionService = conversionService;
		this.contentTypeResolver = contentTypeResolver;
		this.supportedMediaTypes = initSupportedMediaTypes(messageConverters);
	}

	private static List<MediaType> initSupportedMediaTypes(List<HttpMessageConverter<?>> converters) {
		Set<MediaType> set = new LinkedHashSet<>();
		converters.forEach(converter -> set.addAll(converter.getWritableMediaTypes()));
		List<MediaType> result = new ArrayList<>(set);
		MediaType.sortBySpecificity(result);
		return Collections.unmodifiableList(result);
	}


	/**
	 * Set the order for this result handler relative to others.
	 * <p>By default this is set to 0 and is generally save to be ahead of other
	 * result handlers since it only gets involved if the method (or class) is
	 * annotated with {@code @ResponseBody}.
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public boolean supports(HandlerResult result) {
		Object handler = result.getHandler();
		if (handler instanceof HandlerMethod) {
			MethodParameter returnType = ((HandlerMethod) handler).getReturnType();
			Class<?> containingClass = returnType.getContainingClass();
			return (AnnotationUtils.findAnnotation(containingClass, ResponseBody.class) != null ||
					returnType.getMethodAnnotation(ResponseBody.class) != null);
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {

		Publisher<?> publisher;
		ResolvableType elementType;
		ResolvableType returnType = result.getReturnValueType();

		if (this.conversionService.canConvert(returnType.getRawClass(), Publisher.class)) {
			Optional<Object> optionalValue = result.getReturnValue();
			if (optionalValue.isPresent()) {
				publisher = this.conversionService.convert(optionalValue.get(), Publisher.class);
			}
			else {
				publisher = Mono.empty();
			}
			elementType = returnType.getGeneric(0);
			if (Void.class.equals(elementType.getRawClass())) {
				return Mono.from((Publisher<Void>)publisher);
			}
		}
		else {
			publisher = Mono.justOrEmpty(result.getReturnValue());
			elementType = returnType;
		}

		List<MediaType> compatibleMediaTypes = getCompatibleMediaTypes(exchange, elementType);
		if (compatibleMediaTypes.isEmpty()) {
			if (result.getReturnValue().isPresent()) {
				List<MediaType> mediaTypes = getProducibleMediaTypes(exchange, elementType);
				return Mono.error(new NotAcceptableStatusException(mediaTypes));
			}
			return Mono.empty();
		}

		MediaType bestMediaType = selectBestMediaType(compatibleMediaTypes);
		if (bestMediaType != null) {
			HttpMessageConverter<?> converter = resolveEncoder(elementType, bestMediaType);
			if (converter != null) {
				ServerHttpResponse response = exchange.getResponse();
				return converter.write((Publisher) publisher, elementType, bestMediaType, response);
			}
		}

		return Mono.error(new NotAcceptableStatusException(this.supportedMediaTypes));
	}

	private List<MediaType> getCompatibleMediaTypes(ServerWebExchange exchange,
			ResolvableType elementType) {

		List<MediaType> acceptableMediaTypes = getAcceptableMediaTypes(exchange);
		List<MediaType> producibleMediaTypes = getProducibleMediaTypes(exchange, elementType);

		Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
		for (MediaType acceptable : acceptableMediaTypes) {
			for (MediaType producible : producibleMediaTypes) {
				if (acceptable.isCompatibleWith(producible)) {
					compatibleMediaTypes.add(selectMoreSpecificMediaType(acceptable, producible));
				}
			}
		}

		List<MediaType> result = new ArrayList<>(compatibleMediaTypes);
		MediaType.sortBySpecificityAndQuality(result);
		return result;
	}

	private List<MediaType> getAcceptableMediaTypes(ServerWebExchange exchange) {
		List<MediaType> mediaTypes = this.contentTypeResolver.resolveMediaTypes(exchange);
		return (mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL) : mediaTypes);
	}

	private List<MediaType> getProducibleMediaTypes(ServerWebExchange exchange, ResolvableType type) {
		Optional<?> optional = exchange.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (optional.isPresent()) {
			Set<MediaType> mediaTypes = (Set<MediaType>) optional.get();
			return new ArrayList<>(mediaTypes);
		}
		else {
			return this.messageConverters.stream()
					.filter(converter -> converter.canWrite(type, null))
					.flatMap(converter -> converter.getWritableMediaTypes().stream())
					.collect(Collectors.toList());
		}
	}

	private MediaType selectMoreSpecificMediaType(MediaType acceptable, MediaType producible) {
		producible = producible.copyQualityValue(acceptable);
		Comparator<MediaType> comparator = MediaType.SPECIFICITY_COMPARATOR;
		return (comparator.compare(acceptable, producible) <= 0 ? acceptable : producible);
	}

	private MediaType selectBestMediaType(List<MediaType> compatibleMediaTypes) {
		for (MediaType mediaType : compatibleMediaTypes) {
			if (mediaType.isConcrete()) {
				return mediaType;
			}
			else if (mediaType.equals(MediaType.ALL) || mediaType.equals(MEDIA_TYPE_APPLICATION_ALL)) {
				return MediaType.APPLICATION_OCTET_STREAM;
			}
		}
		return null;
	}

	private HttpMessageConverter<?> resolveEncoder(ResolvableType type, MediaType mediaType) {
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canWrite(type, mediaType)) {
				return converter;
			}
		}
		return null;
	}

}
