/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validator.internal.metadata.aggregated;

import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

import javax.validation.metadata.BeanDescriptor;

import org.hibernate.validator.internal.metadata.core.MetaConstraint;
import org.hibernate.validator.internal.metadata.raw.ExecutableElement;

/**
 * Interface defining the meta data about the constraints defined in a given bean.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public interface BeanMetaData<T> {

	/**
	 * @return the class of the bean.
	 */
	Class<T> getBeanClass();

	/**
	 * @return an instance of {@code ElementDescriptor} describing the bean this meta data applies for.
	 */
	BeanDescriptor getBeanDescriptor();

	/**
	 * @return A list of all cascaded methods and fields (methods/fields annotated with &#064;Valid).
	 */
	Set<Member> getCascadedMembers();

	/**
	 * Get the composition of the default group sequence.
	 * <p>
	 * If the bean state is given in parameter and the bean metadata has a default group sequence provider then the
	 * dynamic default group sequence composition is returned. In the other cases the default group sequence
	 * redefinition specified by BV is used.
	 * </p>
	 *
	 * @param beanState the bean state.
	 *
	 * @return a list of classes representing the default group sequence.
	 */
	List<Class<?>> getDefaultGroupSequence(T beanState);

	/**
	 * @return {@code true} if the entity redefines the default group sequence, {@code false} otherwise.
	 */
	boolean defaultGroupSequenceIsRedefined();

	/**
	 * @return A set of {@code MetaConstraint} instances encapsulating the information of all the constraints
	 *         defined on the bean. This collection includes constraints from super classes as well
	 */
	Set<MetaConstraint<?>> getMetaConstraints();

	/**
	 * @return A set of {@code MetaConstraint} instances encapsulating the information of all the constraints
	 *         defined on the bean directly (including constraints defined on implemented interfaces). It does not
	 *         contain constraints from super classes or interfaces implemented by super classes
	 */
	Set<MetaConstraint<?>> getDirectMetaConstraints();

	/**
	 * Returns the constraint-related meta data for the given method of the
	 * class represented by this bean meta data.
	 *
	 * @param method The method of interest.
	 *
	 * @return An aggregated view on the constraint related meta data from the
	 *         given method all the methods from super-types which it overrides
	 *         or implements.
	 */
	ExecutableMetaData getMetaDataFor(ExecutableElement method);

	/**
	 * @param name The name of the property
	 *
	 * @return true if the property exists on the object
	 *         even if the property does not host any constraint nor is cascaded
	 */
	boolean isPropertyPresent(String name);

	/**
	 * @return Returns a list of classes representing the class hierarchy for the entity. The list start with the
	 *         element itself and goes up the hierarchy chain. Interfaces are not included.
	 */
	List<Class<?>> getClassHierarchy();
}
