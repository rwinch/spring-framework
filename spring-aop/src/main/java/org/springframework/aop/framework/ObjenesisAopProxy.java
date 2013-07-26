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

package org.springframework.aop.framework;

import org.objenesis.ObjenesisStd;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;

/**
 * Objenesis based extension of {@link CglibAopProxy} to create proxy instances without
 * invoking the constructor of the class.
 * 
 * @author Oliver Gierke
 * @since 4.0
 */
class ObjenesisAopProxy extends CglibAopProxy {

	private final ObjenesisStd objenesis;

	/**
	 * Creates a ew {@link ObjenesisAopProxy} using the given {@link AdvisedSupport}.
	 * 
	 * @param config must not be {@literal null}.
	 */
	public ObjenesisAopProxy(AdvisedSupport config) {

		super(config);
		this.objenesis = new ObjenesisStd(true);
	}

	@Override
	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {

		Factory factory = (Factory) objenesis.newInstance(enhancer.createClass());
		factory.setCallbacks(callbacks);
		return factory;
	}
}
