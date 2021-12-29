/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 01.11.2020
 */

package net.mmo.utils.kism.arch;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;

import javax.persistence.Entity;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import net.mmo.utils.kism.ui.views.nodes.FormFactory;

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
			.should().resideInAnyPackage("..entities..");
		rule.check(classes);

		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Repository..")
			.should().resideInAnyPackage("..repositories..");
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
		LayeredArchitecture layers = Architectures.layeredArchitecture()
			.layer("Main").definedBy("net.mmo.utils.kism")
			.layer("Backend").definedBy("..backend..")
			.layer("Service").definedBy("..backend.service..")
			.layer("Entities").definedBy("..entities..")
			.layer("Security").definedBy("..security")
			.layer("UI").definedBy("..ui..")
			.layer("UINodes").definedBy("..ui.views.nodes")
			.layer("UIHistory").definedBy("..ui.views.history")
			.layer("UINodesConnections").definedBy("..ui.views.nodes.connection_forms..")
			;

		ArchRule rule = layers
			.whereLayer("Main").mayNotBeAccessedByAnyLayer()
			.whereLayer("UI").mayOnlyBeAccessedByLayers("UINodes", "UIHistory", "Security")
			.whereLayer("UINodes").mayOnlyBeAccessedByLayers("UINodesConnections", "UIHistory", "UI")
			.whereLayer("Security").mayOnlyBeAccessedByLayers("UINodes")
			.whereLayer("Backend").mayOnlyBeAccessedByLayers("Main", "UI","Entities")
			;
		rule.check(classes);

		JavaClasses allExceptFormFactory = classes.that(not(belongToAnyOf(FormFactory.class)));
		rule = layers
			.whereLayer("UINodesConnections").mayOnlyBeAccessedByLayers("UINodes") // violated by FormFactory but that's OK
			;
		rule.check(allExceptFormFactory);
	}

	@ArchTest
	void checkForReferencesToMessagesClassOutsideOfCurrentPackage(JavaClasses classes) {
		DescribedPredicate<JavaAccess<?>> isForeignMessageClassPredicate =
			new DescribedPredicate<JavaAccess<?>>("target is accessing a foreign Messages class")
				{
					@Override
					public boolean apply(JavaAccess<?> access) {
						JavaClass targetClass = access.getTarget().getOwner();
						if ("Messages".equals(targetClass.getSimpleName())) {
							JavaClass callerClass = access.getOwner().getOwner();
							return !targetClass.getPackageName().equals(callerClass.getPackageName());
						}
						return false;
					}
				};

		ArchRule rule = ArchRuleDefinition.noClasses().should().accessTargetWhere(isForeignMessageClassPredicate);
		rule.check(classes);
	}
}
