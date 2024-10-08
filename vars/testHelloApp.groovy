// vars/testHelloApp.groovy

def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            APP_NAME = config.appName ?: 'helloworldapplication'
        }

        stages {
            stage('Run Unit Tests') {
                steps {
                    echo "Running unit tests for ${APP_NAME}..."
                    // Command to run unit tests (customize based on your tech stack)
                    sh '''
                    # Example using Maven
                    mvn test
                    '''
                }
            }

            stage('Run Integration Tests') {
                steps {
                    echo "Running integration tests for ${APP_NAME}..."
                    // Command to run integration tests (if applicable)
                    sh '''
                    # Example for integration tests
                    mvn verify
                    '''
                }
            }
        }

        post {
            always {
                junit '**/target/surefire-reports/*.xml' // Archiving JUnit test results
            }
            success {
                echo "Tests passed for ${APP_NAME}."
            }
            failure {
                echo "Tests failed for ${APP_NAME}."
                // Optionally add notifications or alerts for failed tests (e.g., Slack, email)
            }
        }
    }
}
