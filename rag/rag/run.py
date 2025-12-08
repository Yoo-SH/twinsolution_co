#!/usr/bin/env python3
"""
CLOVAX 실행 스크립트

개발 및 프로덕션 환경에서 애플리케이션을 실행할 수 있습니다.
"""

import os
import sys
import argparse
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="CLOVAX 실행 스크립트")
    parser.add_argument(
        "--env", 
        choices=["dev", "prod"], 
        default="dev",
        help="실행 환경 (기본값: dev)"
    )
    parser.add_argument(
        "--host", 
        default="0.0.0.0",
        help="호스트 주소 (기본값: 0.0.0.0)"
    )
    parser.add_argument(
        "--port", 
        type=int, 
        default=8000,
        help="포트 번호 (기본값: 8000)"
    )
    parser.add_argument(
        "--reload", 
        action="store_true",
        help="코드 변경 시 자동 재시작 (개발 모드)"
    )
    parser.add_argument(
        "--workers", 
        type=int, 
 default=1,
        help="워커 프로세스 수 (기본값: 1)"
    )
    
    args = parser.parse_args()
    
    # 환경 변수 설정
    if args.env == "dev":
        os.environ.setdefault("RELOAD", "true")
        os.environ.setdefault("DEBUG", "true")
        if not args.reload:
            args.reload = True
    else:
        os.environ.setdefault("RELOAD", "false")
        os.environ.setdefault("DEBUG", "false")
    
    # 호스트 및 포트 설정
    os.environ.setdefault("HOST", args.host)
    os.environ.setdefault("PORT", str(args.port))
    
    # CLOVA Studio API 키 확인
    if not os.getenv("CLOVA_STUDIO_API_KEY"):
        print("⚠️  경고: CLOVA_STUDIO_API_KEY 환경 변수가 설정되지 않았습니다.")
        print("   .env 파일을 생성하거나 환경 변수를 설정해주세요.")
        print("   예시: export CLOVA_STUDIO_API_KEY=your_api_key_here")
        print()
    
    # uvicorn 실행
    if args.env == "dev":
        print(f"🚀 CLOVAX 개발 서버를 시작합니다...")
        print(f"   호스트: {args.host}")
        print(f"   포트: {args.port}")
        print(f"   자동 재시작: {'활성화' if args.reload else '비활성화'}")
        print(f"   API 문서: http://{args.host}:{args.port}/docs")
        print()
        
        os.system(f"uvicorn main:app --host {args.host} --port {args.port} --reload")
    else:
        print(f"🚀 CLOVAX 프로덕션 서버를 시작합니다...")
        print(f"   호스트: {args.host}")
        print(f"   포트: {args.port}")
        print(f"   워커 수: {args.workers}")
        print()
        
        os.system(f"uvicorn main:app --host {args.host} --port {args.port} --workers {args.workers}")

if __name__ == "__main__":
    main()
