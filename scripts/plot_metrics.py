#!/usr/bin/env python3
"""
RAG 챗봇 성능 지표 시각화 스크립트
"""
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.font_manager as fm

# 한글 폰트 설정 (Windows 환경)
plt.rcParams['font.family'] = 'Malgun Gothic'  # 또는 'NanumGothic'
plt.rcParams['axes.unicode_minus'] = False  # 마이너스 기호 깨짐 방지

# CSV 데이터
data = """faq_count,threshold,total_questions,correct_answers,accuracy,keyword_coverage,mrr,avg_response_ms,description
316,0.6,20,18,0.9,0.5185,N/A,6754.05,Initial 20-question baseline (pre-cache)
41,0.6,20,18,0.9,0.8148,N/A,9923.35,After cache bug fix - 20 questions
41,0.6,100,87,0.87,0.8915,N/A,8874.7,Full 100-question evaluation
41,0.65,100,92,0.92,0.8605,N/A,7749.87,threshold 0.6→0.65 (False Positive 감소 테스트)
41,0.7,100,83,0.83,0.814,N/A,6210.76,threshold 0.65→0.70 (False Positive 전면 차단 시도)
41,0.63,100,92,0.92,0.8992,N/A,7992.32,threshold 0.625 (0.60와 0.65 사이 최적값 탐색)
41,0.63,100,92,0.92,0.8915,0.6314,9640.46,MRR
41,0.63,100,91,0.91,0.8915,0.6451,9757.98,RAG Seed Reinforcement (5 Questions)
41,0.63,100,89,0.89,0.8837,0.6765,9719.71,RAG Seed Reinforcement (All)
41,0.6,100,87,0.87,0.8837,0.6882,9715.58,threshold=0.6
41,0.63,100,89,0.89,0.8837,0.6765,10219.3,threshold=0.63
41,0.63,100,93,0.93,0.8605,0.6588,9199.24,gpt reject detection
41,0.6,100,90,0.9,0.876,0.6765,9176.28,threshold=0.6
44,0.63,100,92,0.92,0.876,0.6608,8378.46,FAQ edit
44,0.63,100,91,0.91,0.8473,0.6686,9102.19,FAQ edit
45,0.63,100,95,0.95,0.8571,0.6531,8812.3,FAQ Addition"""

# 데이터 로드
from io import StringIO
df = pd.read_csv(StringIO(data))

# MRR 'N/A'를 NaN으로 변환
df['mrr'] = pd.to_numeric(df['mrr'], errors='coerce')

# 인덱스 생성 (실험 번호)
df['experiment'] = range(1, len(df) + 1)

# 그래프 생성 (하나의 그래프에 모든 지표)
fig, ax = plt.subplots(figsize=(14, 8))

# Accuracy
ax.plot(df['experiment'], df['accuracy'], marker='o', linewidth=2.5, markersize=7,
        color='#2E86AB', label='Accuracy', alpha=0.8)

# Keyword Coverage
ax.plot(df['experiment'], df['keyword_coverage'], marker='s', linewidth=2.5, markersize=7,
        color='#A23B72', label='Keyword Coverage', alpha=0.8)

# MRR (N/A 제거)
mrr_data = df.dropna(subset=['mrr'])
ax.plot(mrr_data['experiment'], mrr_data['mrr'], marker='^', linewidth=2.5, markersize=7,
        color='#F18F01', label='MRR', alpha=0.8)

# 그래프 설정
ax.set_xlabel('#', fontsize=13, fontweight='bold')
ax.set_ylabel('Score', fontsize=13, fontweight='bold')
ax.set_title('RAG Chatbot 성능 변화 추이', fontsize=16, fontweight='bold', pad=20)
ax.set_ylim(0.5, 1.0)
ax.grid(True, alpha=0.3, linestyle='--')
ax.legend(loc='lower right', fontsize=12, framealpha=0.9)

# 레이아웃 조정
plt.tight_layout()

# 저장
output_path = 'C:/dev/unibook/data/rag_metrics_visualization.png'
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"✅ 그래프 저장 완료: {output_path}")

# 화면에 표시
plt.show()

# 통계 요약 출력
print("\n📊 주요 통계:")
print(f"최종 Accuracy: {df.iloc[-1]['accuracy']:.2%}")
print(f"최종 Keyword Coverage: {df.iloc[-1]['keyword_coverage']:.2%}")
print(f"최종 MRR: {df.iloc[-1]['mrr']:.4f}")
print(f"\nAccuracy 개선폭: {df.iloc[0]['accuracy']:.2%} → {df.iloc[-1]['accuracy']:.2%} (+{(df.iloc[-1]['accuracy'] - df.iloc[0]['accuracy']):.2%})")
print(f"Keyword Coverage 개선폭: {df.iloc[0]['keyword_coverage']:.2%} → {df.iloc[-1]['keyword_coverage']:.2%} ({(df.iloc[-1]['keyword_coverage'] - df.iloc[0]['keyword_coverage']):.2%})")
