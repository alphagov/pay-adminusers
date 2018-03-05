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
    HOSTED_GRAPHITE_ACCOUNT_ID = credentials('graphite_account_id')
    HOSTED_GRAPHITE_API_KEY = credentials('graphite_api_key')
  }

  stages {
    stage('Maven Build') {
      steps {
        sh 'docker pull govukpay/postgres:9.4.4'
        sh 'mvn clean package'
      }
      post {
        failure {
          postMetric("adminusers.maven-build.failure", 1, "new")
        }
        success {
          postSuccessfulMetrics("adminusers.maven-build")
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
          postMetric("adminusers.docker-build.failure", 1, "new")
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
          postMetric("adminusers.docker-tag.failure", 1, "new")
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
      postMetric("adminusers.failure", 1, "new")
    }
    success {
      postSuccessfulMetrics("adminusers")
    }
  }
}
