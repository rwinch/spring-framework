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

package org.springframework.core.codec.support;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @see JacksonJsonDecoder
 */
public class JacksonJsonEncoder extends AbstractEncoder<Object> {

	private final ObjectMapper mapper;

	private static final ByteBuffer START_ARRAY_BUFFER = ByteBuffer.wrap(new byte[]{'['});

	private static final ByteBuffer SEPARATOR_BUFFER = ByteBuffer.wrap(new byte[]{','});

	private static final ByteBuffer END_ARRAY_BUFFER = ByteBuffer.wrap(new byte[]{']'});

	public JacksonJsonEncoder() {
		this(new ObjectMapper());
	}

	public JacksonJsonEncoder(ObjectMapper mapper) {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		Assert.notNull(mapper, "'mapper' must not be null");

		this.mapper = mapper;
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream,
			DataBufferFactory dataBufferFactory, ResolvableType type, MimeType mimeType,
			Object... hints) {
		if (inputStream instanceof Mono) {
			// single object
			return Flux.from(inputStream)
					.map(value -> serialize(value, dataBufferFactory));
		}
		else {
			// array
			Mono<DataBuffer> startArray =
					Mono.just(dataBufferFactory.wrap(START_ARRAY_BUFFER));
			Flux<DataBuffer> arraySeparators =
					Mono.just(dataBufferFactory.wrap(SEPARATOR_BUFFER)).repeat();
			Mono<DataBuffer> endArray =
					Mono.just(dataBufferFactory.wrap(END_ARRAY_BUFFER));

			Flux<DataBuffer> serializedObjects = Flux.from(inputStream)
					.map(value -> serialize(value, dataBufferFactory));

			Flux<DataBuffer> array = Flux.zip(serializedObjects, arraySeparators)
					.flatMap(tuple -> Flux.just(tuple.getT1(), tuple.getT2()));

			Flux<DataBuffer> arrayWithoutLastSeparator = array.skipLast(1);

			return Flux.concat(startArray, arrayWithoutLastSeparator, endArray);
		}
	}

	private DataBuffer serialize(Object value, DataBufferFactory dataBufferFactory) {
		DataBuffer buffer = dataBufferFactory.allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();
		try {
			this.mapper.writeValue(outputStream, value);
		}
		catch (IOException e) {
			throw new CodecException("Error while writing the data", e);
		}
		return buffer;
	}


}
