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

    stages {
        stage('Checkout') {
            steps {
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
                sh './gradlew bundleRelease'
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