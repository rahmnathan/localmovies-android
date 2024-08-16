node {
    def jdk

    stage('Setup') {
        jdk = tool name: 'Java 21'
        env.JAVA_HOME = "${jdk}"
    }
    stage('Checkout') {
        git 'https://github.com/rahmnathan/localmovies-android.git'
    }
    stage('Build') {
        sh './gradlew bundleRelease'
    }
    stage('Sign') {
        withCredentials([
                file(credentialsId: 'localmovies-android-sign-key', variable: 'KEYSTORE'),
                string(credentialsId: 'localmovies-android-sign-key-password', variable: 'KEYSTORE_PASSWORD')
        ]) {
            sh 'jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore $KEYSTORE -storepass $KEYSTORE_PASSWORD local-movie-app/build/outputs/bundle/release/local-movie-app-release.aab localmovies'
        }
    }
    stage('Upload') {
        androidApkUpload googleCredentialsId: 'Google Play account',
                trackName: 'production',
                rolloutPercentage: '100'
    }
}