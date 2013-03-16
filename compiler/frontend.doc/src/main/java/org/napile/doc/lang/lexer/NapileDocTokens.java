/*
 * Copyright 2010-2013 napile.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.doc.lang.lexer;

/**
 * @author VISTALL
 * @since 22:46/30.01.13
 */
public interface NapileDocTokens
{
	NapileDocToken DOC_START = new NapileDocToken("DOC_START");

	NapileDocToken DOC_END = new NapileDocToken("DOC_END");

	NapileDocToken NEW_LINE = new NapileDocToken("NEW_LINE");

	NapileDocToken WHITE_SPACE = new NapileDocToken("WHITE_SPACE");

	NapileDocToken TEXT_PART = new NapileDocToken("TEXT_PART");

	NapileDocToken TILDE = new NapileDocToken("TILDE");

	NapileDocToken CODE_MARKER = new NapileDocToken("CODE_MARKER");
}
