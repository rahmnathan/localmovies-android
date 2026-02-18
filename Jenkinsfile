pipeline {
    agent {
        kubernetes {
            yaml """
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: jnlp
    image: rahmnathan/inbound-agent
    imagePullPolicy: Always
    tty: true
"""
        }
    }

    tools {
        jdk 'Java 21'
    }

    parameters {
        string(
                name: 'LOCALMOVIE_API_CLIENT_VERSION',
                defaultValue: '',
                description: 'Optional localmovie-api-client version. Leave blank to use gradle.properties'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    sh 'git config --global user.email "rahm.nathan@protonmail.com"'
                    sh 'git config --global user.name "rahmnathan"'
                    sshagent(credentials: ['Github-Git']) {
                        sh 'mkdir -p /home/jenkins/.ssh'
                        sh 'ssh-keyscan  github.com >> ~/.ssh/known_hosts'
                    }
                }
                checkout([$class           : 'GitSCM',
                          branches         : [[name: '*/master']],
                          extensions       : scm.extensions,
                          userRemoteConfigs: [[
                                                      url          : 'git@github.com:rahmnathan/localmovies-android.git',
                                                      credentialsId: 'Github-Git'
                                              ]]
                ])
            }
        }
        stage('Build') {
            steps {
                script {
                    if (params.LOCALMOVIE_API_CLIENT_VERSION?.trim()) {
                        sh "./gradlew bundleRelease -PlocalmovieApiClientVersion=${params.LOCALMOVIE_API_CLIENT_VERSION.trim()}"
                    } else {
                        sh './gradlew bundleRelease'
                    }
                }
            }
        }
        stage('Sign') {
            environment {
                KEYSTORE = credentials('localmovies-android-sign-key')
                KEYSTORE_PASSWORD = credentials('localmovies-android-sign-key-password')
            }
            steps {
                sh 'jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore $KEYSTORE -storepass $KEYSTORE_PASSWORD local-movie-app/build/outputs/bundle/release/local-movie-app-release.aab localmovies'
            }
        }
        stage('Upload') {
            steps {
                androidApkUpload googleCredentialsId: 'google-play-account',
                        trackName: 'production',
                        rolloutPercentage: '100'
            }
        }
    }
}
