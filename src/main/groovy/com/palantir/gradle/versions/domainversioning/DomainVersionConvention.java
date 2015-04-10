package com.palantir.gradle.versions.domainversioning;

import org.gradle.api.Project;

public class DomainVersionConvention {

	private Project project;
	
	public DomainVersionConvention(Project project) {
		this.project = project;
	}
	
	public String domainVersion() {
		return DomainVersionPlugin.buildDomainVersion(project, null);
	}
	
	public String domainVersion(String userDomain) {
		return DomainVersionPlugin.buildDomainVersion(project, userDomain);
	}
}
