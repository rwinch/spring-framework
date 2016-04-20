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

import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.codec.support.AbstractAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;

import static org.junit.Assert.assertFalse;

/**
 * @author Arjen Poutsma
 */
public class DataBufferUtilsTests extends AbstractAllocatingTestCase {

	@Test
	public void readChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt")
				.toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);

		Flux<DataBuffer> flux = DataBufferUtils.read(channel, allocator, 4);

		TestSubscriber<DataBuffer> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(flux).
				assertNoError().
				assertComplete().
				assertValues(stringBuffer("foo\n"), stringBuffer("bar\n"),
						stringBuffer("baz\n"), stringBuffer("qux\n"));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readUnalignedChannel() throws Exception {
		URI uri = DataBufferUtilsTests.class.getResource("DataBufferUtilsTests.txt")
				.toURI();
		FileChannel channel = FileChannel.open(Paths.get(uri), StandardOpenOption.READ);

		Flux<DataBuffer> flux = DataBufferUtils.read(channel, allocator, 3);

		TestSubscriber<DataBuffer> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(flux).
				assertNoError().
				assertComplete().
				assertValues(stringBuffer("foo"), stringBuffer("\nba"),
						stringBuffer("r\nb"), stringBuffer("az\n"), stringBuffer("qux"),
						stringBuffer("\n"));

		assertFalse(channel.isOpen());
	}

	@Test
	public void readInputStream() {
		InputStream is = DataBufferUtilsTests.class
				.getResourceAsStream("DataBufferUtilsTests.txt");

		Flux<DataBuffer> flux = DataBufferUtils.read(is, allocator, 4);

		TestSubscriber<DataBuffer> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(flux).
				assertNoError().
				assertComplete().
				assertValues(stringBuffer("foo\n"), stringBuffer("bar\n"),
						stringBuffer("baz\n"), stringBuffer("qux\n"));
	}

	@Test
	public void readUnalignedInputStream() throws Exception {
		InputStream is = DataBufferUtilsTests.class
				.getResourceAsStream("DataBufferUtilsTests.txt");

		Flux<DataBuffer> flux = DataBufferUtils.read(is, allocator, 3);

		TestSubscriber<DataBuffer> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(flux).
				assertNoError().
				assertComplete().
				assertValues(stringBuffer("foo"), stringBuffer("\nba"),
						stringBuffer("r\nb"), stringBuffer("az\n"), stringBuffer("qux"),
						stringBuffer("\n"));
	}


	@Test
	public void takeUntilByteCount() {
		Flux<DataBuffer> flux =
				Flux.just(stringBuffer("foo"), stringBuffer("bar"), stringBuffer("baz"));

		Flux<DataBuffer> result = DataBufferUtils.takeUntilByteCount(flux, 5L);

		TestSubscriber<DataBuffer> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(result).
				assertNoError().
				assertComplete().
				assertValues(stringBuffer("foo"), stringBuffer("ba"));
	}


}