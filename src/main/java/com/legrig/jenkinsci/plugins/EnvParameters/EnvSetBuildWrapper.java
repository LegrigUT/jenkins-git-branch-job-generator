/**
 * 
 */
package com.legrig.jenkinsci.plugins.EnvParameters;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * EnvSetBuildWrapper {@link BuildWrapper}.
 * 
 * <p>
 * 
 * Build Wrapper that allows multiple jobs to share a single
 * BuildNumberGenerator
 * 
 * <p>
 * 
 * @author Michael Lasevich
 */
public class EnvSetBuildWrapper extends BuildWrapper {
	public final Log log=LogFactory.getLog(this.getClass());

	private static final String ME="EnvSetBuildWrapper: ";
	
	private List<EnvParameter> parameters;
	
	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public EnvSetBuildWrapper(List<EnvParameter> parameters) {
		this.parameters=parameters;
	}

	public List<EnvParameter> getParameters() {
		return parameters;
	}
	
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {
		final EnvVars newVars=generateNewEnvSet(build, listener);
		return new Environment() {
			@Override
			public void buildEnvVars(Map<String, String> env) {
				env.putAll(newVars);
			}
		};
	}
	
	private EnvVars generateNewEnvSet(AbstractBuild build, BuildListener listener){
		EnvVars newEnv=new EnvVars();
		try {
			EnvVars env=build.getEnvironment(listener);
			for (EnvParameter parameter:parameters){
				String param=parameter.getEnvParameter();
				if ( parameter.isNoOverwrite() && (env.containsKey(param))){
					listener.getLogger().println(ME+"Not overwriting parameter '"+param+"'");
					continue;
				}
				String value=parameter.getParameterValue();
				if (StringUtils.isEmpty(param)){
					listener.getLogger().println(ME+ " Skipping blank parameter");
				}
				if (! parameter.isLiteral()){
					//listener.getLogger().println(ME+ " resolving parameter value...");
					value=env.expand(newEnv.expand(value));
				}else{
					//listener.getLogger().println(ME+ " using literal parameter value...");
				}
				newEnv.put(param, value);
				//listener.getLogger().println(ME+"Setting fixed parameter '"+param+"' = '"+value+"'");
			}	
		} catch (IOException e) {
			listener.fatalError(ME+" Unable to get build environment.."+e.getLocalizedMessage());
		} catch (InterruptedException e) {
			listener.fatalError(ME+" Unable to get build environment.."+e.getLocalizedMessage());
		}
		return newEnv;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void preCheckout(AbstractBuild build, Launcher launcher,
			BuildListener listener) {
		
		
		return;
	}

	
	
	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl)super.getDescriptor();
	}

	
	/**
	 * Descriptor for {@link EnvSetBuildWrapper}. Used as a
	 * singleton. The class is marked as public so that it can be accessed from
	 * views.
	 * 
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends BuildWrapperDescriptor {
		public DescriptorImpl() {
			super(EnvSetBuildWrapper.class);
			load();
		}
		
		@Override
		public boolean isApplicable(AbstractProject<?, ?> job) {
			return true;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws FormException {
			return super.configure(req, json);
		}

		@Override
		public String getDisplayName() {
			return "Set Fixed Environment Variables.";
		}


	}


	public void addParameters(EnvVars newParameters) {
		if (newParameters!=null){
			Map<String, EnvParameter> currentParameters=new HashMap<String,EnvParameter>(parameters.size());
			for(EnvParameter parameter:parameters){
				currentParameters.put(parameter.getEnvParameter(), parameter);
			}
			for(String param:newParameters.keySet()){
				EnvParameter parameter=currentParameters.get(param);
				if (parameter==null){
					parameter=new EnvParameter(param, newParameters.get(param), false, false);
					parameters.add(parameter);
					currentParameters.put(param, parameter);
				}else{
					parameter.setParameterName(param);
					parameter.setParameterValue(newParameters.get(param));
				}
			}
		}
	}
}
