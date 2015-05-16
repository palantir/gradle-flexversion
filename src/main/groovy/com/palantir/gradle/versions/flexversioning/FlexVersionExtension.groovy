package com.palantir.gradle.versions.flexversioning

import java.util.regex.Pattern

class FlexVersionExtension {
    List envvarSources = [];
    List stripRefs = ["refs/tags/", "refs/heads/", "origin/"];
    Pattern domainPattern = null;
    boolean useTags = false;
}
