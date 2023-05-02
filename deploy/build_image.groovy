DOCKER_TAG = "${env.ECR_IMAGE}:${params.IMAGE_TAG}"
currentBuild.displayName = "#${currentBuild.id} - ${params.IMAGE_TAG}"

node('spot-webframe-ami-update') {
  withCredentials([sshUserPrivateKey(credentialsId: env.GITHUB_RUNWAYCI_CREDENTIAL_ID, keyFileVariable: 'identityFile')]) {
    sh """#!/bin/bash
      cp ${identityFile} /root/.ssh/id_rsa
      chmod 400 /root/.ssh/id_rsa
      ssh-keyscan github.com > /root/.ssh/known_hosts
    """

    stage('Checkout') {
      checkout scm: [
        $class: 'GitSCM',
        branches: [[name: "*/$BRANCH"]],
        doGenerateSubmoduleConfigurations: false,
        extensions: [],
        submoduleCfg: [],
        userRemoteConfigs: [[
          credentialsId: env.GITHUB_RUNWAYCI_CREDENTIAL_ID,
          url: 'git@github.com:freshdesk/conductor.git'
        ]]
      ]
    }

    stage('Build') {
      dir('docker/') {
        sh "docker build -t ${DOCKER_TAG} --force-rm --no-cache -f docker/serverAndUI/Dockerfile ../"
      }
    }

    stage('Push') {
      sh "eval \$(aws ecr get-login --registry-ids ${env.AWS_ACCOUNT_ID} --no-include-email --region ${env.AWS_REGION})"
      sh "docker push ${DOCKER_TAG}"
    }
  }
}
