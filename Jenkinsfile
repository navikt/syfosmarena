#!/usr/bin/env groovy

pipeline {
    agent any

     environment {
           APPLICATION_NAME = 'syfosmarena'
           DOCKER_SLUG = 'syfo'
           DISABLE_SLACK_MESSAGES = false
       }

     stages {
        stage('initialize') {
            steps {
                init action: 'gradle'
            }
        }
        stage('build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh './gradlew test'
            }
        }
        stage('create uber jar') {
            steps {
                sh './gradlew shadowJar'
                slackStatus status: 'passed'
            }
        }
        stage('Create kafka topics') {
            steps {
                sh 'echo TODO'
                // TODO
            }
        }
         stage('deploy to preprod') {
             steps {
                     dockerUtils action: 'createPushImage'
                     deployApp action: 'kubectlDeploy', cluster: 'preprod-fss'
                 }
             }
         stage('deploy to production') {
             when { environment name: 'DEPLOY_TO', value: 'production' }

             steps {
                     deployApp action: 'kubectlDeploy', cluster: 'prod-fss', file: 'naiserator-prod.yaml'
                 }
             }
        }
        post {
            always {
                postProcess action: 'always'
            }
            success {
                postProcess action: 'success'
            }
            failure {
                postProcess action: 'failure'
            }
        }
}
