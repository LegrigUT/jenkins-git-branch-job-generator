/**
 * 
 */
package com.legrig.jenkinsci.plugins.GitBranchJobGenerator;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author mlasevich
 *
 */
@ExportedBean
public class BranchRule  extends AbstractDescribableImpl<BranchRule> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5642698046334746381L;
	
	private List<BranchMatchPattern> matchPatterns;
	private String branchType;
	private boolean create;
	private String templateProject;
	private String projectName;
	private String projectDisplayName;
	private String projectDescription;
	private boolean enableOnCreate;
	private boolean disableOnDelete;
	private boolean runOnCreate;
	private boolean injectEnvVariables;
	
	/**
	 * 
	 */
	@DataBoundConstructor
	public BranchRule(
			List<BranchMatchPattern> matchPatterns,
			boolean create,
			String branchType, 
			String templateProject, 
			String projectName, String projectDisplayName, 
			String projectDescription, 
			boolean enableOnCreate, 
			boolean disableOnDelete,
			boolean runOnCreate,
			boolean injectEnvVariables) {
		this.create=create;
		this.matchPatterns=matchPatterns;
		this.branchType=branchType;
		this.templateProject=templateProject;
		this.projectName=projectName;
		this.projectDisplayName=projectDisplayName;
		this.projectDescription=projectDescription;
		this.enableOnCreate=enableOnCreate;
		this.disableOnDelete=disableOnDelete;
		this.runOnCreate=runOnCreate;
		this.injectEnvVariables=injectEnvVariables;
		
	}

	public List<BranchMatchPattern> getMatchPatterns() {
		return matchPatterns;
	}

	public void setMatchPatterns(List<BranchMatchPattern> matchPatterns) {
		this.matchPatterns = matchPatterns;
	}

	public String getBranchType() {
		return branchType;
	}

	public void setBranchType(String branchType) {
		this.branchType = branchType;
	}

	public boolean isCreate() {
		return create;
	}

	public boolean getCreate() {
		return create;
	}

	
	public void setCreate(boolean create) {
		this.create = create;
	}

	public boolean isEnableOnCreate() {
		return enableOnCreate;
	}

	public boolean getEnableOnCreate() {
		return enableOnCreate;
	}

	public void setEnableOnCreate(boolean enableOnCreate) {
		this.enableOnCreate = enableOnCreate;
	}

	public boolean isDisableOnDelete() {
		return disableOnDelete;
	}

	public boolean getDisableOnDelete() {
		return disableOnDelete;
	}

	public void setDisableOnDelete(boolean disableOnDelete) {
		this.disableOnDelete = disableOnDelete;
	}
	

	public String getTemplateProject() {
		return templateProject;
	}

	public void setTemplateProject(String templateProject) {
		this.templateProject = templateProject;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectDisplayName() {
		return projectDisplayName;
	}

	public void setProjectDisplayName(String projectDisplayName) {
		this.projectDisplayName = projectDisplayName;
	}

	public String getProjectDescription() {
		return projectDescription;
	}

	public void setProjectDescription(String projectDescription) {
		this.projectDescription = projectDescription;
	}

	public boolean isRunOnCreate() {
		return runOnCreate;
	}
	
	public boolean getRunOnCreate() {
		return runOnCreate;
	}
	
	public void setRunOnCreate(boolean runOnCreate) {
		this.runOnCreate = runOnCreate;
	}

	public boolean isInjectEnvVariables() {
		return injectEnvVariables;
	}
	
	public boolean getInjectEnvVariables() {
		return injectEnvVariables;
	}
	
	public void setInjectEnvVariables(boolean injectEnvVariables) {
		this.injectEnvVariables = injectEnvVariables;
	}
	@Extension
    public static class DescriptorImpl extends Descriptor<BranchRule> {

		@Override
		public String getDisplayName() {
			return "Branch Rule";
		}
		
		public Set<String> getTemplateProjects(){
			GitBranchJobGeneratorBuilder.DescriptorImpl descriptor = Jenkins.getInstance().getDescriptorByType(GitBranchJobGeneratorBuilder.DescriptorImpl.class);
        	return  descriptor.getTemplateProjects();
        }
		 
	 }
}
