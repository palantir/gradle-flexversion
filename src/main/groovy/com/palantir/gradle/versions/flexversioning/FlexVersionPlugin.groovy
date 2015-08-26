// Copyright 2015 Palantir Technologies
//
// Licensed under the Apache License, Version 2.0 (the "License")
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

import java.util.regex.Pattern

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.gradle.api.Plugin
import org.gradle.api.Project

class FlexVersionPlugin implements Plugin<Project> {

    private static final String DOMAIN_OVERRIDE_PROPERTY = "FLEX_VERSION_DOMAIN_OVERRIDE";
    private static final String DOMAIN_TAG_PROPERTY = "FLEX_VERSION_USE_TAG";

    public static final String GROUP = "Flex Versioning";

    @Override
    public void apply(Project project) {
        FlexVersionExtension extension = new FlexVersionExtension();
        FlexVersionConvention convention = new FlexVersionConvention(project, extension);
        project.getConvention().getPlugins().put("flexversion", convention);
        project.getExtensions().add("flexversion", extension);
    }

    static FlexVersion buildFlexVersion(Project project, String userDomain, FlexVersionExtension flexExtension) {
        Repository repo = GitUtils.getRepoFromProject(project);

        RevWalk walk = new RevWalk(repo);


        // Find the HEAD commit and ref
        RevCommit headCommit = GitUtils.getCommitHead(repo);
        Ref headRef = repo.getRef(Constants.HEAD);


        // Count commits
        int commitCount = GitUtils.countCommitHistory(repo, headCommit);


        // Find the tag on the HEAD commit
        String domainIfTag = GitUtils.getTagOnCommit(repo, Constants.HEAD);


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
        boolean tag = false;
        if (System.env[DOMAIN_OVERRIDE_PROPERTY] != null) {
            domain = System.env[DOMAIN_OVERRIDE_PROPERTY];
        } else if ((System.env[DOMAIN_TAG_PROPERTY] != null || flexExtension.useTags) && domainIfTag != null) {
            domain = domainIfTag;
            tag = true;
        } else if (userDomain != null && !userDomain.trim().isEmpty()) {
            domain = userDomain.trim();
        } else if (envvarDomain != null) {
            domain = envvarDomain;
        } else if (headRef.isSymbolic()){
            domain = headRef.getTarget().getName();
            if (domain.startsWith("refs/heads/")) {
                domain = domain.substring("refs/heads/".length());
            }
        }
        domain = domain.replaceAll("/", "-");


        // Check if the domain matches the required pattern
        if (flexExtension.domainPattern != null) {
            if (!(flexExtension.domainPattern.matcher(domain).matches())) {
                throw new FlexVersionPatternException(domain, flexExtension.domainPattern);
            }
        }

        return new FlexVersion(domain, commitCount, headCommit.name(), tag, GitUtils.isDirtyRepo(repo));
    }

    static class FlexVersionPatternException extends Exception {
        public FlexVersionPatternException(String domain, Pattern pattern) {
            super("Domain [${domain}] does not match the required pattern ${pattern}.")
        }
    }
}
