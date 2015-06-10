package com.palantir.gradle.versions.flexversioning

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

class FlexVersionTests {


    static File repoFolder;
    static Git git;
    static Repository repo;
    
    Project project;

    @BeforeClass
    public static void setUpRepo() {
        repoFolder = new File("${System.properties['user.dir']}/flex-testing");
        repoFolder.deleteDir();
        repoFolder.mkdirs();
        
        git = Git.init().setDirectory(repoFolder).call();
        repo = new FileRepositoryBuilder().findGitDir(repoFolder).build();
        
        new File("${repoFolder}/testing").append("Some content");
        git.add().addFilepattern("testing").call();
        git.commit().setMessage("WOW").call();
        
        RevCommit head = new RevWalk(repo).parseCommit(repo.resolve(Constants.HEAD));
        println "HEAD " + head.name();
        
        git.tag().setAnnotated(true).setName("0.642.0").setMessage("This is 0.642.0 wow!").setObjectId(null).call();
        
        
    }
    
    @Before
    public void setUpProject() {
        project = ProjectBuilder.builder().withProjectDir(repoFolder).withName("flex-test").build();
        new FlexVersionPlugin().apply(project);
    }
    
    @Test
    public void one () {
        println project.flexVersion()
    }
    
    @Test
    public void two () {
        project.flexversion.useTags = true
        println project.flexVersion()
    }
    
    @Test
    public void three() {
        project.flexversion.envvarSources << "buildRef"
        println project.flexVersion()
    }
}
