package com.legrig.jenkinsci.plugins.GitBranchJobGenerator;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.plugins.git.Branch;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitAPI;
import hudson.plugins.git.IGitAPI;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.GitTool;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.legrig.jenkinsci.plugins.EnvParameters.EnvParameter;
import com.legrig.jenkinsci.plugins.EnvParameters.EnvSetBuildWrapper;

/**
 * Sample {@link Builder}.
 * 
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link GitBranchJobGeneratorBuilder} is created. The created instance is
 * persisted to the project configuration XML by using XStream, so this allows
 * you to use instance fields (like {@link #name}) to remember the
 * configuration.
 * 
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 */
public class GitBranchJobGeneratorBuilder extends Builder {
	private Log log = LogFactory.getLog(this.getClass());

	private List<BranchRule> branchRules;
	private String gitURL="";
	private boolean verbose=false;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public GitBranchJobGeneratorBuilder(String gitURL,
			List<BranchRule> branchRules, boolean verbose) {
		this.branchRules = branchRules;
		this.gitURL = gitURL;
		this.verbose = verbose;
		//log.info("Saving branch rules: " + branchRules.size());
	}

	public List<BranchRule> getBranchRules() {
		return branchRules;
	}

	public String getGitURL() {
		return gitURL;
	}

	public boolean isVerbose() {
		return verbose;
	}
	
	public void setBranchRules(List<BranchRule> branchRules) {
		this.branchRules = branchRules;
	}
	
	public void setGitURL(String gitURL) {
		this.gitURL = gitURL;
	}
	
	public void  verbose(PrintStream out, String msg){
		if (isVerbose()){
			out.println("VERBOSE: "+msg);
		}
	}
	
	private String cleanName(String rawName){
		return cleanName(rawName, "[^A-Za-z0-9-_.]", "_");
	}
	
	private String cleanName(String rawName, String regex, String replacement){
		return rawName.replaceAll(regex, replacement);
	}
	public EnvVars generateCurrentTimeEnvVars(){
		EnvVars newJobEnv=new EnvVars();
		Date now=new Date();
		Calendar rightNow = Calendar.getInstance();
		newJobEnv.put("DAY", String.valueOf(rightNow.get(Calendar.DAY_OF_MONTH)));
		newJobEnv.put("MONTH", String.valueOf(rightNow.get(Calendar.MONTH)));
		newJobEnv.put("YEAR", String.valueOf(rightNow.get(Calendar.YEAR)));
		newJobEnv.put("DATE", String.valueOf(rightNow.get(Calendar.DAY_OF_MONTH)+
				"/"+String.valueOf(rightNow.get(Calendar.MONTH)+1)+
				"/"+String.valueOf(rightNow.get(Calendar.YEAR)))
				);
		
		newJobEnv.put("TIME", String.valueOf(rightNow.get(Calendar.HOUR_OF_DAY))+
				":"+String.valueOf(rightNow.get(Calendar.MINUTE))+
				":"+String.valueOf(rightNow.get(Calendar.SECOND))
				//+" "+String.valueOf(rightNow.get(Calendar.AM_PM))
				);
		return newJobEnv;
	}
	
	public EnvVars generatePerBranchEnvVars(String branchFull, String branchProper, String branchEffective, String type){
		EnvVars newJobEnv=new EnvVars();
		newJobEnv.put("TYPE", type);
		newJobEnv.put("TYPE_CAP", StringUtils.capitalize(type));
		newJobEnv.put("BRANCH_FULL", branchFull);
		newJobEnv.put("BRANCH_PROPER", branchProper);
		newJobEnv.put("BRANCH", branchEffective);
		newJobEnv.put("BRANCH_CLEAN", cleanName(branchEffective));
		newJobEnv.put("BRANCH_PROPER_CLEAN", cleanName(branchProper));
		return newJobEnv;
	}
	
	/*
	public String resolveName(String pattern, String branchFull, String branch, String type, EnvVars baseVars){
		String ret=pattern;
		
				ret=newJobEnv.expand(ret);
		return ret;
				
	}*/
	
	
	
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		boolean delayedError=false;
		log.info("Performing build");
		Jenkins jenkins=Jenkins.getInstance();
		PrintStream out = listener.getLogger();
		out.println("Checking GIT for branches:");
		
		try{
			gitURL=null;
		SCM scm = build.getProject().getScm();
		FilePath ws = build.getWorkspace();	
		if (gitURL == null || gitURL.isEmpty()) {
				if (scm instanceof GitSCM){
					GitSCM gitScm=(GitSCM)scm;
					List<UserRemoteConfig> remConfs = gitScm.getUserRemoteConfigs();
					if (remConfs.size()==0){
						listener.fatalError("No Git repositories defined...");
					}
					UserRemoteConfig remConf = remConfs.get(0);
					gitURL=remConf.getUrl();
					String relDir=gitScm.getRelativeTargetDir();
					if (relDir != null && relDir.length() > 0) {
			            ws=ws.child(relDir);
			        }
				}

		}
		
		
		// get git executable on master
        final EnvVars environment = new EnvVars(System.getenv()); // GitUtils.getPollEnvironment(project, null, launcher, TaskListener.NULL, false);
       
		GitTool.DescriptorImpl descriptor = jenkins.getDescriptorByType(GitTool.DescriptorImpl.class);
		String gitExe = descriptor.getInstallations()[0].forNode(jenkins, TaskListener.NULL).getGitExe();
		//String branches=launchCommandIn(gitExe, launcher, args, build.getWorkspace(), environment);
		//TaskListener.NULL
		IGitAPI git = new GitAPI(gitExe, ws, TaskListener.NULL, environment,null);
		String remDef=git.getDefaultRemote("");
		
		Set<Branch> allBranches=git.getRemoteBranches();
		
		Collection<String> allJobs=jenkins.getJobNames();
		Set<String> remainingJobs=new TreeSet<String>();
		remainingJobs.addAll(allJobs);
		for (Branch branch:allBranches){
			boolean matched=false;
			String branchNameFull=branch.getName();
			if (branchNameFull.equals(remDef+"/HEAD")){
				verbose(out, "Skipping HEAD link");
				continue;
			}
			verbose(out, "Examining branch: '"+branch.getName()+"'");
			if (branchNameFull.startsWith("remotes/")) branchNameFull=branchNameFull.substring("remotes/".length());
			
			String branchNameProper=branchNameFull;
			if (branchNameProper.startsWith(remDef+"/")){
				branchNameProper=branchNameProper.substring(remDef.length()+1);
			}
			//TODO: change effective to be config driven
			String branchNameEffective=branchNameProper;
			if (branchNameEffective.startsWith("release/")){
				branchNameEffective=branchNameEffective.substring("release/".length());
			}
			verbose(out, "Examining branch: '"+branch.getName()+"'");
			for (BranchRule rule:branchRules){
				verbose(out, ".... Examining rule "+rule.getBranchType());
				for (BranchMatchPattern pattern:rule.getMatchPatterns()){
					if (branchNameProper.matches(pattern.getPattern())){
						verbose(out, ".... .... Matched branch '"+branchNameProper+"' to pattern '"+pattern.getPattern()+"' or rule for type "+rule.getBranchType());
						matched=true;
						if (rule.isCreate()){
							
							EnvVars newJobBranchEnvOnly=generatePerBranchEnvVars(branchNameFull,branchNameProper, branchNameEffective, rule.getBranchType());
							EnvVars newJobEnvOnly=generateCurrentTimeEnvVars();
							newJobEnvOnly.putAll(newJobBranchEnvOnly);
							
							EnvVars newJobEnv=new EnvVars(build.getEnvironment(listener));
							newJobEnv.putAll(newJobEnvOnly);
							
							String newJobName=newJobEnv.expand(rule.getProjectName());
							String newJobDisplayName=newJobEnv.expand(rule.getProjectDisplayName());
							String newJobDescription=newJobEnv.expand(rule.getProjectDescription());
							
							
							if (! allJobs.contains(newJobName)){
								
								out.println("..... Creating job: "+newJobName);
								verbose(out, "..... 	Job Type: "+rule.getBranchType());
								verbose(out, "..... 	Display Name: "+newJobDisplayName);
								verbose(out, "..... 	Description: "+newJobDisplayName);
								
								TopLevelItem templateProject = jenkins.getItem(rule.getTemplateProject());
								if (templateProject != null && templateProject instanceof AbstractProject){
									Project newJob = (Project) jenkins.copy(templateProject, newJobName);
									if (! StringUtils.isBlank(newJobDisplayName)){
										newJob.setDisplayName(newJobDisplayName);
									}
									if (! StringUtils.isBlank(newJobDescription)){
										newJob.setDescription(newJobDescription);
									}
									if (newJob!=null){
										
										SCM olsJobScm=newJob.getScm();
										GitSCM tempSCM;
										if (olsJobScm instanceof GitSCM){
											verbose(out, ".... .... Copying existing GIT settings...");
											tempSCM=(GitSCM)olsJobScm;
										}else{
											verbose(out, ".... .... Adding new GIT settings...");
											tempSCM=new GitSCM(gitURL);
										}
										List<UserRemoteConfig> remConfs=new ArrayList<UserRemoteConfig>();
										
										UserRemoteConfig oldRemoteConfig;
										UserRemoteConfig newRemoteConfig;
							
										List<UserRemoteConfig> oldRemConfs=tempSCM.getUserRemoteConfigs();
										
										if (rule.getInjectEnvVariables()){
											verbose(out, "....  Configuring injectEnvVariables...");
											DescribableList wrappers = newJob.getBuildWrappersList();
											EnvSetBuildWrapper.DescriptorImpl envInjectDesc = jenkins.getDescriptorByType(EnvSetBuildWrapper.DescriptorImpl.class);
											EnvSetBuildWrapper envInjector=(EnvSetBuildWrapper) wrappers.get(envInjectDesc);
											
											if (envInjector==null){
												verbose(out, "....  Adding new wrapper...");
												envInjector=new EnvSetBuildWrapper(new ArrayList<EnvParameter>());
												wrappers.add(envInjector);
											}
											envInjector.addParameters(newJobBranchEnvOnly);
										
										}else{
											verbose(out, "....  Skipping injectEnvVariables...");
										}
										
										
										if (remConfs.size()>0){
											oldRemoteConfig = oldRemConfs.get(0);
											newRemoteConfig=new UserRemoteConfig(gitURL, oldRemoteConfig.getName(), oldRemoteConfig.getRefspec());
											remConfs.add(newRemoteConfig);
											for (int i=1;i<oldRemConfs.size();i++){
												oldRemoteConfig=oldRemConfs.get(i);
												remConfs.add(
														new UserRemoteConfig(oldRemoteConfig.getUrl(), oldRemoteConfig.getName(), oldRemoteConfig.getRefspec())
														);
											}
										}else{
											newRemoteConfig=new UserRemoteConfig(gitURL, "", "");
											remConfs.add(newRemoteConfig);
										}
										
										List<BranchSpec> gitBranchSpecs= new ArrayList<BranchSpec>();
										gitBranchSpecs.add(new BranchSpec(branchNameFull));
										GitSCM newJobScm=new GitSCM(
												"", 
												remConfs, 
												gitBranchSpecs,
												tempSCM.getUserMergeOptions(), 
												tempSCM.getDoGenerate(),
												tempSCM.getSubmoduleCfg(),
												tempSCM.getClean(), 
												tempSCM.getWipeOutWorkspace(), 
												tempSCM.getBuildChooser(),
												tempSCM.getBrowser(),
												tempSCM.getGitTool(), 
												tempSCM.getAuthorOrCommitter(), 
												tempSCM.getRelativeTargetDir(), 
												tempSCM.getReference(), 
												tempSCM.getExcludedRegions(),
												tempSCM.getExcludedUsers(),
												tempSCM.getLocalBranch(),
												tempSCM.getDisableSubmodules(),
												tempSCM.getRecursiveSubmodules(),
												tempSCM.getPruneBranches(),
												tempSCM.getRemotePoll(),
												tempSCM.getGitConfigName(),
												tempSCM.getGitConfigEmail(),
												tempSCM.getSkipTag(), 
												tempSCM.getIncludedRegions(), 
												tempSCM.isIgnoreNotifyCommit(), 
												tempSCM.getUseShallowClone()
												);
										
										if (rule.isEnableOnCreate()){
											verbose(out, ".... .... Enabling job "+newJobName);
											newJob.enable();
										}else{
											verbose(out, ".... .... Not Enabling job "+newJobName);
										}
										newJob.setScm(newJobScm);
										newJob.save();
										
										if (rule.isRunOnCreate()){
											verbose(out, ".... .... Executing job "+newJobName);
											List<Cause> causes = build.getCauses();
											Cause cause;
											if (causes.size()>0){
												cause=causes.get(0);
											}else{
												cause=new Cause.UserIdCause();
											}
											newJob.scheduleBuild(cause);
										}else{
											verbose(out, ".... .... Not Executing job "+newJobName);
										}
									}else{
										out.println(" ERROR: Something went wrong while creating new job. Unable to find it after creation.");
										verbose(out, ".... .... ERROR: Delayed Error in job "+newJobName);
										delayedError=true;
									}
								}else{
									out.println(" ERROR: No template project '"+rule.getTemplateProject()+"' found. Unable to create new job.");
									verbose(out, "Delayed Error in job "+newJobName);
									delayedError=true;
								}	
							}else{
								verbose(out, "....  Job '"+newJobName+"' already exists");
							}
							
						}else{
							verbose(out, ".... Ignoring branch "+branchNameProper+" as per ignore rule.");
						}
						break;
					}else{
						verbose(out, ".... No Match for branch '"+branchNameProper+"' to pattern '"+pattern.getPattern()+"' or rule for type "+rule.getBranchType());
					}
					if (matched) break;
				}
			}
		}
		
		} catch (IOException e) {
			e.printStackTrace(out);
			listener.fatalError("Failed to connect to git: " + e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace(out);
			listener.fatalError("Failed to connect to git: " + e.getMessage());
		}finally{
			
		}
		if (delayedError){
			listener.fatalError("Exiting due to delayed error...");
		}
		return true;
	}

	// Overridden for better type safety.
	// If your plugin doesn't really define any property on Descriptor,
	// you don't have to do this.
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	/**
	 * Descriptor for {@link GitBranchJobGeneratorBuilder}. Used as a singleton.
	 * The class is marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {
		/**
		 * To persist global configuration information, simply store it in a
		 * field and call save().
		 * 
		 * <p>
		 * If you don't want fields to be persisted, use <tt>transient</tt>.
		 */
		private String templatePattern = ".*";
		private boolean templateDisabledOnly = true;
		private Log log = LogFactory.getLog(this.getClass());

		public DescriptorImpl() {
			super(GitBranchJobGeneratorBuilder.class);
			load();
		}

		public FormValidation doCheckTemplatePattern(
				@QueryParameter String value) throws IOException,
				ServletException {
			if (getTemplateProjects(value, false).size() == 0) {
				return FormValidation
						.warning("No Current Jobs Matching this pattern!");
			}
			return FormValidation.ok();
		}

		public Set<String> getTemplateProjects(String pattern,
				boolean disabledOnly) {
			Collection<String> jobNames = Jenkins.getInstance().getJobNames();
			List<TopLevelItem> jobs = Jenkins.getInstance().getItems();
			Set<String> ret = new TreeSet<String>();

			for (TopLevelItem item : jobs) {
				// log.info("Checking item: "+item.getName()+
				// " ("+item.getAllJobs().size()+")");

				if (item instanceof AbstractProject) {
					AbstractProject job = (AbstractProject) item;
					// log.info("Checking item: "+item.getName()+" is a job - Disabled: "+job.isDisabled()+" Show Disabled Only: "+disabledOnly+" Pattern: "+pattern);
					String jobName = job.getName();
					if ((!disabledOnly || job.isDisabled())
							&& jobName.matches(pattern)) {
						ret.add(jobName);
						// log.info("........MATCH!!!");
					}
				}
			}
			return ret;
		}

		public Set<String> getTemplateProjects() {
			return getTemplateProjects(templatePattern, templateDisabledOnly);
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project
			// types
			return true;
		}

		public String getTemplatePattern() {
			return templatePattern;
		}

		public boolean isTemplateDisabledOnly() {
			return templateDisabledOnly;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Git Auto Job Generation";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			// To persist global configuration information,
			// set that to properties and call save().
			templateDisabledOnly = formData.getBoolean("templateDisabledOnly");
			templatePattern = formData.getString("templatePattern");
			log.info("Saved pattern as " + templatePattern);
			// ^Can also use req.bindJSON(this, formData);
			// (easier when there are many fields; need set* methods for this,
			// like setUseFrench)
			save();
			return super.configure(req, formData);
		}

	}
}
