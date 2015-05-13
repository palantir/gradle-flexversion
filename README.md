# Flex Versioning plugin for gradle [![Build Status](https://travis-ci.org/palantir/gradle-flexversion.svg?branch=0.1.0)](https://travis-ci.org/palantir/gradle-flexversion)

## Why?
We created Flex Versioning for two reasons. One reason is that at Palantir, the same commit is often built and/or released in different contexts, in different "domains". As we create more of these domains (such as customer branches or patch branches) it becomes increasingly difficult to understand what artifacts belong to what domains and what commits they came from. No existing versioning scheme in wide use matches this requirement, so we have to "roll our own".

The second reason is that most other versioning schemes out there are not flexible enough, and are too specific about implementation. For example, most insist upon using tags, which we have found to be a disaster when you do it wrong by making your mistakes immutable and difficult to mitigate, and an equally big disaster when you do it right because of a shared, global namespace that is not very secure or future proof. Additionally, we have projects from "uber lightweight" all the way up to "dedicated release manager" heavy-weight with hundreds of branches. Flex Versions is a scheme which is consistent enough across these projects without putting undue burden on them.

## How does it work?
Any git commit is part of a set of 'domains'.  The version of this commit then becomes `$DOMAIN-$N-g$X`.  `$DOMAIN` is one of the domains it belongs to. `$N` is the number of commits in the commit's entire history. `$X` is the 12-character prefix of the SHA1 of the commit.  This has the same format as the `git describe` command so the domain version can be used in any git command and git will do "do the right thing".

Tags are unnecessary here.  However, if a "clean version" is wanted, Flex Versioning will use the tag if the commit has a tag on it and the environment variable `FLEX_VERSION_USE_TAG` is set.  If there are multiple tags on one commit, it will use the tag `git describe` would have picked.


## Using it in Gradle

	buildscript {
		repositories {
			maven {
				url "http://dl.bintray.com/palantir/releases"
			}
			mavenCentral()
		}
		dependencies {
			classpath 'com.palantir:gradle-flexversion:0.2.0'
		}
	}

	apply plugin: 'gradle-flexversion'
	version flexVersion()

## How is the domain picked?

The plugin will pick a domain in the following order:

1.  Set by environment variable `FLEX_VERSION_DOMAIN_OVERRIDE` (i.e. `FLEX_VERSION_DOMAIN_OVERRIDE=foo ./gradlew build`)
2.  Set by a tag if and only if environment variable `FLEX_VERSION_USE_TAG` is set and the commit has a tag. (No commit #s or hash are appended in this case)
3.  Environment variable from a user provided list in the `flexversion` extension's property `envvarSources`.
3.  Passed in by the user as a parameter to `flexVersion()`.  **This method is very discouraged because it will cause merge conflicts and isn't deterministic**
4.  Read the symbolic ref of HEAD (This is great because it will basically use the local branch name)
5.  The value `unspecified`

## Using it in Build Infrastructure

If your build infrastructure has a HEAD value set, then Flex Version will just work.  Flex Version uses the equivalent of `git symbolic-ref --short HEAD`.  In case the build infrastrcture works with detatched head and/or using environment variables with the branch name, then use the extension.

In this example, we assume we have two build infrastructures.  The first is using some kind of Gerrit plugin that sets the branch in `GERRIT_BRANCH` with the form `gerrit/BRANCHNAME`.  The second is Bamboo and we assume it sets the environment variable `BAMBOO_REPO_BRANCH`.


	flexversion {
		envvarSources << "GERRIT_BRANCH" << "BAMBOO_REPO_BRANCH"
		stripRefs << "gerrit/"
	}

Flex Version will first check for `FLEX_VERSION_DOMAIN_OVERRIDE`, the tag condition, and then the environment variables `GERRIT_BRANCH` and `BAMBOO_REPO_BRANCH` for a branch value.  If it finds it in one of the environment variables we passed in, it will strip off the `gerrit/` at the beginning.  The property `stripRefs` has a default value of `["refs/tags/", refs/heads/", "origin/"]`.  The order values to `envvarSources` is also the order of priority (decreasing).  It will pick the first one it finds.

## Enforcing certain domains

While Flex Versioning is all about allowing almost any domain, it still provides a way to enforce a pattern.  There is an extra property in the `flexversion` closure that will take a Java/Groovy Pattern and enforce that the domain matches it.  For example, semver.org Version 2 can be matched with `~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/`.

	flexversion {
    domainPattern = ~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/
	}

Before returning the version string, if the found domain doesn't match that lovely pattern, it will fail the build.

# LICENSE

Gradle Flex Version is released by Palantir Technologies, Inc. under the Apache 2.0 License. see the included LICENSE file for details.
