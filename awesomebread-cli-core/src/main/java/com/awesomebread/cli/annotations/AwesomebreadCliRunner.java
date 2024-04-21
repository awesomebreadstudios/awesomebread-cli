package com.awesomebread.cli.annotations;

import com.awesomebread.cli.runner.CliRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is an opinionated version of {@link SpringBootApplication @SpringBootApplication} that
 * includes the {@link CliRunner} automatically which looks for
 * <p>
 * If this is not desired, at the  least you will need to scan {@link CliRunner} or {@link com.awesomebread.cli.runner}
 * with Spring in order to start the runner.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootApplication(scanBasePackageClasses = CliRunner.class)
public @interface AwesomebreadCliRunner {


    /**
     * Base packages for Spring to scan
     * @return packages to scan
     */
    @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
    String[] config() default {};

    String description();
}
