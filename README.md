# Flex Versioning plugin for gradle [![Build Status](https://travis-ci.org/palantir/gradle-flexversion.svg?branch=master)](https://travis-ci.org/palantir/gradle-flexversion) [![Download](https://api.bintray.com/packages/palantir/releases/gradle-flexversion/images/download.svg) ](https://bintray.com/palantir/releases/gradle-flexversion/_latestVersion)

## Why?
We created Flex Versioning for two reasons. One reason is that at Palantir, the same commit is often built and/or released in different contexts, in different "domains". As we create more of these domains (such as customer branches or patch branches) it becomes increasingly difficult to understand what artifacts belong to what domains and what commits they came from. No existing versioning scheme in wide use matches this requirement, so we have to "roll our own".

The second reason is that most other versioning schemes out there are not flexible enough, and are too specific about implementation. For example, most insist upon using tags, which we have found to be a disaster when you do it wrong by making your mistakes immutable and difficult to mitigate, and an equally big disaster when you do it right because of a shared, global namespace that is not very secure or future proof. Additionally, we have projects from "uber lightweight" all the way up to "dedicated release manager" heavy-weight with hundreds of branches. Flex Versions is a scheme which is consistent enough across these projects without putting undue burden on them.

## How does it work?
Any git commit is part of a set of 'domains'.  The version of this commit then becomes `$DOMAIN-$N-g$X`.  `$DOMAIN` is one of the domains it belongs to. `$N` is the number of commits in the commit's entire history. `$X` is the 12-character prefix of the SHA1 of the commit.  This has the same format as the `git describe` command so the domain version can be used in any git command and git will do "do the right thing".

Tags are unnecessary here.  However, if a "clean version" is wanted, Flex Versioning will use the tag if the commit has a tag on it and the environment variable `FLEX_VERSION_USE_TAG` is set or property `useTags` is `true`.  If there are multiple tags on one commit, it will use the tag `git describe` would have picked.  This means that the tag should be an annoted tag.


## Adding it to Gradle

```gradle
buildscript {
	repositories {
		maven {
			url "http://dl.bintray.com/palantir/releases"
		}
		mavenCentral()
	}
	dependencies {
		classpath 'com.palantir:gradle-flexversion:0.4.0'
	}
}

apply plugin: 'gradle-flexversion'
version flexVersion()
```

## Basic Usage

### Versioning using the branch name and tags for releases

The default usage of Flex Versions picks the branch name as the domain.  If the `useTags` property is set to true and the HEAD commit has a tag on it, the version will be the tag name.  If there are any `/` characters in the branch name, they are converted to `-`.

The `build.gradle` file has the following set up for versioning:

```gradle
apply plugin: 'gradle-flexversion'
flexversion {
	useTags = true
}
addPrintVersionTask()
version flexVersion()
```

With `addPrintVersionTask()`, we get a `printVersion` task the plugin adds to see what the version is.

```console
user:~/git/flexversions-example (develop) $ git log -1 --format=oneline
b7feb2b69d01d39648031a60d8bb473f094437d3 Add environment variables in the README as well
user:~/git/flexversions-example (develop) $ ./gradlew printVersion --quiet
develop-59-b7feb2b69d01
user:~/git/flexversions-example (develop) $ git tag -a 0.1.0 b7feb2b69d01 -m "Release 0.1.0" # Let's tag the current commit
user:~/git/flexversions-example (develop) $ ./gradlew printVersion --quiet
0.1.0
```

### Versioning using a user-defined domain and tags for releases

If you don't wish to use the branch name as the domain, `flexVersion()` will also accept a string for the domain.

The `build.gradle` file has the following set up for versioning:

```gradle
apply plugin: 'gradle-flexversion'
flexversion {
	useTags = true
}
addPrintVersionTask()
version flexVersion("2.3.0-dev")
```

With `addPrintVersionTask()`, we get a `printVersion` task the plugin adds to see what the version is.

```console
user:~/git/flexversions-example (develop) $ git log -1 --format=oneline
b7feb2b69d01d39648031a60d8bb473f094437d3 Add environment variables in the README as well
user:~/git/flexversions-example (develop) $ ./gradlew printVersion --quiet
2.3.0-dev-59-b7feb2b69d01
user:~/git/flexversions-example (develop) $ git tag -a 2.3.0 b7feb2b69d01 -m "Release 2.3.0" # Let's tag the current commit
user:~/git/flexversions-example (develop) $ ./gradlew printVersion --quiet
2.3.0
```

<a name="closureproperties"></a>
## Closure properties and variables

There is a `flexVersion` closure for setting up the plugin.  Below are the properties in the closure with examples of using each.

```gradle
flexversion {
	envvarSources << "GIT_BRANCH" << "GERRIT_BRANCH"
	stripRefs << "myremote/"
	domainPattern = ~/\d+\.\d+\.\d/
	useTags = true
}
```

* `envvarSources` - A list of environment variables to check for domain values (uses the first one it finds).  Defaults to `[]`.
* `stripRefs` - A list of prefixes to remove from the domain (before `/` are converted to `-`) found via `envvarSources`.  It will strip all the prefixes it finds.  Default is `["refs/tags/", "refs/heads/", "origin/"]`
* `domainPattern` - Before returning the version, check the the domain matches this Pattern.  Default is `null`.
* `useTags` - If true and the commit being build has a tag on it, the version returned will be the tag's value.  Default is `false`.

There are some environment variables as well:

* `FLEX_VERSION_DOMAIN_OVERRIDE` - This environment variable completely hijacks the domain picking logic.  If this variable is set, the domain becomes the value no matter the state of the repo or build script.
* `FLEX_VERSION_USE_TAG` - If this is set and the commit being built has a tag on it, the version returned will be the tag's value.  (This works the same as the property `useTags`)

## Advanced

### How is a domain picked?

The plugin will pick a domain in the following order:

1.  Set by environment variable `FLEX_VERSION_DOMAIN_OVERRIDE`. (example: `FLEX_VERSION_DOMAIN_OVERRIDE=foo ./gradlew publish`)
2.  Set by a tag if and only if environment variable `FLEX_VERSION_USE_TAG` is set or the property `useTags` is `true` and the commit has a tag.  **No commit counts or git hash are appended in this case**
3.  Environment variable from a user-provided list in the `flexversion` closure property `envvarSources`
4.  Passed in by the user as a parameter to `flexVersion()`
5.  Reading the symbolic ref of HEAD (This will basically use the local branch name)
6.  The value `unspecified`

After the domain is picked, all `/` characters are converted to `-`.  If the current state of the repo isn't clean, `-dirty` is appended at the end of the version.  (This is true even in the tags case).

### The type `flexVersion()` returns

The `flexVersion` method does not return a `String` object.  It returns a `FlexVersion` object.  It can be used as a parameter for Gradle's `version`.  Making this an object allows for pulling out pieces of the version string for other uses (if desired).

```groovy
class FlexVersion {
	String domain // The domain portion (or the tag value)
	String gitHash // The git hash truncated to 12 characters
	int commitCount // The number of commits in HEAD's history
	boolean dirty // Is the dirty bit set?
	boolean isTag // Is the domain from a tag?
	String toString() // The fully filled in version that Gradle would use
}
```

Even though the `toString()` of a version using tags only has the tag value, the `FlexVersion` object returned will still have the `gitHash` and `commitCount` values set.

### Use Case: Enforcing domains to have a format

While Flex Versioning is all about allowing almost any domain, it still provides a way to enforce a pattern.  There is an extra property in the `flexversion` closure that will take a Java/Groovy Pattern and enforce that the domain matches it.  For example, semver.org Version 2 can be matched with `~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/`.

```gradle
flexversion {
	domainPattern = ~/([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))\.([0-9]|(?:[1-9]\d*))(?:\-([a-zA-Z1-9][a-zA-Z0-9-]*(?:\.[a-zA-Z1-9][a-zA-Z0-9-]*)*))?/
}
```

Before returning the version string, if the found domain doesn't match the given pattern, it will fail the build.

### Use Case: Build script is running in a detached head environment

There are some build environments that may not have a local branch set.  If in these cases the value `unspecified` as the domain is not OK, the `envvarSources` property can be used if environment variables contain the required domains.  For example, we can assume that we run on our build system that triggers due to changes in a git repo.  For the sake of simplicity, the build system does a `git checkout GITHASH` leaving the local repo in a headless state.  The system provides an environment variable `GIT_BRANCH` with the value of the branch or tag.

```gradle
flexversion {
	envvarSources << "GIT_BRANCH"
}
```

If Flex Version finds that `GIT_BRANCH` is set, it will use its value as the domain.

#### Stripping remote ref information

If the environment variable provided by the build system (or other environment) contains some kind of remote name (example: `origin/`), `stripRefs` is used to remove those.

Using our example above, if `GIT_BRANCH` value is `gerrit/master`, our domain will end up being `gerrit-master`.  To fix this:

```gradle
flexversion {
	envvarSources << "GIT_BRANCH"
	stripRefs << "gerrit/"
}
```

See [closure properties](#closureproperties) above for the default value of `stripRefs`.

# LICENSE

Gradle Flex Version is released by Palantir Technologies, Inc. under the Apache 2.0 License. see the included LICENSE file for details.
