/**
 * @author Michael Moser (michael.moser@freesurf.ch)
 * @since 01.11.2020
 */

package net.mmo.utils.kism.arch;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;

import java.io.Serializable;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.tngtech.archunit.library.GeneralCodingRules;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
// import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
// import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
// import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import net.mmo.utils.kism.ui.views.nodes.FormFactory;

@AnalyzeClasses(packages = "net.mmo.utils.kism..", importOptions = { ImportOption.DoNotIncludeTests.class })
@SuppressWarnings({"nls", "javadoc"})
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
					.and().areNotEnums() // enums are considered as classes
					.should().bePublic();
				rule.check(classes);
	}

	@ArchTest
	void checkNamingAndLocation(JavaClasses classes) {
		ArchRule rule;
		rule = ArchRuleDefinition.classes()
			.that().areAnnotatedWith(Entity.class)
			.should().resideInAnyPackage("..entities..");
		rule.allowEmptyShould(true).check(classes);

		rule = ArchRuleDefinition.classes()
			.that().areAnnotatedWith(MappedSuperclass.class)
			.should().resideInAnyPackage("..entities..");
		rule.allowEmptyShould(true).check(classes);

		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Repository")
			.should().resideInAnyPackage("..repositories..");
		rule.allowEmptyShould(true).check(classes);
	}

	@ArchTest
	void checkReferences(JavaClasses classes) {
		ArchRule rule;
		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Repository")
			.should().dependOnClassesThat().resideInAnyPackage("..entities..")
			;
		rule.allowEmptyShould(true).check(classes);

		rule = ArchRuleDefinition.classes()
			.that().haveNameMatching("..Service")
			.should().dependOnClassesThat().resideInAnyPackage("..entities..")
			.andShould().dependOnClassesThat().resideInAnyPackage("..repositories..")
			;
		rule.allowEmptyShould(true).check(classes);
	}

	@ArchTest
	void checkSerializability(JavaClasses classes) {
		ArchRule rule = ArchRuleDefinition.classes()
			.that().resideInAnyPackage("..entities..")
			.and().doNotHaveSimpleName("NodeFactory")
			.and().doNotHaveSimpleName("Messages")
			.and().doNotHaveSimpleName("JDBCHandling")
			.and().areNotInnerClasses()
			.should().beAssignableTo(Serializable.class)
			;
		rule.allowEmptyShould(true).check(classes);
	}

	@ArchTest
	void checkLayers(JavaClasses classes) {
		LayeredArchitecture layers = Architectures.layeredArchitecture().consideringAllDependencies()
			.layer("Main").definedBy("net.mmo.utils.kism")
			.layer("Backend").definedBy("..backend..")
			.layer("Service").definedBy("..backend.service..")
			.layer("Entities").definedBy("..entities..")
			.layer("Security").definedBy("..security")
			.layer("UI").definedBy("..ui..") // ALL in and below ..ui
			.layer("UINodes").definedBy("..ui.views.nodes")
			.layer("UIHistory").definedBy("..ui.views.history")
			.layer("UINodesConnections").definedBy("..ui.views.nodes.connection_forms..")
			;

		ArchRule rule = layers
			.whereLayer("Main").mayNotBeAccessedByAnyLayer()
			.whereLayer("UI").mayOnlyBeAccessedByLayers("UINodes", "UIHistory", "Security")
			.whereLayer("UINodes").mayOnlyBeAccessedByLayers("UINodesConnections", "UIHistory", "UI")
			.whereLayer("Security").mayOnlyBeAccessedByLayers("UI")
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
	public void classes_should_adhere_to_general_coding_rules(JavaClasses classes) {

		JavaClasses allExcludingExceptions = classes
//			.that(are(not(equivalentTo(Main.class)))
//				// .and(that(not(belongToAnyOf(foobar.class)))
//				// ...
//				)
			;

		CompositeArchRule.of(GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS)
			.and(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING)
			// the others we *do* use - so disabled:
			// .and(GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS)
			// .and(GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME)
			.because("these are violation of our general coding rules!")
			.check(allExcludingExceptions);
	}

	@ArchTest
	void checkCycles(JavaClasses classes) {
		SlicesRuleDefinition.slices().matching("ch.zh.ksta.zhquest.(*)..").should().beFreeOfCycles();
	}

	@ArchTest
	void checkForReferencesToMessagesClassOutsideOfCurrentPackage(JavaClasses classes) {
		DescribedPredicate<JavaAccess<?>> isForeignMessageClassPredicate =
			new DescribedPredicate<JavaAccess<?>>("target is accessing a foreign Messages class")
				{
					@Override
					public boolean test(JavaAccess<?> access) {
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
