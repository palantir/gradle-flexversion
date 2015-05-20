package com.palantir.gradle.versions.flexversioning

import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;

class PrintVersionTask extends DefaultTask {

    public PrintVersionTask() {
        setDescription("Print the version of the project.")
        setGroup(FlexVersionPlugin.GROUP);
    }

    @TaskAction
    public void printVersion() {
        getProject().getLogger().log(LogLevel.QUIET, getProject().getVersion().toString())
    }
}
