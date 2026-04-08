# API 명세 보관 폴더

이 폴더는 SolarWise 프로젝트에서 합의한 외부/내부 API 명세를 저장하는 공간입니다.

## 포함 문서
- `API.md`
  - 프론트엔드가 호출하는 공개 API 명세
  - 백엔드 전체 서비스 요구사항 및 내부 AI 연동 초안 포함
- `AI_API_명세서_final.md`
  - AI 서버(FastAPI) 팀과 협업할 때 기준이 되는 AI 추론 API 명세

## 확인 순서
1. `API.md`에서 프론트엔드 공개 API 경로와 응답 형식을 확인합니다.
2. `AI_API_명세서_final.md`에서 AI 서버 요청/응답 필드를 대조합니다.
3. `docs/planning/backend-work-plan.md`에서 현재 백엔드 구현 갭과 우선순위를 확인합니다.

## 현재 백엔드와의 핵심 차이
- 현재 컨트롤러 경로는 `/api/auth`, `/api/dashboard`, `/api/anomalies` 중심입니다.
- 첨부 명세는 `/api/v1/auth`, `/api/v1/plants`, `/api/v1/users/me` 등 도메인 중심 경로를 요구합니다.
- 공통 응답 형식(`success`, `data`, `message`)과 AI 내부 연동 계약은 아직 코드에 반영되지 않았습니다.

