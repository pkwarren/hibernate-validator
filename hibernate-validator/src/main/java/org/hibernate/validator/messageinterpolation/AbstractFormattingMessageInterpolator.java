/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates, and individual contributors
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

package org.hibernate.validator.messageinterpolation;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.MessageInterpolator;


public abstract class AbstractFormattingMessageInterpolator implements MessageInterpolator {
	public static final String VALIDATED_VALUE_KEYWORD = "validatedValue";
	public static final String VALIDATED_VALUE_FORMAT_SEPARATOR = ":";

	private static final Pattern VALIDATED_VALUE_START_PATTERN = Pattern.compile( "\\$\\{" + VALIDATED_VALUE_KEYWORD );
	private final MessageInterpolator delegate;

	public AbstractFormattingMessageInterpolator() {
		this( null );
	}

	public AbstractFormattingMessageInterpolator(MessageInterpolator userMessageInterpolator) {
		if ( userMessageInterpolator == null ) {
			this.delegate = new ResourceBundleMessageInterpolator();
		}
		else {
			this.delegate = userMessageInterpolator;
		}
	}

	public String interpolate(String message, Context context) {
		return interpolateMessage( delegate.interpolate( message, context ), context.getValidatedValue() );
	}

	public String interpolate(String message, Context context, Locale locale) {
		return interpolateMessage( delegate.interpolate( message, context, locale ), context.getValidatedValue() );
	}

	/**
	 * Interpolate the validated value in the given message
	 *
	 * @param message the message where validated value have to be interpolated
	 * @param validatedValue the value of the object being validated
	 *
	 * @return the interpolated message
	 */
	private String interpolateMessage(String message, Object validatedValue) {
		String interpolatedMessage = message;
		Matcher matcher = VALIDATED_VALUE_START_PATTERN.matcher( message );

		while ( matcher.find() ) {
			int nbOpenCurlyBrace = 1;
			boolean isDoubleQuoteBloc = false;
			boolean isSimpleQuoteBloc = false;
			int lastIndex = matcher.end();

			do {

				char current = message.charAt( lastIndex );

				if ( current == '\'' ) {
					if ( !isDoubleQuoteBloc && !isEscaped( message, lastIndex ) ) {
						isSimpleQuoteBloc = !isSimpleQuoteBloc;
					}
				}
				else if ( current == '"' ) {
					if ( !isSimpleQuoteBloc && !isEscaped( message, lastIndex ) ) {
						isDoubleQuoteBloc = !isDoubleQuoteBloc;
					}
				}
				else if ( !isDoubleQuoteBloc && !isSimpleQuoteBloc ) {
					if ( current == '{' ) {
						nbOpenCurlyBrace++;
					}
					else if ( current == '}' ) {
						nbOpenCurlyBrace--;
					}
				}

				lastIndex++;

			} while ( nbOpenCurlyBrace > 0 && lastIndex < message.length() );

			//The validated value expression seems correct
			if ( nbOpenCurlyBrace == 0 ) {
				String expression = message.substring( matcher.start(), lastIndex );
				String expressionValue = interpolateValidatedValue( expression, validatedValue );
				interpolatedMessage = interpolatedMessage.replaceFirst(
						Pattern.quote( expression ), Matcher.quoteReplacement( expressionValue )
				);
			}
		}

		return interpolatedMessage;
	}

	/**
	 * Returns if the character at the given index in the String
	 * is an escaped character (preceded by a backslash character).
	 *
	 * @param enclosingString the string which contain the given character
	 * @param charIndex the index of the character
	 *
	 * @return true if the given character is escaped false otherwise
	 */
	private boolean isEscaped(String enclosingString, int charIndex) {
		if ( charIndex < 0 || charIndex > enclosingString.length() ) {
			throw new IndexOutOfBoundsException( "The given index must be between 0 and enclosingString.length() - 1" );
		}
		return charIndex > 0 && enclosingString.charAt( charIndex - 1 ) == '\\';
	}

	/**
	 * Returns the value of the interpolated validated value.
	 *
	 * @param expression the expression to interpolate
	 * @param validatedValue the value of the object being validated
	 *
	 * @return the interpolated value
	 */
	abstract String interpolateValidatedValue(String expression, Object validatedValue);

}
