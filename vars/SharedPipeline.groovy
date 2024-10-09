def call() {
    pipeline {
        agent any

        environment {
            APP_NAME = "${config.appName ?: 'helloworldapplication'}"
            VERSION = "${config.version ?: '1.0.0'}"
            NAMESPACE = "${config.namespace ?: 'default'}"
            DOCKER_IMAGE = "pramila188/${APP_NAME}:${VERSION}"  // Adjust to your DockerHub repository
            DOCKER_CREDENTIALS = 'dockerhunpwd'  // Your DockerHub credentials ID
        }

        stages {

            stage('Checkout Code') {
                steps {
                    echo "Checking out code for ${APP_NAME}..."
                    // Checkout from the repository (customize with your repo)
                    git url: 'https://github.com/pramilasawant/helloword1.git', branch: 'main'
                }
            }

            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building Docker image for ${APP_NAME}..."
                        sh """
                        docker build -t ${DOCKER_IMAGE} .
                        """
                    }
                }
            }

            stage('Push Docker Image') {
                steps {
                    script {
                        echo "Pushing Docker image ${DOCKER_IMAGE} to DockerHub..."
                        withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDENTIALS}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                            sh """
                            echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin
                            docker push ${DOCKER_IMAGE}
                            """
                        }
                    }
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    echo "Deploying ${APP_NAME} to Kubernetes..."
                    // Helm command to deploy application to Kubernetes
                    sh """
                    helm upgrade --install ${APP_NAME} ./helm/ --namespace ${NAMESPACE} \
                    --set image.repository=pramila188/${APP_NAME} --set image.tag=${VERSION}
                    """
                }
            }
        }

        post {
            success {
                echo "Pipeline execution successful for ${APP_NAME}."
            }
            failure {
                echo "Pipeline execution failed for ${APP_NAME}."
                // Optionally send notifications or alerts (e.g., Slack)
            }
        }
    }
}
