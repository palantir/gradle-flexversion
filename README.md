# Domain Versioning plugin for gradle

## Why?
 

## How does it work?
Any git commit is part of a set of 'domains'.  The version of this commit then becomes `$DOMAIN-$N-g$X`.
`$DOMAIN` is one of the domains it belongs to. `$N` is the number of commits in the commit's entire history. `$X` is the
12-character prefix of the SHA1 of the commit.  This has the same format as the `git describe` command so the domain
version can be used in any git command and git will do "do the right thing".

Tags are unnecessary here.  However, if a "clean version" is wanted, Domain Versioning will use the tag if the commit
has a tag on it and the environment variable `DOMAIN_VERSION_USE_TAG` is set.  If there are multiple tags on one commit,
it will use the tag `git describe` would have picked.


## Using it in Gradle

	buildscript {
		repositories {
	 		// Bintray
	 	}
	}
	dependencies {
		classpath 'com.palantir:gradle-domainversion:0.1.0'
	}
	
	apply plugin: 'gradle-gitdomainversion'
	version domainVersion()
	
### How is the domain picked?

The plugin will pick a domain in the following order:
1. Set by environment variable `DOMAIN_VERSION_DOMAIN_OVERRIDE` (i.e. `DOMAIN_VERSION_DOMAIN_OVERRIDE=foo ./gradlew build`)
2. Set by a tag if and only if environment variable `DOMAIN_VERSION_USE_TAG` is set and the commit has a tag. (No commit #s or hash are appended in this case)
3. Passed in by the user as a parameter to `domainVersion()`.  **This method is very discouraged because it will cause merge conflicts and isn't deterministic**
4. Read the symbolic ref of HEAD (This is great because it will basically use the local branch name)
5. The value `unspecified`

