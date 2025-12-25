# 네이버 클라우드 플랫폼 배포 가이드

## 1단계: 네이버 클라우드 플랫폼 서버 생성

### 1-1. 콘솔 접속 및 서버 생성
1. 네이버 클라우드 플랫폼 콘솔 접속 (https://console.ncloud.com)
2. **Server > Server** 메뉴 선택
3. **서버 생성** 클릭

### 1-2. 서버 사양 설정
- **이미지**: Ubuntu 22.04 LTS 또는 CentOS 7.9
- **서버 사양**: 
  - 최소: vCPU 2개, 메모리 4GB
  - 권장: vCPU 4개, 메모리 8GB
- **스토리지**: 50GB 이상 (SSD 권장)
- **네트워크**: 기본 VPC 선택 또는 새로 생성
- **ACG(Access Control Group)**: 
  - SSH (22): 내 IP
  - HTTP (80): 모든 IP
  - HTTPS (443): 모든 IP
  - Custom TCP (8080): 모든 IP (백엔드)

### 1-3. 인증키 생성 및 다운로드
1. **인증키 생성** 클릭
2. 인증키 이름 입력 (예: `buildgenie-key`)
3. 인증키 다운로드 (`.pem` 파일)

### 1-4. 서버 생성 완료
- 서버 이름: `buildgenie-server`
- 생성 완료 후 **공인 IP** 확인

---

## 2단계: 서버 접속

### 2-1. SSH 접속 (Mac/Linux)
```bash
# 키 파일 권한 설정
chmod 400 buildgenie-key.pem

# 서버 접속
ssh -i buildgenie-key.pem root@<공인-IP>
# 또는 Ubuntu의 경우
ssh -i buildgenie-key.pem ubuntu@<공인-IP>
```

### 2-2. Windows (PuTTY 사용)
1. PuTTYgen으로 `.pem` → `.ppk` 변환
2. PuTTY로 접속

---

## 3단계: 서버 환경 설정

### 3-1. 시스템 업데이트
```bash
# Ubuntu
sudo apt update && sudo apt upgrade -y

# CentOS
sudo yum update -y
```

### 3-2. Java 17 설치
```bash
# Ubuntu
sudo apt install openjdk-17-jdk -y

# CentOS
sudo yum install java-17-openjdk-devel -y

# 확인
java -version
```

### 3-3. Node.js 및 npm 설치
```bash
# Node.js 20.x 설치
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# 또는 CentOS
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# 확인
node -v
npm -v
```

### 3-4. Git 설치
```bash
# Ubuntu
sudo apt install git -y

# CentOS
sudo yum install git -y
```

### 3-5. Nginx 설치
```bash
# Ubuntu
sudo apt install nginx -y

# CentOS
sudo yum install nginx -y

# 시작 및 자동 시작 설정
sudo systemctl start nginx
sudo systemctl enable nginx
```

---

## 4단계: 코드 배포

### 4-1. 프로젝트 디렉토리 생성
```bash
cd /opt
sudo mkdir -p buildgenie
sudo chown $USER:$USER buildgenie
cd buildgenie
```

### 4-2. GitHub에서 코드 클론
```bash
# SSH 키 설정 (필요한 경우)
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# GitHub에 공개키 등록

# 코드 클론
git clone git@github.com:TaeYunAhn/buildgenie.ai.kr.git .

# 또는 HTTPS 사용
git clone https://github.com/TaeYunAhn/buildgenie.ai.kr.git .
```

---

## 5단계: 백엔드 배포

### 5-1. 백엔드 빌드
```bash
cd /opt/buildgenie/backend

# Gradle Wrapper 권한 설정
chmod +x gradlew

# 빌드
./gradlew clean build -x test

# JAR 파일 확인
ls -la build/libs/
```

### 5-2. 데이터베이스 설정
```bash
# data 디렉토리 생성
mkdir -p data

# data.zip이 있다면 압축 해제
# unzip data.zip -d data/
```

### 5-3. application.yaml 설정
```bash
# application.yaml 확인 및 수정
nano src/main/resources/application.yaml
```

주요 설정:
- `server.port: 8080`
- `spring.datasource.url: jdbc:h2:file:./data/twinsolution;AUTO_SERVER=TRUE`
- OpenAI API 키 확인

### 5-4. Systemd 서비스 생성
```bash
sudo nano /etc/systemd/system/buildgenie-backend.service
```

서비스 파일 내용:
```ini
[Unit]
Description=BuildGenie Backend Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/buildgenie/backend
ExecStart=/usr/bin/java -jar /opt/buildgenie/backend/build/libs/ConstructionSlm-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 5-5. 서비스 시작
```bash
sudo systemctl daemon-reload
sudo systemctl start buildgenie-backend
sudo systemctl enable buildgenie-backend
sudo systemctl status buildgenie-backend
```

---

## 6단계: 프론트엔드 배포

### 6-1. 프론트엔드 빌드
```bash
cd /opt/buildgenie/frontend

# 의존성 설치
npm install

# 환경 변수 설정 (필요한 경우)
nano .env.production
```

`.env.production` 내용:
```env
VITE_API_BASE_URL=http://<공인-IP>:8080
```

```bash
# 프로덕션 빌드
npm run build

# 빌드 결과 확인
ls -la dist/
```

### 6-2. Nginx 설정
```bash
sudo nano /etc/nginx/sites-available/buildgenie
```

Nginx 설정 내용:
```nginx
server {
    listen 80;
    server_name <공인-IP> 또는 <도메인>;

    root /opt/buildgenie/frontend/dist;
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

### 6-3. Nginx 설정 활성화
```bash
# Ubuntu
sudo ln -s /etc/nginx/sites-available/buildgenie /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default

# CentOS
sudo cp /etc/nginx/sites-available/buildgenie /etc/nginx/conf.d/buildgenie.conf
```

### 6-4. Nginx 재시작
```bash
# 설정 테스트
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
```

---

## 7단계: ACG(Access Control Group) 설정

### 7-1. 콘솔에서 ACG 설정
1. 네이버 클라우드 콘솔 → **Server > ACG**
2. 서버에 연결된 ACG 선택
3. **인바운드 규칙** 추가:
   ```
   규칙 이름    프로토콜    포트 범위    허용 IP
   SSH         TCP         22          내 IP
   HTTP        TCP         80          0.0.0.0/0
   HTTPS       TCP         443         0.0.0.0/0
   Backend     TCP         8080        0.0.0.0/0
   ```

---

## 8단계: 배포 확인

### 8-1. 서비스 상태 확인
```bash
# 백엔드 서비스 상태
sudo systemctl status buildgenie-backend

# Nginx 상태
sudo systemctl status nginx

# 포트 확인
sudo netstat -tlnp | grep -E ':(80|8080)'
```

### 8-2. 로그 확인
```bash
# 백엔드 로그
sudo journalctl -u buildgenie-backend -f

# Nginx 로그
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

### 8-3. 브라우저에서 접속
- 프론트엔드: `http://<공인-IP>`
- 백엔드 API: `http://<공인-IP>:8080/api/dashboard/summary`
- H2 콘솔: `http://<공인-IP>/h2-console`
- Swagger UI: `http://<공인-IP>/swagger-ui/index.html`

---

## 9단계: 도메인 연결 (선택사항)

### 9-1. 도메인 DNS 설정
1. 도메인 제공업체에서 A 레코드 추가
2. 호스트: `@` 또는 `www`
3. 값: 서버 공인 IP

### 9-2. Nginx 설정 수정
```bash
sudo nano /etc/nginx/sites-available/buildgenie
```

`server_name`을 도메인으로 변경:
```nginx
server_name buildgenie.ai.kr www.buildgenie.ai.kr;
```

### 9-3. SSL 인증서 설정 (Let's Encrypt)
```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# SSL 인증서 발급
sudo certbot --nginx -d buildgenie.ai.kr -d www.buildgenie.ai.kr
```

---

## 10단계: 자동 배포 설정 (선택사항)

### 10-1. 배포 스크립트 생성
```bash
nano /opt/buildgenie/deploy.sh
```

배포 스크립트 내용:
```bash
#!/bin/bash

cd /opt/buildgenie

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
chmod +x /opt/buildgenie/deploy.sh
```

### 10-2. GitHub Actions 설정 (선택사항)
`.github/workflows/deploy.yml` 파일 생성

---

## 문제 해결

### 백엔드가 시작되지 않는 경우
```bash
# 로그 확인
sudo journalctl -u buildgenie-backend -n 50

# 포트 확인
sudo lsof -i :8080

# JAR 파일 확인
ls -la /opt/buildgenie/backend/build/libs/
```

### 프론트엔드가 표시되지 않는 경우
```bash
# Nginx 로그 확인
sudo tail -f /var/log/nginx/error.log

# 권한 확인
ls -la /opt/buildgenie/frontend/dist

# Nginx 설정 확인
sudo nginx -t
```

### 포트 접근이 안 되는 경우
1. ACG 설정 확인
2. 방화벽 확인:
```bash
# Ubuntu
sudo ufw status
sudo ufw allow 80
sudo ufw allow 8080

# CentOS
sudo firewall-cmd --list-all
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --reload
```

---

## 체크리스트

- [ ] 네이버 클라우드 서버 생성 완료
- [ ] ACG 설정 완료
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

- 네이버 클라우드 플랫폼은 리전별로 가격이 다를 수 있습니다
- 공인 IP는 서버 생성 시 자동 할당되거나 별도로 신청해야 할 수 있습니다
- ACG는 AWS의 보안 그룹과 유사한 기능입니다
- 서버 사양은 트래픽에 따라 조정이 필요할 수 있습니다

