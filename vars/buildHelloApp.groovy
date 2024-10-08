// vars/buildHelloApp.groovy

def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            APP_NAME = config.appName ?: 'helloworldapplication'
            VERSION = config.version ?: '1.0.0'
        }

        stages {
            stage('Checkout Code') {
                steps {
                    echo "Checking out code for ${APP_NAME}..."
                    // Checkout from the repository (this can be customized)
                    git url: 'https://github.com/pramilasawant/hellowordapplication.git', branch: 'main'
                }
            }

            stage('Build Application') {
                steps {
                    script {
                        echo "Building ${APP_NAME} version ${VERSION}..."
                        // Command to build the application, e.g., Maven, Gradle, or another build tool
                        sh '''
                        # Adjust the build commands for your specific application
                        mvn clean package
                        '''
                    }
                }
            }

            stage('Archive Artifacts') {
                steps {
                    echo "Archiving build artifacts for ${APP_NAME}..."
                    archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false
                }
            }
        }

        post {
            success {
                echo "${APP_NAME} build successful."
            }
            failure {
                echo "Build failed for ${APP_NAME}."
                // Optionally send notifications or alerts (e.g., Slack)
            }
        }
    }
}

