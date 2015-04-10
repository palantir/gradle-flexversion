package com.palantir.gradle.versions.domainversioning

import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.RefDirectory;
import org.eclipse.jgit.internal.storage.file.RefDirectory.LooseUnpeeled;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SymbolicRef
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Plugin
import org.gradle.api.Project

class DomainVersionPlugin implements Plugin<Project> {

	public static final String GROUP = "Domain Versioning";
	private static final String DOMAIN_OVERRIDE_PROPERTY = "DOMAIN_VERSION_DOMAIN_OVERRIDE";
	private static final String DOMAIN_TAG_PROPERTY = "DOMAIN_VERSION_USE_TAG";
	private static int hashLength = 12;
	
	@Override
	public void apply(Project project) {
		DomainVersionConvention convention = new DomainVersionConvention(project);
		project.getConvention().getPlugins().put("domainversion", convention);
	}

	public static String buildDomainVersion(Project project, String userDomain) {
		Repository repo = getRepo(project);
		
		RevWalk walk = new RevWalk(repo);

		// Find the HEAD commit and ref
		AnyObjectId headId = repo.resolve(Constants.HEAD);
		RevCommit headCommit = walk.parseCommit(headId);
		Ref headRef = repo.getRef(Constants.HEAD);
		String headSha1 = headCommit.name();
		
		
		// Find the first commit
		walk.sort(RevSort.REVERSE);
		walk.markStart(headCommit);
		RevCommit firstCommit = walk.next();
		
		
		// Count commits
		String commitCount = RevWalkUtils.count(walk, headCommit, firstCommit).toString();
		

		// Find if there is a tag on the HEAD commit.		
		String domainIfTag = null;
		String gitDescribe = Git.wrap(repo).describe().setTarget("HEAD").call();
		repo.tags.each { k, v ->
			String refname = v.getName();
			if (refname.startsWith("refs/tags/")) {
				refname = refname.substring("refs/tags/".length());
			}
			if (gitDescribe.equals(refname)) {
				domainIfTag = refname;
			}
		}
		
		
		/*
		 * Choose a domain in decreasing priority:
		 * 1. Set by environment variable
		 * 2. Set by tag iff an environment variable is set and the tag is on the HEAD commit
		 * 3. Passed in by the user
		 * 4. Symbolic ref of HEAD
		 * 5. unspecified
		 */
		String domain = "unspecified";
		if (System.env[DOMAIN_OVERRIDE_PROPERTY] != null) {
			domain = System.env[DOMAIN_OVERRIDE_PROPERTY];
		} else if (System.env[DOMAIN_TAG_PROPERTY] != null && domainIfTag != null) {
			domain = domainIfTag;
		} else if (userDomain != null && !userDomain.trim().isEmpty()) {
			domain = userDomain;
			return domain;
		} else if (headRef.isSymbolic()){
			String targetName = headRef.getTarget().getName();
			if (targetName.startsWith("refs/heads/")) {
				targetName = targetName.substring("refs/heads/".length());
			}
			domain = targetName.replaceAll("/", "-");
		}
		
		return "${domain}-${commitCount}-g${headSha1.substring(0,hashLength)}";
	}
	
	private static Repository getRepo(Project project) {
		File repoLocation = Paths.get(project.projectDir, ".git").toFile();
		Repository repo;
		try {
			repo = new FileRepositoryBuilder().readEnvironment()
					.findGitDir(repoLocation).build();			
		} catch (IllegalArgumentException iae) {
			//TODO: Throw exception from this plugin
			throw iae;
		}
		
		return repo;
	}

}
