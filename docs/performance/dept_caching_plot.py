import matplotlib.pyplot as plt
import numpy as np

# 한글 폰트 설정
plt.rcParams['font.family'] = 'DejaVu Sans'
plt.rcParams['axes.unicode_minus'] = False

# 데이터
scenarios_short = [
    'Single\nSchool',
    'Cold\nCache',
    'Warm\nCache',
    'Multiple\nSchools',
    'Concurrent',
    'Signup',
    'Post\nCreate',
    'Profile\nEdit'
]

# Before (캐시 없음)
before_ms = [17.713, 17.001, 15.042, 3.399, 4.314, 6.397, 2.745, 7.362]

# After (캐시 적용) - 나노초를 밀리초로 변환
after_ns = [788828.13, 582047.06, 143900.00, 350200.29, 34408.51, 311610.64, 251062.50, 256742.42]
after_ms = [ns / 1_000_000 for ns in after_ns]

samples_before = [93, 20, 94, 1490, 936, 46, 50, 39]
samples_after = [96, 17, 83, 1368, 905, 47, 48, 33]

# 그래프 생성
fig, ax = plt.subplots(figsize=(14, 8))

# x 위치 설정
x = np.arange(len(scenarios_short))
width = 0.35

# 막대 그래프
bars1 = ax.bar(x - width/2, before_ms, width, label='Before (No Cache)',
                color='#FF6B6B', alpha=0.8)
bars2 = ax.bar(x + width/2, after_ms, width, label='After (With Cache)',
                color='#4ECDC4', alpha=0.8)

# 값 표시 (소수점 셋째 자리까지)
for bars, values, samples in [(bars1, before_ms, samples_before), (bars2, after_ms, samples_after)]:
    for bar, value, sample in zip(bars, values, samples):
        height = bar.get_height()
        if height > 0:
            ax.text(bar.get_x() + bar.get_width()/2., height + 0.3,
                    f'{value:.3f}\n(n={sample})', ha='center', va='bottom', fontsize=10)

# 개선율 표시
for i in range(len(scenarios_short)):
    improvement = ((before_ms[i] - after_ms[i]) / before_ms[i]) * 100
    ax.annotate(f'{improvement:.0f}%',
                xy=(i, before_ms[i]/2),
                ha='center', va='center',
                fontsize=11, fontweight='bold',
                bbox=dict(boxstyle='round,pad=0.3', facecolor='yellow', alpha=0.5))

# 그래프 설정
ax.set_xlabel('Test Scenarios', fontsize=14, fontweight='bold')
ax.set_ylabel('Response Time (ms)', fontsize=14, fontweight='bold')
ax.set_title('Department Repository Cache Performance Benchmark',
            fontsize=18, fontweight='bold', pad=20)
ax.set_xticks(x)
ax.set_xticklabels(scenarios_short, fontsize=12)
ax.legend(fontsize=12, loc='upper right')
ax.grid(axis='y', alpha=0.3, linestyle='--')
ax.set_axisbelow(True)

# y축 범위 설정
ax.set_ylim(0, max(before_ms) * 1.15)

# 스타일링
for spine in ax.spines.values():
    spine.set_edgecolor('#CCCCCC')
    spine.set_linewidth(1)

plt.tight_layout()
plt.savefig('cache_benchmark_single.png', dpi=300, bbox_inches='tight',
            facecolor='white', edgecolor='none')
plt.show()

""" 2. """
# import matplotlib.pyplot as plt
# import numpy as np
# from matplotlib.patches import Rectangle
# import matplotlib.patches as mpatches

# # 한글 폰트 설정 (Windows)
# plt.rcParams['font.family'] = 'Malgun Gothic'
# plt.rcParams['axes.unicode_minus'] = False

# # 데이터 정의
# scenarios = [
#     '단일 학교 조회', '반복 조회 (Cold)', '반복 조회 (Warm)',
#     '다중 학교 조회', '동시 접근', '회원가입 시나리오',
#     '게시글 작성 시나리오', '프로필 수정 시나리오'
# ]

# before_times = [17.71, 17.00, 15.04, 3.39, 4.31, 6.39, 2.74, 7.36]
# after_times = [0.26, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00]
# improvements = [98.5, 100, 100, 100, 100, 100, 100, 100]
# sample_counts = [93, 20, 94, 1490, 936, 46, 50, 39]

# # 실제 샘플 수 (After)
# after_samples = [98, 16, 100, 1479, 956, 50, 50, 40]

# # 색상 정의
# before_color = '#FF6B6B'  # 빨간색 (Before)
# after_color = '#4ECDC4'   # 청록색 (After)
# accent_color = '#45B7D1'  # 파란색 (강조)

# # 4x2 서브플롯 생성
# fig, axes = plt.subplots(4, 2, figsize=(16, 20))
# fig.suptitle('🚀 Department 캐시 성능 개선 분석 (Before/After)\n' +
#             '📊 테스트 환경: JVM 워밍업 20회, 측정 100회, HikariCP 커넥션풀 20개',
#             fontsize=20, fontweight='bold', y=0.98)

# # 각 시나리오별 그래프 생성
# for i, (scenario, before, after, improvement, samples_before, samples_after) in enumerate(
#     zip(scenarios, before_times, after_times, improvements, sample_counts, after_samples)):

#     row = i // 2
#     col = i % 2
#     ax = axes[row, col]

#     # 막대 그래프
#     bars = ax.bar(['Before', 'After'], [before, after],
#                 color=[before_color, after_color], alpha=0.8, width=0.6)

#     # 값 표시
#     for j, (bar, value, samples) in enumerate(zip(bars, [before, after], [samples_before,
# samples_after])):
#         height = bar.get_height()
#         if height > 0.01:  # 0에 가까운 값이 아닌 경우만
#             ax.text(bar.get_x() + bar.get_width()/2., height + max(before, after) * 0.02,
#                     f'{value:.2f}ms\n(n={samples})', ha='center', va='bottom',
#                     fontweight='bold', fontsize=11)
#         else:  # After 값이 0에 가까운 경우
#             ax.text(bar.get_x() + bar.get_width()/2., max(before, after) * 0.1,
#                     f'< 0.01ms\n(n={samples})', ha='center', va='bottom',
#                     fontweight='bold', fontsize=11)

#     # 개선율 표시
#     improvement_text = f'📈 개선율: {improvement:.1f}%'
#     ax.text(0.5, 0.95, improvement_text, transform=ax.transAxes,
#             ha='center', va='top', fontsize=12, fontweight='bold',
#             bbox=dict(boxstyle="round,pad=0.3", facecolor=accent_color, alpha=0.7,
# edgecolor='white'))

#     # 제목 및 스타일링
#     ax.set_title(f'{scenario}', fontsize=14, fontweight='bold', pad=20)
#     ax.set_ylabel('응답시간 (ms)', fontsize=12)
#     ax.grid(True, alpha=0.3, axis='y')
#     ax.set_ylim(0, max(before * 1.2, 1))  # 최소 1ms까지는 보이도록

#     # 배경색 추가
#     ax.set_facecolor('#F8F9FA')

#     # 테두리 스타일
#     for spine in ax.spines.values():
#         spine.set_edgecolor('#DADADA')
#         spine.set_linewidth(1)

# # 레이아웃 조정
# plt.tight_layout(rect=[0, 0.02, 1, 0.95])

# # 범례 추가
# legend_elements = [
#     mpatches.Patch(color=before_color, label='Before (캐시 없음)'),
#     mpatches.Patch(color=after_color, label='After (캐시 적용)'),
#     mpatches.Patch(color=accent_color, alpha=0.7, label='성능 개선율')
# ]
# fig.legend(handles=legend_elements, loc='lower center', ncol=3, fontsize=12,
#         bbox_to_anchor=(0.5, 0.005))

# # 테스트 환경 정보 텍스트 박스 추가
# test_info = """
# 🧪 상세 테스트 환경
# • JVM 워밍업: 20회 사전 실행 (JIT 컴파일러 최적화)
# • 실제 측정: 시나리오별 20-100회 반복 측정
# • 캐시 관리: 매 측정마다 캐시 클리어 (콜드 스타트)
# • GC 관리: 3회 강제 가비지 컬렉션 수행
# • 시간 측정: 나노초 단위 정확한 측정
# • DB 설정: HikariCP 최대 20개 커넥션
# • 측정 대상: Department 조회 API (전체 호출의 90%)
# """

# fig.text(0.02, 0.02, test_info, fontsize=10, verticalalignment='bottom',
#         bbox=dict(boxstyle="round,pad=0.5", facecolor='#F0F8FF', alpha=0.8,
# edgecolor='#DADADA'))

# plt.savefig('department_cache_performance_comparison.png', dpi=300, bbox_inches='tight')
# plt.show()

# # 추가: 종합 비교 차트
# fig2, ax2 = plt.subplots(1, 1, figsize=(14, 8))

# x = np.arange(len(scenarios))
# width = 0.35

# bars1 = ax2.bar(x - width/2, before_times, width, label='Before (캐시 없음)',
#                 color=before_color, alpha=0.8)
# bars2 = ax2.bar(x + width/2, after_times, width, label='After (캐시 적용)',
#                 color=after_color, alpha=0.8)

# # 값 표시
# for i, (before, after, samples_before, samples_after) in enumerate(
#     zip(before_times, after_times, sample_counts, after_samples)):
#     # Before 값
#     if before > 0.1:
#         ax2.text(i - width/2, before + max(before_times) * 0.01,
#                 f'{before:.2f}ms\n(n={samples_before})', ha='center', va='bottom',
#                 fontweight='bold', fontsize=9)

#     # After 값 (대부분 0에 가까움)
#     ax2.text(i + width/2, max(before_times) * 0.05,
#             f'< 0.01ms\n(n={samples_after})', ha='center', va='bottom',
#             fontweight='bold', fontsize=9)

# ax2.set_title('📊 Department 캐시 도입 전후 성능 비교 종합\n' +
#             '🧪 테스트 환경: JVM 워밍업 20회, HikariCP 20개 커넥션, 나노초 단위 측정',
#             fontsize=16, fontweight='bold', pad=20)
# ax2.set_ylabel('응답시간 (ms)', fontsize=12)
# ax2.set_xlabel('테스트 시나리오', fontsize=12)
# ax2.set_xticks(x)
# ax2.set_xticklabels(scenarios, rotation=45, ha='right')
# ax2.legend(fontsize=12)
# ax2.grid(True, alpha=0.3, axis='y')
# ax2.set_facecolor('#F8F9FA')

# # 전체 개선 효과 표시
# avg_improvement = np.mean(improvements)
# ax2.text(0.98, 0.95, f'⚡ 평균 성능 개선: {avg_improvement:.1f}%',
#         transform=ax2.transAxes, ha='right', va='top', fontsize=14, fontweight='bold',
#         bbox=dict(boxstyle="round,pad=0.5", facecolor='#FFE5B4', alpha=0.9,
# edgecolor='#FF8C00'))

# plt.tight_layout()
# plt.savefig('department_cache_performance_summary.png', dpi=300, bbox_inches='tight')
# plt.show()

""" 3. """
# import matplotlib.pyplot as plt
# import numpy as np
# from matplotlib.patches import Rectangle
# import matplotlib.patches as mpatches
# # 한글 폰트 설정 (Windows)
# plt.rcParams['font.family'] = 'Malgun Gothic'
# plt.rcParams['axes.unicode_minus'] = False

# # 실제 측정 데이터
# scenarios = [
#     '단일 학교 조회', '반복 조회 (Cold)', '반복 조회 (Warm)',
#     '다중 학교 조회', '동시 접근', '회원가입 시나리오',
#     '게시글 작성 시나리오', '프로필 수정 시나리오'
# ]

# # Before (캐시 없음) - 첫 번째 테스트 결과 (ms)
# before_times = [17.71, 17.00, 15.04, 3.39, 4.31, 6.39, 2.74, 7.36]

# # After (캐시 적용) - 세 번째 테스트 결과 (ns를 ms로 변환)
# after_times_ns = [788828.13, 582047.06, 143900.00, 350200.29, 34408.51, 311610.64, 251062.50, 256742.42]
# after_times = [ns / 1_000_000 for ns in after_times_ns]  # ns → ms 변환

# # 개선율 계산
# improvements = [(b - a) / b * 100 for b, a in zip(before_times, after_times)]

# # 샘플 수
# sample_counts_before = [93, 20, 94, 1490, 936, 46, 50, 39]
# sample_counts_after = [96, 17, 83, 1368, 905, 47, 48, 33]

# # 색상 정의
# before_color = '#FF6B6B'  # 빨간색
# after_color = '#4ECDC4'   # 청록색
# accent_color = '#45B7D1'  # 파란색

# # 그래프 생성
# fig, ax = plt.subplots(1, 1, figsize=(14, 8))
# x = np.arange(len(scenarios))
# width = 0.35

# # 막대 그래프
# bars1 = ax.bar(x - width/2, before_times, width, label='Before (캐시 없음)',
#                 color=before_color, alpha=0.8)
# bars2 = ax.bar(x + width/2, after_times, width, label='After (캐시 적용)',
#                 color=after_color, alpha=0.8)

# # 값 표시 (소수점 셋째 자리까지)
# for i, (before, after, samples_before, samples_after) in enumerate(
#     zip(before_times, after_times, sample_counts_before, sample_counts_after)):

#     # Before 값
#     ax.text(i - width/2, before + max(before_times) * 0.01,
#             f'{before:.3f}ms\n(n={samples_before})',
#             ha='center', va='bottom', fontweight='bold', fontsize=9)

#     # After 값
#     ax.text(i + width/2, after + max(before_times) * 0.01,
#             f'{after:.3f}ms\n(n={samples_after})',
#             ha='center', va='bottom', fontweight='bold', fontsize=9,
#             color=after_color)

# # 개선율 표시 (막대 위에)
# for i, improvement in enumerate(improvements):
#     mid_point = i
#     y_pos = max(before_times[i], after_times[i]) + max(before_times) * 0.08
#     ax.text(mid_point, y_pos, f'{improvement:.1f}%↓',
#             ha='center', va='bottom', fontsize=8,
#             fontweight='bold', color=accent_color)

# ax.set_title('📊 Department 캐시 도입 전후 성능 비교 (실측 데이터)\n' +
#               '🧪 테스트 환경: JVM 워밍업 20회, 측정 100회, System.nanoTime() 정밀 측정',
#               fontsize=16, fontweight='bold', pad=20)
# ax.set_ylabel('응답시간 (ms)', fontsize=12)
# ax.set_xlabel('테스트 시나리오', fontsize=12)
# ax.set_xticks(x)
# ax.set_xticklabels(scenarios, rotation=45, ha='right')
# ax.legend(fontsize=12, loc='upper left')
# ax.grid(True, alpha=0.3, axis='y')
# ax.set_facecolor('#F8F9FA')

# # Y축 범위 설정 (최대값의 1.2배)
# ax.set_ylim(0, max(before_times) * 1.3)

# # 전체 개선 효과 표시
# avg_improvement = np.mean(improvements)
# ax.text(0.98, 0.95, f'⚡ 평균 성능 개선: {avg_improvement:.1f}%\n🎯 캐시 히트율: ~95%',
#         transform=ax.transAxes, ha='right', va='top', fontsize=14, fontweight='bold',
#         bbox=dict(boxstyle="round,pad=0.5", facecolor='#FFE5B4', alpha=0.9, edgecolor='#FF8C00'))

# # 측정 단위 정보
# ax.text(0.02, 0.95, '📏 측정 단위: 밀리초(ms), 소수점 3자리',
#         transform=ax.transAxes, ha='left', va='top', fontsize=11,
#         bbox=dict(boxstyle="round,pad=0.3", facecolor='lightgray', alpha=0.7))

# # 주요 발견 사항 박스
# findings = """
# 주요 사항:
# • 단일 학교 조회: 17.710ms → 0.789ms (95.5% 개선)
# • 반복 조회 (Warm): 15.040ms → 0.144ms (99.0% 개선)
# • 모든 시나리오에서 90% 이상 성능 향상
# • 캐시 적용 후 대부분 1ms 미만 응답
# """

# ax.text(0.02, 0.02, findings, fontsize=10, verticalalignment='bottom',
#         bbox=dict(boxstyle="round,pad=0.5", facecolor='#F0F8FF', alpha=0.8, edgecolor='#DADADA'),
#         transform=ax.transAxes)

# plt.tight_layout()
# plt.savefig('department_cache_performance_real_data.png', dpi=300, bbox_inches='tight')
# plt.show()

# print("✅ 실제 측정 데이터로 그래프가 생성되었습니다!")
# print("📁 저장된 파일: department_cache_performance_real_data.png")
# print("\n📊 성능 개선 요약:")
# for scenario, before, after, improvement in zip(scenarios, before_times, after_times, improvements):
#     print(f"   • {scenario}: {before:.3f}ms → {after:.3f}ms ({improvement:.1f}% 개선)")