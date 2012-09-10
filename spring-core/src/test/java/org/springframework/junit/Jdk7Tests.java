/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.junit;

import static org.springframework.junit.Assumptions.assumeAtLeastJdk17;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Run with JDK 1.6 to see that that Assumptions
 *
 * @author Rob Winch
 *
 */
public class Jdk7Tests {

	@BeforeClass
	public static void assumptions() {
		assumeAtLeastJdk17();
	}

	/**
	 * Will not fail when ran with JDK 1.6 since does not meet assumptions
	 */
	@Test
	public void demoAssumptionWorks() {
		demoFailsWithNoAssumption();
	}

	/**
	 * Will not fail when ran with JDK 1.6 since does not meet assumptions
	 */
	@Test
	public void demoFailsWithNoAssumption() {
		String javaVersion = System.getProperty("java.version");
		Assert.assertTrue(javaVersion.startsWith("1.7."));
	}
}
