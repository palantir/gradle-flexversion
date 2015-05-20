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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.gradle.api.GradleException
import org.gradle.api.Project

public class FlexVersionConvention {

    private static final String DOMAIN_OVERRIDE_PROPERTY = "FLEX_VERSION_DOMAIN_OVERRIDE"
    private static final String DOMAIN_TAG_PROPERTY = "FLEX_VERSION_USE_TAG"

    private Project project

    public FlexVersionConvention(Project project) {
        this.project = project
    }

    public FlexVersion flexVersion() {
        return buildFlexVersion(project, null)
    }

    public FlexVersion flexVersion(String userDomain) {
        return buildFlexVersion(project, userDomain)
    }

    private static FlexVersion buildFlexVersion(Project project, String userDomain) {
        FlexVersionExtension flexExtension = project.extensions.findByType(FlexVersionExtension)
        Repository repo = GitUtils.getGitRepository(project.projectDir)

        // Find the HEAD commit and its tag (if tagged).
        RevCommit headCommit = GitUtils.getHeadCommit(repo)
        String headTag = GitUtils.getHeadTag(repo)

        // Count commits
        RevCommit firstCommit = GitUtils.getFirstCommit(repo, headCommit)
        int commitCount = RevWalkUtils.count(new RevWalk(repo), headCommit, firstCommit)

        // Collect potential domain name from environment variables.
        String envVarDomain = getDomainFromEnvironment(flexExtension)

        /*
         * Choose a domain in decreasing priority:
         * 1. Set by environment variable
         * 2. Set by tag iff an environment variable is set and the tag is on the HEAD commit
         * 3. Environment variable from a user provided list
         * 4. Passed in by the user
         * 5. Symbolic ref of HEAD
         * 6. "unspecified"
         */
        String domain
        boolean domainOnlyVersion = false
        if (System.env[DOMAIN_OVERRIDE_PROPERTY] != null) {
            domain = System.env[DOMAIN_OVERRIDE_PROPERTY]
        } else if ((System.env[DOMAIN_TAG_PROPERTY] != null || flexExtension.useTags) && headTag != null) {
            domain = headTag
            domainOnlyVersion = true
        } else if (envVarDomain != null) {
            domain = envVarDomain
        } else if (userDomain != null && !userDomain.trim().isEmpty()) {
            domain = userDomain.trim()
        } else if (repo.getRef(Constants.HEAD).isSymbolic()) {
            domain = repo.getRef(Constants.HEAD).getTarget().getName()
            if (domain.startsWith("refs/heads/")) {
                domain = domain.substring("refs/heads/".length())
            }
        } else {
            domain = "unspecified"
        }
        domain = domain.replaceAll("/", "-")

        // Check if the domain matches the required pattern
        if (flexExtension?.domainPattern) {
            if (!flexExtension.domainPattern ==~ domain) {
                throw new GradleException("Domain [${domain}] does not match the required pattern ${flexExtension.domainPattern}.")
            }
        }

        boolean isDirty = !Git.wrap(repo).status().call().isClean()
        return new FlexVersion(domain, commitCount, headCommit.name(), domainOnlyVersion, isDirty)
    }

    private static String getDomainFromEnvironment(FlexVersionExtension flexExtension) {
        String envvarDomain = null
        for (ev in flexExtension.envvarSources) {
            if (System.env[ev] != null) {
                envvarDomain = System.env[ev]
                for (s in flexExtension.stripRefs) {
                    if (envvarDomain.startsWith(s)) {
                        envvarDomain = envvarDomain.substring(s.length())
                    }
                }
                break
            }
        }
        envvarDomain
    }
}
