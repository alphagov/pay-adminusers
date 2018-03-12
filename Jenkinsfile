#!/usr/bin/env groovy

pipeline {
  agent any

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
  }

  stages {
    stage('Maven Build') {
      steps {
        script {
          def long stepBuildTime = System.currentTimeMillis()

          sh 'docker pull govukpay/postgres:9.4.4'
          sh 'mvn clean package'
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
    stage('Test') {
      steps {
        runEndToEnd("adminusers")
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
        deployEcs("adminusers", "test", null, true, true)
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
