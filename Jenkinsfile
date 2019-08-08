#!/usr/bin/env groovy

pipeline {
  agent any

  parameters {
    booleanParam(defaultValue: false, description: '', name: 'runEndToEndTestsOnPR')
  }

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
    RUN_END_TO_END_ON_PR = "${params.runEndToEndTestsOnPR}"
    JAVA_HOME="/usr/lib/jvm/java-1.11.0-openjdk-amd64"
  }

  stages {
    stage('Maven Build') {
      steps {
        script {
          long stepBuildTime = System.currentTimeMillis()
          sh 'mvn -version'
          sh 'mvn clean verify'
          runProviderContractTests()
          postSuccessfulMetrics("adminusers.maven-build", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("adminusers.maven-build.failure", 1)
        }
      }
    }

    stage('Docker Build') {
      steps {
        script {
          buildAppWithMetrics{
            app = "adminusers"
          }
        }
      }
      post {
        failure {
          postMetric("adminusers.docker-build.failure", 1)
        }
      }
    }
    stage('Tests') {
      failFast true
      parallel {
        stage('End-to-End Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_END_TO_END_ON_PR', value: 'true'
                }
            }
            steps {
                runAppE2E("adminusers", "card")
            }
        }
      }
    }
    stage('Docker Tag') {
      steps {
        script {
          dockerTagWithMetrics {
            app = "adminusers"
          }
        }
      }
      post {
        failure {
          postMetric("adminusers.docker-tag.failure", 1)
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        checkPactCompatibility("adminusers", gitCommit(), "test")
        deployEcs("adminusers")
      }
    }
    stage('Pact Tag') {
      when {
        branch 'master'
      }
      steps {
        echo 'Tagging provider pact with "test"'
        tagPact("adminusers", gitCommit(), "test")
      }
    }
    stage('Smoke Tests') {
      when {
        branch 'master'
      }
      steps {
        runDirectDebitSmokeTest()
      }
    }
    stage('Complete') {
      failFast true
      parallel {
        stage('Tag Build') {
          when {
            branch 'master'
          }
          steps {
            tagDeployment("adminusers")
          }
        }
        stage('Trigger Deploy Notification') {
          when {
            branch 'master'
          }
          steps {
            triggerGraphiteDeployEvent("adminusers")
          }
        }
      }
    }
  }
  post {
    failure {
      postMetric(appendBranchSuffix("adminusers") + ".failure", 1)
    }
    success {
      postSuccessfulMetrics(appendBranchSuffix("adminusers"))
    }
  }
}
