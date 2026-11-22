package com.mckevin.agentcrm.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Garde-fous de l'architecture hexagonale, vérifiés à chaque build :
 * le domaine ne dépend de rien, les cas d'usage ne dépendent que du domaine et de leurs ports,
 * et les adaptateurs ne se connaissent pas entre eux.
 */
@AnalyzeClasses(packages = "com.mckevin.agentcrm", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPureJava = classes().that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..")
            .because("le domaine porte les règles métier et ne doit dépendre d'aucun framework");

    @ArchTest
    static final ArchRule applicationOnlyKnowsDomainAndPorts = classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "..application..", "java..",
                    "org.springframework.transaction.annotation..")
            .because("les cas d'usage ne connaissent que le domaine et leurs ports (hors démarcation transactionnelle)");

    @ArchTest
    static final ArchRule domainAndApplicationIgnoreAdapters = noClasses().that()
            .resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..adapter..", "..config..");

    @ArchTest
    static final ArchRule webDoesNotReachInfrastructure = noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("l'API REST passe toujours par les cas d'usage");

    @ArchTest
    static final ArchRule outboundAdaptersAreIndependent = noClasses().that().resideInAPackage("..adapter.out.crm..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..");
}
