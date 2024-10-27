def call() {
    pipeline {
        agent any

        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhubpwd')
            SLACK_CREDENTIALS = credentials('b3ee302b-e782-4d8e-ba83-7fa591d43205')
            SONARQUBE_CREDENTIALS = credentials('pipeline_Stoken')
            SONARQUBE_SERVER = 'http://localhost:9000'
            ANCHORE_URL = 'http://192.168.1.6:8228'
            ANCHORE_USERNAME = 'admin'
            ANCHORE_PASSWORD = 'foobar'
        }

        parameters {
            string(name: 'JAVA_REPO', defaultValue: 'https://github.com/pramilasawant/helloword1.git', description: 'Java Application Repository')
            string(name: 'DOCKERHUB_USERNAME', defaultValue: 'pramila188', description: 'DockerHub Username')
            string(name: 'JAVA_IMAGE_NAME', defaultValue: 'testhello', description: 'Java Docker Image Name')
            string(name: 'JAVA_NAMESPACE', defaultValue: 'test', description: 'Kubernetes Namespace for Java Application')
        }

        stages {
            stage('Clone Repository') {
                steps {
                    git url: params.JAVA_REPO, branch: 'main'
                }
            }

            stage('Build and Push Docker Image') {
                steps {
                    dir('testhello') {
                        sh 'mvn clean install'
                        script {
                            def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}")
                            docker.withRegistry('', 'dockerhubpwd') {
                                image.push()
                            }
                        }
                    }
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    dir('testhello') {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                mvn sonar:sonar \
                                    -Dsonar.projectKey=testhello \
                                    -Dsonar.host.url=${SONARQUBE_SERVER} \
                                    -Dsonar.login=${SONARQUBE_CREDENTIALS}
                            '''
                        }
                    }
                }
            }

            stage('Get Approval') {
                steps {
                    script {
                        input message: 'Do you approve this deployment?', ok: 'Yes, deploy'
                    }
                }
            }

            stage('Install yq') {
                steps {
                    sh '''
                        wget https://github.com/mikefarah/yq/releases/download/v4.6.1/yq_linux_amd64 -O "${WORKSPACE}/yq"
                        chmod +x "${WORKSPACE}/yq"
                        export PATH="${WORKSPACE}:$PATH"
                    '''
                }
            }

            stage('Build and Package Java Helm Chart') {
                steps {
                    dir('testhello') {
                        sh '''
                            "${WORKSPACE}/yq" e -i '.image.tag = "latest"' ./myspringbootchart/values.yaml
                            helm template ./myspringbootchart
                            helm lint ./myspringbootchart
                            helm package ./myspringbootchart --version "1.0.0"
                        '''
                    }
                }
            }

            stage('Deploy Java Application to Kubernetes') {
                steps {
                    script {
                        kubernetesDeploy(
                            configs: 'Build and Deploy Java and Python Applications',
                            kubeconfigId: 'kubeconfig1pwd'
                        )
                    }
                }
            }

            stage('Check Analysis Status') {
                steps {
                    script {
                        def IMAGE_DIGEST = sh (
                            script: "curl -s -u '${ANCHORE_USERNAME}:${ANCHORE_PASSWORD}' ${ANCHORE_URL}/v1/images/${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}/digest",
                            returnStdout: true
                        ).trim()
                        
                        def status = sh (
                            script: "curl -s -u '${ANCHORE_USERNAME}:${ANCHORE_PASSWORD}' ${ANCHORE_URL}/v1/images/${IMAGE_DIGEST} | jq '.analysis_status'",
                            returnStdout: true
                        ).trim()
                        echo "Analysis Status: ${status}"
                        
                        if (status != '"analyzed"') {
                            error "Image analysis is not complete. Please check Anchore for details."
                        }
                    }
                }
            }

            stage('Get Scan Results') {
                steps {
                    script {
                        def IMAGE_DIGEST = sh (
                            script: "curl -s -u '${ANCHORE_USERNAME}:${ANCHORE_PASSWORD}' ${ANCHORE_URL}/v1/images/${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}/digest",
                            returnStdout: true
                        ).trim()

                        def scanReport = sh (
                            script: "curl -s -u '${ANCHORE_USERNAME}:${ANCHORE_PASSWORD}' ${ANCHORE_URL}/v1/images/${IMAGE_DIGEST}/content",
                            returnStdout: true
                        ).trim()
                        echo "Scan Results: ${scanReport}"
                    }
                }
            }
        }

        post {
            always {
                script {
                    def slackBaseUrl = 'https://slack.com/api/'
                    def slackChannel = '#builds'
                    def slackColor = currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger'
                    def slackMessage = "Build ${currentBuild.fullDisplayName} finished with status: ${currentBuild.currentResult}"

                    echo "Sending Slack notification to ${slackChannel} with message: ${slackMessage}"

                    slackSend(
                        baseUrl: 'https://yourteam.slack.com/api/',
                        teamDomain: 'StarAppleInfotech',
                        channel: '#builds',
                        color: slackColor,
                        botUser: true,
                        tokenCredentialId: SLACK_CREDENTIALS,
                        notifyCommitters: false,
                        message: "Build Java Application #${env.BUILD_NUMBER} finished with status: ${currentBuild.currentResult}"
                    )
                }

                emailext(
                    to: 'pramila.narawadesv@gmail.com',
                    subject: "Jenkins Build ${env.JOB_NAME} #${env.BUILD_NUMBER} ${currentBuild.currentResult}",
                  
