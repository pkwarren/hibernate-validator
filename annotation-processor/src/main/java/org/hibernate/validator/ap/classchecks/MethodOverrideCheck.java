/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.ap.classchecks;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.hibernate.validator.ap.checks.ConstraintCheckIssue;
import org.hibernate.validator.ap.util.CollectionHelper;

/**
 * Abstract base class for {@link ClassCheck} implementations of overridden method checks.
 *
 * @author Marko Bekhta
 */
public abstract class MethodOverrideCheck extends AbstractClassCheck {

	private static final String JAVA_LANG_OBJECT = "java.lang.Object";

	private final Elements elementUtils;

	private final Types typeUtils;

	public MethodOverrideCheck(Elements elementUtils, Types typeUtils) {
		this.elementUtils = elementUtils;
		this.typeUtils = typeUtils;
	}

	@Override
	public Collection<ConstraintCheckIssue> checkMethod(ExecutableElement currentMethod) {
		if ( !needToPerformAnyChecks( currentMethod ) ) {
			return Collections.emptySet();
		}
		TypeElement currentTypeElement = getEnclosingTypeElement( currentMethod );

		// find if there's a method that was overridden by a current one.
		List<ExecutableElement> overridden = findAllOverriddenElements( currentTypeElement, currentMethod );
		if ( overridden.isEmpty() ) {
			return Collections.emptySet();
		}

		// if there's more than one overridden method we need to make sure that all of them match
		Set<ConstraintCheckIssue> errors = CollectionHelper.newHashSet();
		for ( ExecutableElement firstExecutable : overridden ) {
			for ( ExecutableElement secondExecutable : overridden ) {
				if ( firstExecutable.equals( secondExecutable ) ) {
					continue;
				}
				if ( !checkOverriddenMethod( firstExecutable, secondExecutable ) ) {
					errors.add( ConstraintCheckIssue.error( currentMethod, null, getErrorMessageKey(), getEnclosingTypeElement( secondExecutable ).getQualifiedName().toString() ) );
				}
			}
		}

		if ( !errors.isEmpty() ) {
			return errors;
		}

		// if we reached this part of code it means we need to check if the current method 'correctly' overrides super methods.
		for ( ExecutableElement overriddenExecutableElement : overridden ) {
			if ( !checkOverriddenMethod( currentMethod, overriddenExecutableElement ) ) {
				return CollectionHelper.asSet( ConstraintCheckIssue.error( currentMethod, null, getErrorMessageKey(), getEnclosingTypeElement( overriddenExecutableElement ).getQualifiedName().toString() ) );

			}
		}

		return Collections.emptySet();
	}

	/**
	 * There can be situations in which no checks should be performed. So in such cases we will not look for any overridden
	 * methods and do any work at all.
	 *
	 * @param currentMethod a method under investigation.
	 *
	 * @return {@code true} if we should proceed with checks and {@code false} otherwise.
	 */
	protected abstract boolean needToPerformAnyChecks(ExecutableElement currentMethod);

	protected abstract boolean checkOverriddenMethod(ExecutableElement currentMethod, ExecutableElement otherMethod);

	/**
	 * Method which returns a Error message key to be used if there's an error in method overriding
	 *
	 * @return error message key
	 */
	protected abstract String getErrorMessageKey();


	/**
	 * Find a list of overridden methods from all super classes and all implemented interfaces
	 *
	 * @param currentTypeElement a class in which method is located
	 * @param current method that we want to find overridden ones for
	 *
	 * @return a list of overridden methods if there are such and empty list otherwise.
	 */
	private List<ExecutableElement> findAllOverriddenElements(
			TypeElement currentTypeElement,
			ExecutableElement current) {
		List<ExecutableElement> elements = CollectionHelper.newArrayList();

		// get a super class
		TypeElement parentTypeElement = (TypeElement) typeUtils.asElement( currentTypeElement.getSuperclass() );

		//look for implemented interfaces:
		elements.addAll( checkInterfacesForOverriddenMethod(
				currentTypeElement,
				currentTypeElement.getInterfaces(),
				current
		) );

		// if super class is java.lang.Object - then there's no need to do any other work - no such method was found.
		while ( !JAVA_LANG_OBJECT.equals( parentTypeElement.toString() ) ) {
			ExecutableElement element = getOverriddenElement( currentTypeElement, parentTypeElement, current );
			if ( element != null ) {
				elements.add( element );
			}

			//need to check all implemented interfaces as well:
			checkInterfacesForOverriddenMethod( currentTypeElement, parentTypeElement.getInterfaces(), current );

			parentTypeElement = (TypeElement) typeUtils.asElement( parentTypeElement.getSuperclass() );
		}

		return elements;
	}

	/**
	 * Find a list of overridden methods from implemented interfaces
	 *
	 * @param currentTypeElement a class in which method is located
	 * @param interfaces a list of implemented interfaces
	 * @param current method that we want to find overridden ones for
	 *
	 * @return a list of overridden methods if there are such, empty list otherwise.
	 */
	private List<ExecutableElement> checkInterfacesForOverriddenMethod(
			TypeElement currentTypeElement,
			List<? extends TypeMirror> interfaces,
			ExecutableElement current) {
		List<ExecutableElement> elements = CollectionHelper.newArrayList();

		for ( TypeMirror anInterface : interfaces ) {
			ExecutableElement element = getOverriddenElement( currentTypeElement, (TypeElement) typeUtils.asElement( anInterface ), current );
			if ( element != null ) {
				elements.add( element );
			}
		}

		return elements;
	}

	/**
	 * Find a method that is overridden by the one passed to this function
	 *
	 * @param currentTypeElement a class in which method is located
	 * @param otherTypeElement a class/interface on which to look for overridden method
	 * @param current method that we want to find overridden ones for
	 *
	 * @return an overridden method if there's one, and {@code null} otherwise.
	 */
	private ExecutableElement getOverriddenElement(
			TypeElement currentTypeElement,
			TypeElement otherTypeElement,
			ExecutableElement current) {

		if ( JAVA_LANG_OBJECT.equals( otherTypeElement.toString() ) ) {
			return null;
		}

		for ( Element element : elementUtils.getAllMembers( otherTypeElement ) ) {
			if ( !element.getKind().equals( ElementKind.METHOD ) ) {
				continue;
			}
			if ( elementUtils.overrides( current, (ExecutableElement) element, currentTypeElement ) ) {
				return (ExecutableElement) element;
			}
		}

		return null;
	}

	/**
	 * Find a {@link TypeElement} that enclose a given {@link ExecutableElement}.
	 *
	 * @param currentMethod a method that you want to find class/interface it belongs to
	 *
	 * @return a class/interface represented by {@link TypeElement} to which a method belongs to
	 */
	private TypeElement getEnclosingTypeElement(ExecutableElement currentMethod) {
		return (TypeElement) typeUtils.asElement( currentMethod.getEnclosingElement().asType() );
	}

}