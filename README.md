# Flex Versioning plugin for gradle [![Build Status](https://travis-ci.org/palantir/gradle-flexversion.svg?branch=master)](https://travis-ci.org/palantir/gradle-flexversion)

## Why?
We created Flex Versioning for two reasons. One reason is that at Palantir, the same commit is often built and/or released in different contexts, in different "domains". As we create more of these domains (such as customer branches or patch branches) it becomes increasingly difficult to understand what artifacts belong to what domains and what commits they came from. No existing versioning scheme in wide use matches this requirement, so we have to "roll our own".

The second reason is that most other versioning schemes out there are not flexible enough, and are too specific about implementation. For example, most insist upon using tags, which we have found to be a disaster when you do it wrong by making your mistakes immutable and difficult to mitigate, and an equally big disaster when you do it right because of a shared, global namespace that is not very secure or future proof. Additionally, we have projects from "uber lightweight" all the way up to "dedicated release manager" heavy-weight with hundreds of branches. Flex Versions is a scheme which is consistent enough across these projects without putting undue burden on them.

## How does it work?
Any git commit is part of a set of 'domains'.  The version of this commit then becomes `$DOMAIN-$N-g$X`.  `$DOMAIN` is one of the domains it belongs to. `$N` is the number of commits in the commit's entire history. `$X` is the 12-character prefix of the SHA1 of the commit.  This has the same format as the `git describe` command so the domain version can be used in any git command and git will do "do the right thing".

Tags are unnecessary here.  However, if a "clean version" is wanted, Flex Versioning will use the tag if the commit has a tag on it and the environment variable `FLEX_VERSION_USE_TAG` is set or property `useTags` is `true`.  If there are multiple tags on one commit, it will use the tag `git describe` would have picked.


## Adding it to Gradle

	buildscript {
		repositories {
			maven {
				url "http://dl.bintray.com/palantir/releases"
			}
			mavenCentral()
		}
		dependencies {
			classpath 'com.palantir:gradle-flexversion:0.3.0'
		}
	}

	apply plugin: 'gradle-flexversion'
	version flexVersion()

## Using it

### Let me just use this locally to build and make tags clean

By default, Flex Version will use the name of the current branch.  If there is no branch, the value `unspecified` will be used.  In this example, any commit without a tag will have a version like `BRANCHNAME-$N-g$X` and if a commit has a tag, it will just be `TAGNAME`.

	apply plugin: 'gradle-flexversion'
		flexversion {
		useTags = true
	}
	version flexVersion()

### Let me use this in my Build Infrastructure w/o a dedicated release build

In this example, Flex Version is being used in a build infrastructure with one build.  It is similar to the above example.  However, in this case, our infrastructure may build things in a headless state and it provides the branch name in an environment variable called `GIT_BRANCH`.

	apply plugin: 'gradle-flexversion'
	flexversion {
		useTags = true
		envvarSources << "GIT_BRANCH"
	}
	version flexVersion()

### OK cool, but I want to have a dedicated release build!

That's no problem.  Use the environment variable for tags instead of the property.

	apply plugin: 'gradle-flexversion'
	flexversion {
		envvarSources << "GIT_BRANCH"
	}
	version flexVersion()

Then, in the build you want to handle building releases (or clean versions), run your gradle command with the environment variable set.  For example, you could run `$ FLEX_VERSION_USE_TAG=set ./gradlew publish`.  This would keep the standard build publishing the `DOMAIN-$N-g$H` pattern and the release publishing the clean tag format.

### I want my domain to be a number without changing my branch name

You can do that too.  Just pass in a value.  In this example, we have a `version.txt` file that only contains the domain we want.

	apply plugin: 'gradle-flexversion'
	version flexVersion(file("version.txt").text.trim())

The thing to be aware of here is that after a git merge, check the value of the file to make sure it's what you want it to be.

## Advanced

### Enforcing domains to have a format

While Flex Versioning is all about allowing almost any domain, it still provides a way to enforce a pattern.  There is an extra property in the `flexversion` closure that will take a Java/Groovy Pattern and enforce that the domain matches it.  For example, semver.org Version 2 can be matched with `~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/`.

	flexversion {
		domainPattern = ~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/
	}

Before returning the version string, if the found domain doesn't match that lovely pattern, it will fail the build.

### How is the domain picked?

The plugin will pick a domain in the following order:

1.  Set by environment variable `FLEX_VERSION_DOMAIN_OVERRIDE` (i.e. `FLEX_VERSION_DOMAIN_OVERRIDE=foo ./gradlew build`)
2.  Set by a tag if and only if environment variable `FLEX_VERSION_USE_TAG` is set or the property `useTags` is `true` and the commit has a tag. (No commit #s or hash are appended in this case)
3.  Environment variable from a user provided list in the `flexversion` extension's property `envvarSources`.
3.  Passed in by the user as a parameter to `flexVersion()`.
4.  Read the symbolic ref of HEAD (This is great because it will basically use the local branch name)
5.  The value `unspecified`

### My domain has origin- or some other extra prefix on it

If you're pulling in the branch name via environment variable, the build infrastructure may be adding something like `origin/` or `refs/tags/` on it.  To strip off any prefixes, make use of the `stripRefs` property.  By default, it's value is `["refs/tags/", "refs/heads/", "origin/"]`.

	flexversion {
		stripRefs << "gerrit/"
	}

The property `stripRefs` is used to strip off any prefix from a domain that was determined via the `envvarSources` property.

### All the properties

*  `envvarSources` - A List of environment variables to check for domain values (uses the first one it finds).  Default is `[]`.
*  `stripRefs` - A list of prefixes to remove from the domain found via `envvarSources`.  It will strip all the prefixes it finds.  Default is `["refs/tags/", "refs/heads/", "origin/"]`.
*  `domainPattern` - Before returning the version, check that the domain matches this Pattern.  Default is none.
*  `useTags` - If true and the commit being built has a tag on it, the version returned will be the tag's value.  Default is `false`.

# LICENSE

Gradle Flex Version is released by Palantir Technologies, Inc. under the Apache 2.0 License. see the included LICENSE file for details.
