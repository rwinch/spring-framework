/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation.spr10546;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


/**
 *
 * @author Rob Winch
 */
public class Spr10546Tests {
	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if(context != null) {
			context.close();
		}
	}

	@Test
	public void beanLoadsWithEnclosingConfigFirst() {
		assertLoadsMyBean(EnclosingConfig.class,EnclosingConfig.ChildConfig.class);
	}

	@Test
	public void beanLoadsWithChildConfigFirst() {
		assertLoadsMyBean(EnclosingConfig.ChildConfig.class, EnclosingConfig.class);
	}

	@Test
	public void beanLoadsWithEnclosingConfig() {
		assertLoadsMyBean(EnclosingConfig.class);
	}

	@Test
	public void beanLoadsWithChildConfig() {
		assertLoadsMyBean(EnclosingConfig.ChildConfig.class);
	}

	@Test
	public void beanLoadsWithScanning() {
		AnnotationConfigApplicationContext ctx= new AnnotationConfigApplicationContext();
		context = ctx;
		ctx.scan(getClass().getPackage().getName());
		ctx.refresh();
		assertThat(context.getBean(String.class), equalTo("myBean"));
	}

	private void assertLoadsMyBean(Class<?>... annotatedClasses) {
		context = new AnnotationConfigApplicationContext(annotatedClasses);
		assertThat(context.getBean(String.class), equalTo("myBean"));
	}

	@Configuration
	public static class ParentConfig {
		@Bean
		public String myBean() {
			return "myBean";
		}
	}

	@Configuration
	public static class EnclosingConfig {
		@Configuration
		public static class ChildConfig extends ParentConfig {}
	}
}
