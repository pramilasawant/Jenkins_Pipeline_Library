// vars/deployHelloApp.groovy

def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            APP_NAME = config.appName ?: 'helloworldapplication'
            ENVIRONMENT = config.env ?: 'dev'
            NAMESPACE = config.namespace ?: 'default'
        }

        stages {
            stage('Prepare Deployment') {
                steps {
                    echo "Preparing deployment for ${APP_NAME} to ${ENVIRONMENT}..."
                    // Optional: Steps to prepare for deployment, such as building Docker images
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    echo "Deploying ${APP_NAME} to ${ENVIRONMENT} in namespace ${NAMESPACE}..."
                    // Command to deploy using Helm
                    sh """
                    helm upgrade --install ${APP_NAME} ./helm/ --namespace ${NAMESPACE} \
                    --set image.tag=${config.version ?: 'latest'}
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment of ${APP_NAME} to ${ENVIRONMENT} successful."
            }
            failure {
                echo "Deployment of ${APP_NAME} to ${ENVIRONMENT} failed."
                // Optionally add notifications or alerts for deployment failure (e.g., Slack, email)
            }
        }
    }
}
