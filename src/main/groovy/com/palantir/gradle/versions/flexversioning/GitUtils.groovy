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
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project

class GitUtils {

    /*
     * Given a Gradle project, return its Git Repository
     */
    static Repository getRepoFromProject(Project project) {
        Repository repo;
        try {
            /* Check if it found a git dir and set anything if not because
             * build() thinks we didn't try to set one otherwise.
             * 'mustExist' boolean only works if gitDir is set explicitly
             */

            FileRepositoryBuilder repoBuilder = new FileRepositoryBuilder();
            repoBuilder.setMustExist(true).findGitDir(project.projectDir);
            if (repoBuilder.getGitDir() == null) {
                repoBuilder.setGitDir(project.projectDir);
            }
            repo = repoBuilder.build();
        } catch (RepositoryNotFoundException rnfe) {
            throw new GradleException("Flex Version plugin needs a project in a git repo to work.")
        }

        return repo;
    }


    /*
     * Get the current HEAD commit of the repo
     */
    static RevCommit getCommitHead(Repository repo) {
        AnyObjectId headId = repo.resolve(Constants.HEAD);
        return (new RevWalk(repo)).parseCommit(headId);
    }


    /*
     * Count the number of commits in the current HEAD commit's history
     */
    static int countCommitHistory(Repository repo, RevCommit commit) {
        // We use git log because trying the RevWalk resulting in edge case off by one errors
        Iterable<RevCommit> commits = Git.wrap(repo).log().call()
        int count = 0;
        commits.each {
            count ++;
        }
        return count;
    }


    /*
     * Find the current tag on a given target.
     *
     * If the commit has multiple tags, the tag chosen will be the
     * one that a 'git describe' would return.  This means the tag
     * needs to be annotated.  If no annotated tags are found, then
     * null is returned.
     */
    static String getTagOnCommit(Repository repo, String target) {
        String tag = null;
        String gitDescribe = Git.wrap(repo).describe().setTarget(target).call();
        repo.tags.each { k, v ->
            String refname = v.getName();
            if (refname.startsWith("refs/tags/")) {
                refname = refname.substring("refs/tags/".length());
            }
            if (gitDescribe.equals(refname)) {
                tag = refname;
            }
        }
        return tag;
    }


    /*
     * Return the dirty bit of the repo.
     */
    static boolean isDirtyRepo(Repository repo) {
        return !Git.wrap(repo).status().call().isClean();
    }
}
