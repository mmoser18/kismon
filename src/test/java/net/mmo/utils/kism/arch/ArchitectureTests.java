/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 01.11.2020
 */

package net.mmo.utils.kism.arch;

import javax.persistence.Entity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;

@AnalyzeClasses(packages = "net.mmo.utils.kism..", importOptions = { ImportOption.DoNotIncludeTests.class })
@SuppressWarnings("nls")
public class ArchitectureTests
{
	// Note: The boilerplate code
	//		String myPackageName = this.getClass().getPackageName();
	//		JavaClasses classes = new ClassFileImporter().importPackages(myPackageName.substring(0, myPackageName.lastIndexOf('.')));
	// can be replaced by @ArchTest and parameters ("JavaClasses classes)"
	@ArchTest
	void checkVisibility(JavaClasses classes) {
		ArchRule rule = ArchRuleDefinition.classes()
					.that().resideInAnyPackage("..entities..", "..service..", "")
					.and().areNotAnonymousClasses()
					.and().areNotInnerClasses()
					.should().bePublic();
				rule.check(classes);
	}

	@ArchTest
	void checkNamingAndLocation(JavaClasses classes) {
		ArchRule rule;
		rule = ArchRuleDefinition.classes()
			.that().areAnnotatedWith(Entity.class)
			.should().resideInAnyPackage("..entities");
		rule.check(classes);

		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Repository..")
			.should().resideInAnyPackage("..repositories");
		rule.check(classes);
	}

	@ArchTest
	void checkReferences(JavaClasses classes) {
		ArchRule rule;
		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Repository")
			.should().dependOnClassesThat().resideInAnyPackage("..entities")
			;
		rule.check(classes);

		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Service")
			.should().dependOnClassesThat().resideInAnyPackage("..entities")
			.andShould().dependOnClassesThat().resideInAnyPackage("..repositories")
			;
		rule.check(classes);
	}

	@ArchTest
	void checkLayers(JavaClasses classes) {
		ArchRule rule = Architectures.layeredArchitecture()
			.layer("Main").definedBy("net.mmo.utils.kism")
			.layer("Backend").definedBy("..backend..")
			.layer("UI").definedBy("..ui..")
			.layer("Security").definedBy("..security")
			.whereLayer("Main").mayNotBeAccessedByAnyLayer()
			.whereLayer("UI").mayNotBeAccessedByAnyLayer()
			.whereLayer("Security").mayNotBeAccessedByAnyLayer()
			.whereLayer("Backend").mayOnlyBeAccessedByLayers("UI")
			;

		rule.check(classes);
	}

}
