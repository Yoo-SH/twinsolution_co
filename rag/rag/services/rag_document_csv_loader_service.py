from langchain_community.document_loaders import CSVLoader
from langchain_core.documents import Document
from typing import List, Optional, Dict, Any
import logging
import os
import pandas as pd
import tempfile

logger = logging.getLogger(__name__)

class CSVDocumentLoaderService:
    """CSV 문서 로더 서비스"""

    def __init__(self):
        self.supported_extensions = ['.csv']

    def load_csv(self, file_path: str, encoding: str = 'utf-8') -> List[Document]:
        """
        CSV 파일을 로드하여 Document 리스트로 반환합니다.

        Args:
            file_path: CSV 파일 경로
            encoding: 파일 인코딩 (기본값: utf-8)

        Returns:
            List[Document]: 로드된 Document 리스트
        """
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {file_path}")

        if not file_path.lower().endswith('.csv'):
            raise ValueError("지원하지 않는 파일 형식입니다. CSV 파일만 허용됩니다.")

        try:
            # pandas로 CSV 파일 읽기
            df = pd.read_csv(file_path, encoding=encoding)
            
            if df.empty:
                logger.warning("CSV 파일이 비어 있습니다.")
                return []

            documents = []
            
            # 각 행을 Document로 변환
            for idx, row in df.iterrows():
                # 한 행의 텍스트 변환
                row_text = self._row_to_text(row, df.columns.tolist())
                
                # 메타데이터 생성
                metadata = {
                    'source_file': os.path.basename(file_path),
                    'file_type': 'csv',
                    'loader_type': 'CSVLoader',
                    'row_index': int(idx),
                    'total_rows': len(df)
                }
                
                # 각 컬럼별 메타데이터 추가
                for col in df.columns:
                    metadata[f'col_{col}'] = str(row[col]) if pd.notna(row[col]) else ""
                
                doc = Document(
                    page_content=row_text,
                    metadata=metadata
                )
                documents.append(doc)

            logger.info(f"CSV 문서 로드 완료: {len(documents)}개 문서")
            return documents

        except UnicodeDecodeError:
            # UTF-8로 읽기 실패 시 cp949, latin-1 순서로 재시도
            try:
                return self.load_csv(file_path, encoding='cp949')
            except:
                try:
                    return self.load_csv(file_path, encoding='euc-kr')
                except:
                    try:
                        return self.load_csv(file_path, encoding='latin-1')
                    except Exception as e:
                        logger.error(f"CSV 파일 인코딩 오류: {str(e)}")
                        raise Exception(f"CSV 파일 인코딩을 확인할 수 없습니다: {str(e)}")
        except Exception as e:
            logger.error(f"CSV 파일 로드 오류: {str(e)}")
            raise Exception(f"CSV 파일 로드 중 오류 발생: {str(e)}")

    def _row_to_text(self, row: pd.Series, columns: List[str]) -> str:
        """
        pandas Series(한 행)를 텍스트로 변환합니다.

        Args:
            row: pandas Series (한 행)
            columns: 컬럼명 리스트

        Returns:
            str: 한 행의 텍스트
        """
        text_parts = []
        
        for col in columns:
            value = row[col]
            if pd.notna(value) and str(value).strip():
                text_parts.append(f"{col}: {str(value).strip()}")
        
        return " | ".join(text_parts)

    def preprocess_text(self, text: str) -> str:
        """
        텍스트 전처리

        Args:
            text: 원본 텍스트

        Returns:
            str: 전처리된 텍스트
        """
        if not text:
            return ""

        # 공백 문자 정규화
        import re
        text = re.sub(r'\s+', ' ', text)
        
        # 양쪽 공백 제거
        text = text.strip()

        return text

    def load_and_preprocess(self, file_path: str, encoding: str = 'utf-8') -> List[Document]:
        """
        CSV 파일을 로드하고 전처리하여 반환합니다.

        Args:
            file_path: CSV 파일 경로
            encoding: 파일 인코딩

        Returns:
            List[Document]: 전처리된 Document 리스트 (빈 텍스트 제외)
        """
        # 1. CSV 로드 (각 행을 Document로)
        documents = self.load_csv(file_path, encoding)

        # 2. 각 Document의 텍스트 전처리
        processed_documents = []
        for doc in documents:
            processed_text = self.preprocess_text(doc.page_content)
            if processed_text.strip():  # 빈 텍스트는 제외
                processed_doc = Document(
                    page_content=processed_text,
                    metadata=doc.metadata
                )
                processed_documents.append(processed_doc)

        logger.info(f"전처리된 CSV 문서 개수: {len(processed_documents)}개")
        return processed_documents

    def save_temp_csv_file(self, file_content: bytes, filename: str) -> str:
        """
        업로드된 CSV 파일을 임시 파일로 저장합니다.

        Args:
            file_content: CSV 파일 바이너리 데이터
            filename: 원본 파일명

        Returns:
            str: 임시 파일 경로
        """
        with tempfile.NamedTemporaryFile(delete=False, suffix='.csv', prefix=f"{filename}_") as tmp_file:
            tmp_file.write(file_content)
            return tmp_file.name

# 싱글턴 인스턴스
csv_document_loader_service = CSVDocumentLoaderService()