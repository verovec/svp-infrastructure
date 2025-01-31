pipeline {
    agent any

    environment {
        REPO_URL = 'git@github.com:verovec/svp-proxy.git'
        REPOSITORY_DIR = '/home/root/svp-proxy'
        HAPROXY_DIR = '/home/root'
        REMOTE_USER = "root"
        REMOTE_HOST = "10.89.155.33"
    }

    stages {
        stage('Deploy to Remote Server') {
            steps {
                script {
                    sshCommand = """
                        ssh -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} << EOF
                        if [ -d "${REPOSITORY_DIR}" ]; then
                            cd ${REPOSITORY_DIR} && git pull origin main
                        else
                            git clone ${REPO_URL} ${REPOSITORY_DIR}
                        fi
                        cp ${REPOSITORY_DIR} ${HAPROXY_DIR}/haproxy.cfg
                        cd ${HAPROXY_DIR}
                        docker-compose down
                        docker-compose up -d
                        EOF
                    """
                    sh sshCommand
                }
            }
        }
    }

    post {
        success {
            echo "Deployment completed successfully!"
        }
        failure {
            echo "Deployment failed!"
        }
    }
}
