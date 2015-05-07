# Flex Versioning plugin for gradle

## Why?
We created Flex Versioning for two reasons. One reason is that at Palantir, the same commit is often built and/or released in different contexts, in different "domains". As we create more of these domains (such as customer branches or patch branches) it becomes increasingly difficult to understand what artifacts belong to what domains and what commits they came from. No existing versioning scheme in wide use matches this requirement, so we have to "roll our own".

The second reason is that most other versioning schemes out there are not flexible enough, and are too specific about implementation. For example, most insist upon using tags, which we have found to be a disaster when you do it wrong by making your mistakes immutable and difficult to mitigate, and an equally big disaster when you do it right because of a shared, global namespace that is not very secure or future proof. Additionally, we have projects from "uber lightweight" all the way up to "dedicated release manager" heavy-weight with hundreds of branches. We need a scheme which is consistent enough across these projects without putting undue burden on them, and that is what we think Flex Versions will accomplish.

## How does it work?
Any git commit is part of a set of 'domains'.  The version of this commit then becomes `$DOMAIN-$N-g$X`.  `$DOMAIN` is one of the domains it belongs to. `$N` is the number of commits in the commit's entire history. `$X` is the 12-character prefix of the SHA1 of the commit.  This has the same format as the `git describe` command so the domain version can be used in any git command and git will do "do the right thing".

Tags are unnecessary here.  However, if a "clean version" is wanted, Flex Versioning will use the tag if the commit has a tag on it and the environment variable `FLEX_VERSION_USE_TAG` is set.  If there are multiple tags on one commit, it will use the tag `git describe` would have picked.


## Using it in Gradle

	buildscript {
		repositories {
	 		// Bintray
	 	}
	}
	dependencies {
		classpath 'com.palantir:gradle-flexversion:0.1.0'
	}
	
	apply plugin: 'gradle-flexversion'
	version flexVersion()
	
### How is the domain picked?

The plugin will pick a domain in the following order:

1.  Set by environment variable `FLEX_VERSION_DOMAIN_OVERRIDE` (i.e. `FLEX_VERSION_DOMAIN_OVERRIDE=foo ./gradlew build`)
2.  Set by a tag if and only if environment variable `FLEX_VERSION_USE_TAG` is set and the commit has a tag. (No commit #s or hash are appended in this case)
3.  Passed in by the user as a parameter to `flexVersion()`.  **This method is very discouraged because it will cause merge conflicts and isn't deterministic**
4.  Read the symbolic ref of HEAD (This is great because it will basically use the local branch name)
5.  The value `unspecified`

# LICENSE

Gradle Flex Version is released by Palantir Technologies, Inc. under the Apache 2.0 License. see the included LICENSE file for details.
