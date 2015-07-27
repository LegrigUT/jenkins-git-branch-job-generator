/**
 * 
 */
package com.legrig.jenkinsci.plugins.GitBranchJobGenerator;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author mlasevich
 *
 */
@ExportedBean
public class BranchMatchPattern  extends AbstractDescribableImpl<BranchMatchPattern> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1051612356191860185L;
	private String pattern;
	
	/**
	 * 
	 */
	@DataBoundConstructor
	public BranchMatchPattern(String pattern) {
		this.pattern=pattern;
	}

	
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public String getPattern() {
		return pattern;
	}

	@Extension
    public static class DescriptorImpl extends Descriptor<BranchMatchPattern> {

		@Override
		public String getDisplayName() {
			return "Branch Match Pattern";
		}
		 
		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws hudson.model.Descriptor.FormException {
			return super.configure(req, json);
		}
	 }

}
