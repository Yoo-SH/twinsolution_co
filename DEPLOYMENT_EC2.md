# AWS EC2 배포 가이드

## 1단계: EC2 인스턴스 생성

### 1-1. AWS 콘솔 접속 및 인스턴스 생성
1. AWS 콘솔 로그인 (https://console.aws.amazon.com)
2. **EC2** 서비스 선택
3. **인스턴스 시작** 클릭

### 1-2. 인스턴스 설정
- **이름**: `buildgenie-server` (선택사항)
- **AMI (Amazon Machine Image)**: 
  - Amazon Linux 2023 또는
  - Ubuntu Server 22.04 LTS
- **인스턴스 유형**: 
  - 최소: t3.medium (2 vCPU, 4GB RAM)
  - 권장: t3.large (2 vCPU, 8GB RAM)
- **키 페어**: 
  - 새 키 페어 생성 또는 기존 키 페어 선택
  - 키 페어 이름: `buildgenie-key`
  - 키 페어 다운로드 (`.pem` 파일) - **중요: 한 번만 다운로드 가능**

### 1-3. 네트워크 설정
- **퍼블릭 IP 자동 할당**: 활성화
- **보안 그룹**: 새 보안 그룹 생성
  - **SSH (22)**: 내 IP
  - **HTTP (80)**: 모든 트래픽 (0.0.0.0/0)
  - **HTTPS (443)**: 모든 트래픽 (0.0.0.0/0)
  - **커스텀 TCP (8080)**: 모든 트래픽 (0.0.0.0/0) - 백엔드용

### 1-4. 스토리지 설정
- **볼륨 크기**: 20GB 이상 (SSD 권장)
- **볼륨 유형**: gp3 (권장)

### 1-5. 인스턴스 시작
- **인스턴스 시작** 클릭
- 생성 완료 후 **퍼블릭 IP 주소** 확인

---

## 2단계: 보안 그룹 설정 확인

### 2-1. 보안 그룹 편집
1. EC2 콘솔 → **보안 그룹**
2. 생성한 보안 그룹 선택
3. **인바운드 규칙** 편집:
   ```
   Type          Protocol    Port Range    Source
   SSH           TCP         22            My IP
   HTTP          TCP         80            0.0.0.0/0
   HTTPS         TCP         443           0.0.0.0/0
   Custom TCP    TCP         8080          0.0.0.0/0
   ```

---

## 3단계: EC2 인스턴스 접속

### 3-1. SSH 접속 (Mac/Linux)
```bash
# 키 파일 권한 설정
chmod 400 buildgenie-key.pem

# EC2 접속 (Amazon Linux 2023)
ssh -i buildgenie-key.pem ec2-user@<EC2-퍼블릭-IP>

# 또는 Ubuntu의 경우
ssh -i buildgenie-key.pem ubuntu@<EC2-퍼블릭-IP>
```

### 3-2. Windows (PuTTY 사용)
1. PuTTYgen으로 `.pem` → `.ppk` 변환
2. PuTTY로 접속
   - Host: `ec2-user@<EC2-퍼블릭-IP>` (Amazon Linux)
   - 또는 `ubuntu@<EC2-퍼블릭-IP>` (Ubuntu)

---

## 4단계: EC2 환경 설정

### 4-1. 시스템 업데이트
```bash
# Amazon Linux 2023
sudo yum update -y

# Ubuntu
sudo apt update && sudo apt upgrade -y
```

### 4-2. Java 17 설치
```bash
# Amazon Linux 2023
sudo yum install java-17-amazon-corretto-devel -y

# Ubuntu
sudo apt install openjdk-17-jdk -y

# 확인
java -version
```

### 4-3. Node.js 및 npm 설치
```bash
# Amazon Linux 2023
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# Ubuntu
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 확인
node -v
npm -v
```

### 4-4. Git 설치
```bash
# Amazon Linux 2023
sudo yum install git -y

# Ubuntu
sudo apt install git -y
```

### 4-5. Nginx 설치 (프론트엔드 서빙용)
```bash
# Amazon Linux 2023
sudo amazon-linux-extras install nginx1 -y
# 또는
sudo yum install nginx -y

# Ubuntu
sudo apt install nginx -y

# 시작 및 자동 시작 설정
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 5단계: 코드 배포

### 5-1. 프로젝트 디렉토리 생성
```bash
# 홈 디렉토리로 이동
cd ~

# 프로젝트 디렉토리 생성
mkdir -p buildgenie
cd buildgenie
```

### 5-2. GitHub에서 코드 클론
```bash
# SSH 키 설정 (필요한 경우)
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# GitHub에 공개키 등록: ~/.ssh/id_rsa.pub

# 코드 클론
git clone git@github.com:TaeYunAhn/buildgenie.ai.kr.git .

# 또는 HTTPS 사용
git clone https://github.com/TaeYunAhn/buildgenie.ai.kr.git .
```

### 5-3. 또는 SCP로 파일 업로드 (로컬에서)
```bash
# 로컬 터미널에서 실행
scp -i buildgenie-key.pem -r /Users/tahn/Dev/twinsolution_co ec2-user@<EC2-IP>:~/buildgenie
```

---

## 6단계: 백엔드 배포

### 6-1. 백엔드 빌드
```bash
cd ~/buildgenie/backend

# Gradle Wrapper 권한 설정
chmod +x gradlew

# 빌드
./gradlew clean build -x test

# JAR 파일 확인
ls -la build/libs/
```

### 6-2. 데이터베이스 설정
```bash
# data 디렉토리 생성
mkdir -p data

# data.zip이 있다면 압축 해제
# unzip data.zip -d data/
```

### 6-3. application.yaml 확인
```bash
# application.yaml 확인 및 수정
nano src/main/resources/application.yaml
```

주요 설정 확인:
- `server.port: 8080`
- `spring.datasource.url: jdbc:h2:file:./data/twinsolution;AUTO_SERVER=TRUE`
- OpenAI API 키 확인

### 6-4. Systemd 서비스 생성 (권장)
```bash
# 서비스 파일 생성
sudo nano /etc/systemd/system/buildgenie-backend.service
```

서비스 파일 내용:
```ini
[Unit]
Description=BuildGenie Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user/buildgenie/backend
ExecStart=/usr/bin/java -jar /home/ec2-user/buildgenie/backend/build/libs/ConstructionSlm-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 6-5. 서비스 시작
```bash
sudo systemctl daemon-reload
sudo systemctl start buildgenie-backend
sudo systemctl enable buildgenie-backend
sudo systemctl status buildgenie-backend
```

### 6-6. 또는 nohup으로 실행 (간단한 방법)
```bash
cd ~/buildgenie/backend
nohup java -jar build/libs/ConstructionSlm-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

---

## 7단계: 프론트엔드 배포

### 7-1. 프론트엔드 빌드
```bash
cd ~/buildgenie/frontend

# 의존성 설치
npm install

# 환경 변수 설정 (필요한 경우)
nano .env.production
```

`.env.production` 내용:
```env
VITE_API_BASE_URL=http://<EC2-퍼블릭-IP>:8080
```

```bash
# 프로덕션 빌드
npm run build

# 빌드 결과 확인
ls -la dist/
```

### 7-2. Nginx 설정
```bash
# Nginx 설정 파일 생성
sudo nano /etc/nginx/conf.d/buildgenie.conf
```

Nginx 설정 내용:
```nginx
server {
    listen 80;
    server_name <EC2-퍼블릭-IP> 또는 <도메인>;

    root /home/ec2-user/buildgenie/frontend/dist;
    index index.html;

    # 프론트엔드 라우팅
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 백엔드 API 프록시
    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # H2 콘솔 프록시
    location /h2-console {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Swagger UI 프록시
    location /swagger-ui {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
}
```

### 7-3. Nginx 권한 설정 및 재시작
```bash
# Nginx가 dist 폴더 접근 가능하도록 권한 설정
sudo chmod -R 755 /home/ec2-user/buildgenie/frontend/dist

# 기본 설정 파일 비활성화 (Amazon Linux)
sudo rm /etc/nginx/conf.d/default.conf

# Nginx 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

---

## 8단계: 방화벽 확인

### 8-1. 포트 확인
```bash
# 포트 확인
sudo netstat -tlnp | grep -E ':(80|8080)'

# 또는
sudo ss -tlnp | grep -E ':(80|8080)'
```

### 8-2. 백엔드 로그 확인
```bash
# systemd 사용 시
sudo journalctl -u buildgenie-backend -f

# nohup 사용 시
tail -f ~/buildgenie/backend/backend.log
```

---

## 9단계: 배포 확인

### 9-1. 브라우저에서 접속
- **프론트엔드**: `http://<EC2-퍼블릭-IP>`
- **백엔드 API**: `http://<EC2-퍼블릭-IP>:8080/api/dashboard/summary`
- **H2 콘솔**: `http://<EC2-퍼블릭-IP>/h2-console`
- **Swagger UI**: `http://<EC2-퍼블릭-IP>/swagger-ui/index.html`

### 9-2. 서비스 상태 확인
```bash
# 백엔드 서비스 상태
sudo systemctl status buildgenie-backend

# Nginx 상태
sudo systemctl status nginx

# 프로세스 확인
ps aux | grep java
ps aux | grep nginx
```

---

## 10단계: 데이터베이스 백업 (선택사항)

```bash
# data 폴더 백업
cd ~/buildgenie/backend
tar -czf data-backup-$(date +%Y%m%d).tar.gz data/

# S3에 업로드 (선택사항)
aws s3 cp data-backup-*.tar.gz s3://your-bucket-name/backups/
```

---

## 11단계: 도메인 연결 (선택사항)

### 11-1. Route 53에서 도메인 설정
1. Route 53 콘솔 접속
2. 호스팅 영역 선택
3. 레코드 생성:
   - 레코드 유형: A
   - 이름: `@` 또는 `www`
   - 값: EC2 퍼블릭 IP

### 11-2. Nginx 설정 수정
```bash
sudo nano /etc/nginx/conf.d/buildgenie.conf
```

`server_name`을 도메인으로 변경:
```nginx
server_name buildgenie.ai.kr www.buildgenie.ai.kr;
```

### 11-3. SSL 인증서 설정 (Let's Encrypt)
```bash
# Certbot 설치
# Amazon Linux 2023
sudo yum install certbot python3-certbot-nginx -y

# Ubuntu
sudo apt install certbot python3-certbot-nginx -y

# SSL 인증서 발급
sudo certbot --nginx -d buildgenie.ai.kr -d www.buildgenie.ai.kr
```

---

## 12단계: 자동 배포 설정 (선택사항)

### 12-1. 배포 스크립트 생성
```bash
nano ~/buildgenie/deploy.sh
```

배포 스크립트 내용:
```bash
#!/bin/bash

cd ~/buildgenie

# 코드 업데이트
git pull origin main

# 백엔드 빌드 및 재시작
cd backend
./gradlew clean build -x test
sudo systemctl restart buildgenie-backend

# 프론트엔드 빌드
cd ../frontend
npm install
npm run build

# Nginx 재시작
sudo systemctl restart nginx

echo "Deployment completed!"
```

```bash
chmod +x ~/buildgenie/deploy.sh
```

### 12-2. GitHub Actions 설정 (선택사항)
`.github/workflows/deploy.yml` 파일 생성

---

## 문제 해결

### 백엔드가 시작되지 않는 경우
```bash
# 로그 확인
sudo journalctl -u buildgenie-backend -n 50
# 또는
tail -f ~/buildgenie/backend/backend.log

# 포트 확인
sudo lsof -i :8080

# JAR 파일 확인
ls -la ~/buildgenie/backend/build/libs/
```

### 프론트엔드가 표시되지 않는 경우
```bash
# Nginx 로그 확인
sudo tail -f /var/log/nginx/error.log

# 권한 확인
ls -la ~/buildgenie/frontend/dist

# Nginx 설정 확인
sudo nginx -t
```

### 포트 접근이 안 되는 경우
1. 보안 그룹 설정 확인
2. 방화벽 확인:
```bash
# Amazon Linux 2023
sudo firewall-cmd --list-all  # firewalld 사용 시
# 또는
sudo iptables -L  # iptables 사용 시

# Ubuntu
sudo ufw status
sudo ufw allow 80
sudo ufw allow 8080
```

---

## 체크리스트

- [ ] EC2 인스턴스 생성 완료
- [ ] 보안 그룹 설정 완료
- [ ] SSH 접속 성공
- [ ] Java 17 설치 완료
- [ ] Node.js 설치 완료
- [ ] Git 설치 완료
- [ ] Nginx 설치 완료
- [ ] 코드 배포 완료
- [ ] 백엔드 빌드 및 실행 완료
- [ ] 프론트엔드 빌드 완료
- [ ] Nginx 설정 완료
- [ ] 브라우저 접속 확인 완료

---

## 참고 사항

- EC2 인스턴스는 시간당 과금되므로 사용하지 않을 때는 중지하세요
- 퍼블릭 IP는 인스턴스를 중지/시작하면 변경될 수 있습니다 (Elastic IP 사용 권장)
- 보안 그룹은 최소한의 포트만 열어두는 것이 좋습니다
- 프로덕션 환경에서는 RDS나 다른 데이터베이스를 사용하는 것을 권장합니다

