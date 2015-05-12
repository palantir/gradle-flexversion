package com.palantir.gradle.versions.flexversioning

class FlexVersionExtension {
    List envvarSources = [];
    List stripRefs = ["refs/tags/", "refs/heads/", "origin/"];
}
