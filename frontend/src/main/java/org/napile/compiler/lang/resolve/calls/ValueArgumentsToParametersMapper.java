/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.lang.resolve.calls;

import static org.napile.compiler.lang.diagnostics.Errors.ARGUMENT_PASSED_TWICE;
import static org.napile.compiler.lang.diagnostics.Errors.MANY_FUNCTION_LITERAL_ARGUMENTS;
import static org.napile.compiler.lang.diagnostics.Errors.MIXING_NAMED_AND_POSITIONED_ARGUMENTS;
import static org.napile.compiler.lang.diagnostics.Errors.NAMED_PARAMETER_NOT_FOUND;
import static org.napile.compiler.lang.diagnostics.Errors.TOO_MANY_ARGUMENTS;
import static org.napile.compiler.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.ERROR;
import static org.napile.compiler.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.OK;
import static org.napile.compiler.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.WEAK_ERROR;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.psi.Call;
import org.napile.compiler.lang.psi.NapileAnonymMethodExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileSimpleNameExpression;
import org.napile.compiler.lang.psi.ValueArgument;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.TemporaryBindingTrace;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author abreslav
 */
/*package*/ class ValueArgumentsToParametersMapper
{

	public enum Status
	{
		STRONG_ERROR(false),
		ERROR(false),
		WEAK_ERROR(false),
		OK(true);

		private final boolean success;

		private Status(boolean success)
		{
			this.success = success;
		}

		public boolean isSuccess()
		{
			return success;
		}

		public Status compose(Status other)
		{
			if(this == STRONG_ERROR || other == STRONG_ERROR)
				return STRONG_ERROR;
			if(this == ERROR || other == ERROR)
				return ERROR;
			if(this == WEAK_ERROR || other == WEAK_ERROR)
				return WEAK_ERROR;
			return this;
		}
	}

	public static <D extends CallableDescriptor> Status mapValueArgumentsToParameters(@NotNull Call call, @NotNull TracingStrategy tracing, @NotNull ResolvedCallImpl<D> candidateCall, @NotNull Set<ValueArgument> unmappedArguments)
	{
		TemporaryBindingTrace temporaryTrace = candidateCall.getTrace();
		Map<CallParameterDescriptor, VarargValueArgument> varargs = Maps.newHashMap();
		Set<CallParameterDescriptor> usedParameters = Sets.newHashSet();

		D candidate = candidateCall.getCandidateDescriptor();

		List<CallParameterDescriptor> valueParameters = candidate.getValueParameters();
		/*if(AnnotationUtils.hasAnnotation(candidate, NapileAnnotationPackage.EXTENSION))
		{
			valueParameters = new ArrayList<CallParameterDescriptor>(valueParameters);
			if(!valueParameters.isEmpty())
				valueParameters.remove(0);
		}  */

		Map<Name, CallParameterDescriptor> parameterByName = Maps.newHashMap();
		for(CallParameterDescriptor valueParameter : valueParameters)
		{
			parameterByName.put(valueParameter.getName(), valueParameter);
		}

		List<? extends ValueArgument> valueArguments = call.getValueArguments();

		Status status = OK;
		boolean someNamed = false;
		boolean somePositioned = false;
		for(int i = 0; i < valueArguments.size(); i++)
		{
			ValueArgument valueArgument = valueArguments.get(i);
			if(valueArgument.isNamed())
			{
				someNamed = true;
				NapileSimpleNameExpression nameReference = valueArgument.getArgumentName().getReferenceExpression();
				CallParameterDescriptor parameterDescriptor = parameterByName.get(nameReference.getReferencedNameAsName());
				if(parameterDescriptor == null)
				{
					temporaryTrace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference.getText()));
					unmappedArguments.add(valueArgument);
					status = WEAK_ERROR;
				}
				else
				{
					temporaryTrace.record(BindingTraceKeys.REFERENCE_TARGET, nameReference, parameterDescriptor);
					if(!usedParameters.add(parameterDescriptor))
					{
						temporaryTrace.report(ARGUMENT_PASSED_TWICE.on(nameReference));
						unmappedArguments.add(valueArgument);
						status = WEAK_ERROR;
					}
					else
					{
						status = status.compose(put(candidateCall, parameterDescriptor, valueArgument, varargs));
					}
				}
				if(somePositioned)
				{
					temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(nameReference));
					status = WEAK_ERROR;
				}
			}
			else
			{
				somePositioned = true;
				if(someNamed)
				{
					temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(valueArgument.asElement()));
					status = WEAK_ERROR;
				}
				else
				{
					int parameterCount = valueParameters.size();
					if(i < parameterCount)
					{
						CallParameterDescriptor parameterDescriptor = valueParameters.get(i);
						usedParameters.add(parameterDescriptor);
						status = status.compose(put(candidateCall, parameterDescriptor, valueArgument, varargs));
					}
					else if(!valueParameters.isEmpty())
					{
						CallParameterDescriptor parameterDescriptor = valueParameters.get(valueParameters.size() - 1);
						/*if(parameterDescriptor.getVarargElementType() != null)
						{
							status = status.compose(put(candidateCall, parameterDescriptor, valueArgument, varargs));
							usedParameters.add(parameterDescriptor);
						}
						else  */
						{
							temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
							unmappedArguments.add(valueArgument);
							status = WEAK_ERROR;
						}
					}
					else
					{
						temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
						unmappedArguments.add(valueArgument);
						status = ERROR;
					}
				}
			}
		}

		List<NapileExpression> functionLiteralArguments = call.getFunctionLiteralArguments();
		if(!functionLiteralArguments.isEmpty())
		{
			NapileExpression possiblyLabeledFunctionLiteral = functionLiteralArguments.get(0);

			if(valueParameters.isEmpty())
			{
				temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
				status = ERROR;
			}
			else
			{
				NapileAnonymMethodExpression functionLiteral = (NapileAnonymMethodExpression) possiblyLabeledFunctionLiteral;

				CallParameterDescriptor parameterDescriptor = valueParameters.get(valueParameters.size() - 1);
				/*if(parameterDescriptor.getVarargElementType() != null)
				{
					temporaryTrace.report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
					status = ERROR;
				}
				else  */
				{
					if(!usedParameters.add(parameterDescriptor))
					{
						temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
						status = WEAK_ERROR;
					}
					else
					{
						status = status.compose(put(candidateCall, parameterDescriptor, CallMaker.makeValueArgument(functionLiteral), varargs));
					}
				}
			}

			for(int i = 1; i < functionLiteralArguments.size(); i++)
			{
				NapileExpression argument = functionLiteralArguments.get(i);
				temporaryTrace.report(MANY_FUNCTION_LITERAL_ARGUMENTS.on(argument));
				status = WEAK_ERROR;
			}
		}


		for(CallParameterDescriptor valueParameter : valueParameters)
		{
			if(!usedParameters.contains(valueParameter))
			{
				if(valueParameter.hasDefaultValue())
					candidateCall.recordValueArgument(valueParameter, new DefaultValueArgument(temporaryTrace.safeGet(BindingTraceKeys.DEFAULT_VALUE_OF_PARAMETER, valueParameter)));
				//else if(valueParameter.getVarargElementType() != null)
				//	candidateCall.recordValueArgument(valueParameter, new VarargValueArgument());
				else
				{
					// tracing.reportWrongValueArguments(temporaryTrace, "No value passed for parameter " + valueParameter.getName());
					tracing.noValueForParameter(temporaryTrace, valueParameter);
					status = ERROR;
				}
			}
		}

		//assert (candidateCall.getThisObject().exists() == candidateCall.getResultingDescriptor().getExpectedThisObject().exists()) : "Shouldn't happen because of TaskPrioritizer: " + candidateCall.getCandidateDescriptor();

		return status;
	}

	private static <D extends CallableDescriptor> Status put(ResolvedCallImpl<D> candidateCall, CallParameterDescriptor parameterDescriptor, ValueArgument valueArgument, Map<CallParameterDescriptor, VarargValueArgument> varargs)
	{
		Status error = OK;
		/*if(parameterDescriptor.getVarargElementType() != null)
		{
			VarargValueArgument vararg = varargs.get(parameterDescriptor);
			if(vararg == null)
			{
				vararg = new VarargValueArgument();
				varargs.put(parameterDescriptor, vararg);
				candidateCall.recordValueArgument(parameterDescriptor, vararg);
			}
			vararg.addArgument(valueArgument);
		}
		else*/
		{
			candidateCall.recordValueArgument(parameterDescriptor, new ExpressionValueArgument(valueArgument));
		}
		return error;
	}
}
