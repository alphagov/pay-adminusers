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
        sh 'docker pull govukpay/postgres:9.4.4'
        sh 'mvn clean package'
      }
    }
    stage('Docker Build') {
      steps {
        script {
          buildApp{
            app = "adminusers"
          }
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
          dockerTag {
            app = "adminusers"
          }
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        deploy("adminusers", "test", null, true, false)
        deployEcs("adminusers", "test", null, true, true)
      }
    }
  }
}
