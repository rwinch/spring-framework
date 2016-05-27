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

package org.springframework.core.io.buffer.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.subscriber.SignalEmitter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.Assert;

/**i
 * Utility class for working with {@link DataBuffer}s.
 *
 * @author Arjen Poutsma
 */
public abstract class DataBufferUtils {

	private static final Consumer<ReadableByteChannel> CLOSE_CONSUMER = channel -> {
		try {
			if (channel != null) {
				channel.close();
			}
		}
		catch (IOException ignored) {
		}
	};

	/**
	 * Reads the given {@code InputStream} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the stream when the flux inputStream terminated.
	 * @param inputStream the input stream to read from
	 * @param allocator the allocator to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(InputStream inputStream,
			DataBufferAllocator allocator, int bufferSize) {
		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");

		ReadableByteChannel channel = Channels.newChannel(inputStream);
		return read(channel, allocator, bufferSize);
	}

	/**
	 * Reads the given {@code ReadableByteChannel} into a {@code Flux} of
	 * {@code DataBuffer}s. Closes the channel when the flux is terminated.
	 * @param channel the channel to read from
	 * @param allocator the allocator to create data buffers with
	 * @param bufferSize the maximum size of the data buffers
	 * @return a flux of data buffers read from the given channel
	 */
	public static Flux<DataBuffer> read(ReadableByteChannel channel,
			DataBufferAllocator allocator, int bufferSize) {
		Assert.notNull(channel, "'channel' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");

		return Flux.generate(() -> channel,
				new ReadableByteChannelGenerator(allocator, bufferSize),
				CLOSE_CONSUMER);
	}

	/**
	 * Relays buffers from the given {@link Publisher} until the total
	 * {@linkplain DataBuffer#readableByteCount() byte count} reaches the given maximum
	 * byte count, or until the publisher is complete.
	 * @param publisher the publisher to filter
	 * @param maxByteCount the maximum byte count
	 * @return a flux whose maximum byte count is {@code maxByteCount}
	 */
	public static Flux<DataBuffer> takeUntilByteCount(Publisher<DataBuffer> publisher,
			long maxByteCount) {
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");

		AtomicLong byteCountDown = new AtomicLong(maxByteCount);

		return Flux.from(publisher).
				takeWhile(dataBuffer -> {
					int delta = -dataBuffer.readableByteCount();
					long currentCount = byteCountDown.getAndAdd(delta);
					return currentCount >= 0;
				}).
				map(dataBuffer -> {
					long currentCount = byteCountDown.get();
					if (currentCount >= 0) {
						return dataBuffer;
					}
					else {
						// last buffer
						int size = (int) (currentCount + dataBuffer.readableByteCount());
						return dataBuffer.slice(0, size);
					}
				});
	}

	/**
	 * Tokenize the {@link DataBuffer} using the given delimiter
	 * function. Does not include the delimiter in the result.
	 * @param dataBuffer the data buffer to tokenize
	 * @param delimiter the delimiter function
	 * @return the tokens
	 */
	public static List<DataBuffer> tokenize(DataBuffer dataBuffer,
			IntPredicate delimiter) {
		Assert.notNull(dataBuffer, "'dataBuffer' must not be null");
		Assert.notNull(delimiter, "'delimiter' must not be null");

		List<DataBuffer> results = new ArrayList<DataBuffer>();
		int idx;
		do {
			idx = dataBuffer.indexOf(delimiter);
			if (idx < 0) {
				results.add(dataBuffer);
			}
			else {
				if (idx > 0) {
					DataBuffer slice = dataBuffer.slice(0, idx);
					slice = retain(slice);
					results.add(slice);
				}
				int remainingLen = dataBuffer.readableByteCount() - (idx + 1);
				if (remainingLen > 0) {
					dataBuffer = dataBuffer.slice(idx + 1, remainingLen);
				}
				else {
					release(dataBuffer);
					idx = -1;
				}
			}
		}
		while (idx != -1);
		return Collections.unmodifiableList(results);
	}

	/**
	 * Retains the given data buffer, it it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to retain
	 * @return the retained buffer
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T retain(T dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return (T) ((PooledDataBuffer) dataBuffer).retain();
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Releases the given data buffer, if it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to release
	 * @return {@code true} if the buffer was released; {@code false} otherwise.
	 */
	public static boolean release(DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return ((PooledDataBuffer) dataBuffer).release();
		}
		return false;
	}

	private static class ReadableByteChannelGenerator
			implements BiFunction<ReadableByteChannel, SignalEmitter<DataBuffer>,
						ReadableByteChannel> {

		private final DataBufferAllocator allocator;

		private final int chunkSize;

		public ReadableByteChannelGenerator(DataBufferAllocator allocator, int chunkSize) {
			this.allocator = allocator;
			this.chunkSize = chunkSize;
		}

		@Override
		public ReadableByteChannel apply(ReadableByteChannel
				channel, SignalEmitter<DataBuffer>	sub) {
			try {
				ByteBuffer byteBuffer = ByteBuffer.allocate(chunkSize);
				int read;
				if ((read = channel.read(byteBuffer)) > 0) {
					byteBuffer.flip();
					boolean release = true;
					DataBuffer dataBuffer = this.allocator.allocateBuffer(read);
					try {
						dataBuffer.write(byteBuffer);
						release = false;
						sub.next(dataBuffer);
					}
					finally {
						if (release) {
							release(dataBuffer);
						}
					}
				}
				else {
					sub.complete();
				}
			}
			catch (IOException ex) {
				sub.fail(ex);
			}
			return channel;
		}
	}

}
