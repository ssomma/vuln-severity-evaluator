package org.challenge.vulnseverityevaluator;


import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {

    private static final String APPLICATION = "Application";
    private static final String CONFIGURATION = "Configuration";
    private static final String CONTROLLER = "Controller";
    private static final String INFRASTRUCTURE = "Infrastructure";
    private static final String MODEL = "Model";
    private static final String REPOSITORY = "Repository";
    private static final String SERVICE = "Service";
    private static final String[] CONTROLLER_ACCESS = {CONFIGURATION};
    private static final String[] INFRASTRUCTURE_ACCESS = {APPLICATION, CONFIGURATION, CONTROLLER, REPOSITORY, SERVICE};
    private static final String[] MODEL_ACCESS = {APPLICATION, CONFIGURATION, CONTROLLER, REPOSITORY, SERVICE};
    private static final String[] REPOSITORY_ACCESS = {CONFIGURATION, SERVICE};
    private static final String[] SERVICE_ACCESS = {CONFIGURATION, CONTROLLER};
    private static final String APPLICATION_PACKAGE = "org.challenge";
    public static final String CONFIGURATION_PACKAGE = "..configuration..";
    public static final String CONTROLLER_PACKAGE = "..presentation.controller..";
    public static final String INFRASTRUCTURE_PACKAGE = "..infrastructure..";
    public static final String MODEL_PACKAGE = "..domain.model..";
    public static final String REPOSITORY_PACKAGE = "..datasource.repository..";
    public static final String SERVICE_PACKAGE = "..domain.service..";

    private final JavaClasses allClasses = new ClassFileImporter()
            .importPackages(APPLICATION_PACKAGE);

    private final JavaClasses mainClasses = new ClassFileImporter()
            .withImportOption(DO_NOT_INCLUDE_TESTS)
            .importPackages(APPLICATION_PACKAGE);

    private static ArchRule annotatedClassesShouldResideIn(Class<? extends Annotation> aClass, String packageName) {
        return classes()
                .that().areAnnotatedWith(aClass)
                .should().resideInAnyPackage(packageName);
    }

    @Test
    void givenLayeredArchitectureWhenCheckThenDoNotThrowException() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer(APPLICATION).definedBy(APPLICATION_PACKAGE)
                .layer(CONFIGURATION).definedBy(CONFIGURATION_PACKAGE)
                .layer(CONTROLLER).definedBy(CONTROLLER_PACKAGE)
                .layer(INFRASTRUCTURE).definedBy(INFRASTRUCTURE_PACKAGE)
                .layer(MODEL).definedBy(MODEL_PACKAGE)
                .layer(REPOSITORY).definedBy(REPOSITORY_PACKAGE)
                .layer(SERVICE).definedBy(SERVICE_PACKAGE)
                .whereLayer(CONFIGURATION).mayNotBeAccessedByAnyLayer()
                .whereLayer(CONTROLLER).mayOnlyBeAccessedByLayers(CONTROLLER_ACCESS)
                .whereLayer(INFRASTRUCTURE).mayOnlyBeAccessedByLayers(INFRASTRUCTURE_ACCESS)
                .whereLayer(MODEL).mayOnlyBeAccessedByLayers(MODEL_ACCESS)
                .whereLayer(REPOSITORY).mayOnlyBeAccessedByLayers(REPOSITORY_ACCESS)
                .whereLayer(SERVICE).mayOnlyBeAccessedByLayers(SERVICE_ACCESS);

        rule.allowEmptyShould(true).check(mainClasses);
    }

    @Test
    void givenConfigurationAnnotatedResideInConfigurationPackageWhenCheckThenDoNotThrowException() {
        annotatedClassesShouldResideIn(Configuration.class, CONFIGURATION_PACKAGE)
                .allowEmptyShould(true)
                .check(mainClasses);
    }

    @Test
    void givenControllerAnnotatedResideInControllerPackageWhenCheckThenDoNotThrowException() {
        annotatedClassesShouldResideIn(Controller.class, CONTROLLER_PACKAGE)
                .allowEmptyShould(true)
                .check(allClasses);
    }

    @Test
    void givenServiceAnnotatedResideInServicePackageWhenCheckThenDoNotThrowException() {
        annotatedClassesShouldResideIn(Service.class, SERVICE_PACKAGE)
                .allowEmptyShould(true)
                .check(allClasses);
    }

    @Test
    void givenEntityAnnotatedResideInModelPackageWhenCheckThenDoNotThrowException() {
        annotatedClassesShouldResideIn(Entity.class, MODEL_PACKAGE)
                .allowEmptyShould(true)
                .check(allClasses);
    }

    @Test
    void givenRepositoryAnnotatedResideInRepositoryPackageWhenCheckThenDoNotThrowException() {
        annotatedClassesShouldResideIn(Repository.class, REPOSITORY_PACKAGE)
                .allowEmptyShould(true)
                .check(allClasses);
    }

    @Test
    void givenNamingConventionForTestMethodsWhenCheckThenDoNotThrowException() {
        ArchRule rule = methods()
                .that().areAnnotatedWith(Test.class)
                .should().haveNameMatching("given.+When.+Then[DoNotThrow|Return|Set|Throw].+");
        rule.check(allClasses);
    }
}
