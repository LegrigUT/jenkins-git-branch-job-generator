/**
 * 
 */
package com.legrig.jenkinsci.plugins.EnvParameters;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.export.ExportedBean;

import com.legrig.jenkinsci.plugins.EnvParameters.EnvSetBuildWrapper.DescriptorImpl;

/**
 * @author mlasevich
 *
 */
@ExportedBean
public class EnvParameter  extends AbstractDescribableImpl<EnvParameter> implements Serializable {
	
	private String parameterName;
	private String parameterValue;
	private boolean literal;
	private boolean noOverwrite;
	
	
	@DataBoundConstructor
	public EnvParameter(String parameterName,String parameterValue, boolean literal, boolean noOverwrite) {
		 this.parameterName=parameterName;
		 this.parameterValue=parameterValue;
		 this.literal=literal;
		 this.noOverwrite=noOverwrite;
	}
	
	public FormValidation doCheckParameterName(@QueryParameter final String value) {
		return this.getDescriptor().doCheckParameterName(value);
	}
	
	
	
	public String getParameterName() {
		return parameterName;
	}
	
	public String getEnvParameter() {
		return parameterName.replace("[^A-Za-z0-9_]", "_");
	}
	
	public String getParameterValue() {
		return parameterValue;
	}
	
	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}
	public void setParameterValue(String parameterValue) {
		this.parameterValue = parameterValue;
	}
	public boolean isLiteral() {
		return literal;
	}
	
	public boolean getLiteral() {
		return literal;
	}
	
	public boolean isNoOverwrite() {
		return noOverwrite;
	}
	public boolean getNoOverwrite() {
		return noOverwrite;
	}
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl)super.getDescriptor();
	}

	@Extension
    public static class DescriptorImpl extends Descriptor<EnvParameter> {

		public DescriptorImpl() {
			super(EnvParameter.class);
			load();
		}
		@Override
		public String getDisplayName() {
			return "Fixed Env Parameter";
		}
		
		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 * 
		 * @param value
		 *            This receives the current value of the field.
		 */
		public FormValidation doCheckParameterName(@QueryParameter final String value) {
			if (value.length() == 0) {
				return FormValidation
						.error("Please set an Parameter Name");
			}else if (value.matches("[^a-zA-Z0-9_]")) {
					return FormValidation
							.error("Parameter name can only contain letters, numbers, and _ characters.");
			} else{ 
				return FormValidation.ok();
			}
		}
		/**
		 * Performs on-the-fly validation of the form field 'name'.
		 * 
		 * @param value
		 *            This receives the current value of the field.
		 */
		public FormValidation doCheckParameterValue(@QueryParameter final String value) {
			return FormValidation.ok();
		}
		 
	 }
}

