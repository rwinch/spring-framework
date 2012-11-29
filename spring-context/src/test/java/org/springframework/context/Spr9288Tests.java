package org.springframework.context;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.jmx.export.annotation.AnnotationMetadataAssemblerTests;
import org.springframework.jmx.support.JmxUtilsTests;
import org.springframework.jmx.support.MBeanServerFactoryBeanTests;
import org.springframework.scripting.jruby.AdvisedJRubyScriptFactoryTests;

@RunWith(Suite.class)
@SuiteClasses(value = {
        AdvisedJRubyScriptFactoryTests.class,
        AnnotationMetadataAssemblerTests.class,
        MBeanServerFactoryBeanTests.class,
        JmxUtilsTests.class,
         })
public class Spr9288Tests {

}
