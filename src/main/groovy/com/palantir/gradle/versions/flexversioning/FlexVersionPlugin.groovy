// Copyright 2015 Palantir Technologies
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.palantir.gradle.versions.flexversioning

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

import com.palantir.gradle.versions.flexversioning.FlexVersionExtension;

class FlexVersionPlugin implements Plugin<Project> {

    public static final String GROUP = "Flex Versioning";
    private static final String DOMAIN_OVERRIDE_PROPERTY = "FLEX_VERSION_DOMAIN_OVERRIDE";
    private static final String DOMAIN_TAG_PROPERTY = "FLEX_VERSION_USE_TAG";

    @Override
    public void apply(Project project) {
        FlexVersionExtension extension = new FlexVersionExtension();
        FlexVersionConvention convention = new FlexVersionConvention(project, extension);
        project.getConvention().getPlugins().put("flexversion", convention);
        project.getExtensions().add("flexversion", extension);
    }

    public static String buildFlexVersion(Project project, String userDomain, FlexVersionExtension flexExtension) {
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


        // Check passed in environment variable list
        String envvarDomain = null;
        for (ev in flexExtension.envvarSources) {
            if (System.env[ev] != null) {
                envvarDomain = System.env[ev];
                for (s in flexExtension.stripRefs) {
                    if (envvarDomain.startsWith(s)) {
                        envvarDomain = envvarDomain.substring(s.length());
                    }
                }
                break;
            }
        }


        // Dirty bit
        String dirty = Git.wrap(repo).status().call().isClean() ? "" : "-dirty";


        /*
         * Choose a domain in decreasing priority:
         * 1. Set by environment variable
         * 2. Set by tag iff an environment variable is set and the tag is on the HEAD commit
         * 3. Environment variable from a user provided list
         * 4. Passed in by the user
         * 5. Symbolic ref of HEAD
         * 6. unspecified
         */
        String domain = "unspecified";
        if (System.env[DOMAIN_OVERRIDE_PROPERTY] != null) {
            domain = System.env[DOMAIN_OVERRIDE_PROPERTY];
        } else if (System.env[DOMAIN_TAG_PROPERTY] != null && domainIfTag != null) {
            domain = domainIfTag;
            return "${domain}${dirty}";
        } else if (envvarDomain != null) {
            domain = envvarDomain;
        } else if (userDomain != null && !userDomain.trim().isEmpty()) {
            domain = userDomain.trim();
        } else if (headRef.isSymbolic()){
            String targetName = headRef.getTarget().getName();
            if (targetName.startsWith("refs/heads/")) {
                targetName = targetName.substring("refs/heads/".length());
            }
            domain = targetName.replaceAll("/", "-");
        }

        return "${domain}-${commitCount}-g${headSha1.substring(0,12)}${dirty}";
    }

    private static Repository getRepo(Project project) {
        File repoLocation = Paths.get(project.projectDir.toString(), ".git").toFile();
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
