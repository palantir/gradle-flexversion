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

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException

import java.nio.file.Paths

public class GitUtils {

    /**
     * Returns the tag of the HEAD commit or null if no such tag exists.
     * TODO(rfink): What if there are multiple tags on HEAD?
     */
    static String getHeadTag(Repository repo) {
        String headTag = null;
        String gitDescribe = Git.wrap(repo).describe().setTarget("HEAD").call();
        repo.tags.each {k, v ->
            String refname = v.getName();
            if (refname.startsWith("refs/tags/")) {
                refname = refname.substring("refs/tags/".length());
            }
            if (gitDescribe.equals(refname)) {
                headTag = refname;
            }
        }
        headTag
    }

    /**
     * Returns a Git {@link Repository} rooted in the given {@code directory}.
     */
    static Repository getGitRepository(File directory) {
        try {
            File repoLocation = Paths.get(directory.toString(), ".git").toFile()
            return new FileRepositoryBuilder()
                    .readEnvironment()
                    .findGitDir(repoLocation)
                    .build()
        } catch (IOException | IllegalArgumentException e) {
            throw new GradleException("Failed to open Git repository in {${directory}", e)
        }
    }

    /**
     * Returns the HEAD commit of the given {@link Repository}.
     */
    static RevCommit getHeadCommit(Repository repo) {
        AnyObjectId headId = repo.resolve(Constants.HEAD)
        return new RevWalk(repo).parseCommit(headId)
    }

    /**
     * Returns the first commit in the given repository.
     */
    static RevCommit getFirstCommit(Repository repo, RevCommit headCommit) {
        RevWalk walk = new RevWalk(repo)
        walk.sort(RevSort.REVERSE)
        walk.markStart(headCommit)
        return walk.next()
    }
}

