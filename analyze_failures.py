#!/usr/bin/env python3
"""
평가 결과에서 틀린 케이스만 추출하고 분석하는 스크립트
"""
import json
from collections import defaultdict

# 최신 평가 결과 파일 읽기
with open('data/evaluation-result-2025-10-26T20-30-06.743155.json', 'r', encoding='utf-8') as f:
  result = json.load(f)

# 틀린 케이스만 필터링
failed_cases = [q for q in result['questionResults'] if not q['correct']]

print(f"📊 전체 {result['totalQuestions']}개 중 {len(failed_cases)}개 실패\n")
print("=" * 80)

# 난이도별 실패 분석
failures_by_difficulty = defaultdict(list)
for case in failed_cases:
  failures_by_difficulty[case['difficulty']].append(case)

print("\n📈 난이도별 실패 분포:")
for difficulty, cases in sorted(failures_by_difficulty.items()):
  print(f"  {difficulty}: {len(cases)}개")

print("\n" + "=" * 80)
print("\n🔍 실패 케이스 상세 분석:\n")

for i, case in enumerate(failed_cases, 1):
  print(f"\n[{i}/{len(failed_cases)}] ID: {case['questionId']}")
  print(f"📌 난이도: {case['difficulty']}")
  print(f"❓ 질문: {case['question']}")
  print(f"✅ 예상: {'매칭 필요' if case['shouldMatch'] else '거부 필요'}")
  print(f"❌ 실제: {'매칭됨' if case['actuallyMatched'] else '거부됨'}")

  if case['totalKeywords'] > 0:
    print(f"🔑 키워드: {case['keywordsFound']}/{case['totalKeywords']} 포함")

  if case['answer']:
    answer_preview = case['answer'][:150] + "..." if len(case['answer']) > 150 else case['answer']
    print(f"💬 답변: {answer_preview}")

  print("-" * 80)

# 실패 유형 분류
false_positives = [c for c in failed_cases if not c['shouldMatch'] and c['actuallyMatched']]
false_negatives = [c for c in failed_cases if c['shouldMatch'] and not c['actuallyMatched']]

print(f"\n📊 실패 유형 요약:")
print(f"  False Positive (불필요한 매칭): {len(false_positives)}개")
print(f"  False Negative (매칭 누락): {len(false_negatives)}개")

# 결과를 파일로도 저장
with open('data/failure-analysis.txt', 'w', encoding='utf-8') as f:
  f.write(f"실패 케이스 분석 ({result['timestamp']})\n")
  f.write(f"전체 {result['totalQuestions']}개 중 {len(failed_cases)}개 실패\n\n")

  for i, case in enumerate(failed_cases, 1):
    f.write(f"\n[{i}] ID: {case['questionId']}\n")
    f.write(f"난이도: {case['difficulty']}\n")
    f.write(f"질문: {case['question']}\n")
    f.write(f"예상: {'매칭' if case['shouldMatch'] else '거부'} / 실제: {'매칭' if case['actuallyMatched'] else '거부'}\n")
    if case['answer']:
      f.write(f"답변: {case['answer']}\n")
    f.write("-" * 80 + "\n")

print("\n✅ 상세 분석 결과가 data/failure-analysis.txt에 저장되었습니다.")
