/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.setup.ConfigurableMockHttpBuilder;

/**
 * Defines common methods for building a {@code MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> 
	extends ConfigurableMockHttpBuilder<MockMvcConfigurer,B>, MockMvcBuilder {


}
