## 1. 브랜치 전략

```tsx
feat - 기능
bug - 버그 수정 (기능 동작 안함)
fix - 설정 수정, 기능 변경없는 간단한 수정  
improve - 기능 개선
setting - 공통 환경 설정

prod - stage - backend - be/feat/<지라키>-작업내용(브랜치)
             |           L /bug/<지라키>-작업내용(브랜치)
             |           L /fix/<지라키>-작업내용(브랜치)
             |           L /setting/<지라키>-작업내용(브랜치)
             |
             |
             L frontend - fe/feat/<지라키>-작업내용
```

### FE 브랜치

| frontend | FE 배포용 |
| --- | --- |
| feat/ | 기능 단위별 작업 브랜치 |
| fix/ | 버그 수정 브랜치 |
| refactor/ | 기능 변경 없는 수정 브랜치 |
| chore/ | 설정 관련 브랜치 |
- `prod` : **프로덕션**(배포 완료 상태)만 존재
- `stage` : **QA/통합 테스트용**(릴리즈 후보)
- `frontend`, `backend` : **개발 통합 브랜치(각 영역별)**
    - `fe/`
        - `feat/<JIRA-KEY>-(short-desc)`
        - `fix/<JIRA-KEY>-(short-desc)`
        - `refactor/<JIRA-KEY>-(short-desc)`
        - `chore/<JIRA-KEY>-(short-desc)`

## 2. 코드 컨벤션

### 커밋

- 커밋 규칙: feat: [FE] [JIRA-KEY] 로그인 리다이렉트 수정
- 커밋 메시지: 국문
- conventional commits ([참고 링크](https://overcome-the-limits.tistory.com/entry/%ED%98%91%EC%97%85-%ED%98%91%EC%97%85%EC%9D%84-%EC%9C%84%ED%95%9C-%EA%B8%B0%EB%B3%B8%EC%A0%81%EC%9D%B8-git-%EC%BB%A4%EB%B0%8B%EC%BB%A8%EB%B2%A4%EC%85%98-%EC%84%A4%EC%A0%95%ED%95%98%EA%B8%B0))
    
    
    | type | 사용하는 곳 |
    | --- | --- |
    | feat | 기능 추가 |
    | fix | 버그 수정 |
    | refactor | 리팩토링(동작은 동일) |
    | perf | 성능 개선 |
    | style | CSS/레이아웃 |
    | docs | 문서 |
    | chore | 설정 |

### 기본 원칙

- **TypeScript를 우선 적용**: `any` 금지(불가피할 때 최소화하고 `TODO` 남김)
- **함수형 컴포넌트 + 훅** 우선, 클래스 컴포넌트 사용 금지
- **불변성** 유지, 상태 최소화, 단방향 데이터 흐름
- **사이드이펙트는 훅 내부로** (`useEffect`, 커스텀 훅)

### React 규칙

- 컴포넌트 파일당 **기능 1개 원칙** (큰 컴포넌트는 분리)
- Props는 **명시적 타입** + `memo`/`useMemo`/`useCallback`은 필요 시에만
- DOM 접근은 `ref` 우선, 직접 조작 최소화
- 접근성(a11y) 준수: `alt`, `aria-*` 속성, 키보드 포커스 고려

### 상태 관리

- 범용: React Query/훅 중심
- 전역 상태가 필요할 때만 상태관리 라이브러리 도입 (Zustand)

### 예시 스니펫

```tsx
// 명시적 타입 + type-only import
import type { User } from '@/types/user';

type Props = { user: User };

export function UserCard({ user }: Props) {
  return <div>{user.name}</div>;
}
```

### 네이밍 규칙

- **파일명은 `소문자-소문자`(kebab-case)** 를 기본으로 합니다. (컴포넌트 파일 제외)
    - 예: `auth-service.ts`, `use-fetch.ts`
- **컴포넌트 파일/컴포넌트명은 PascalCase**
    - 예: `UserCard.tsx`, `function UserCard() { ... }`
- **훅(Hook)**: `use-무엇.ts` (파일명), `use무엇` (함수명)
- **테스트 파일**: `.test.ts` 또는 `.test.tsx`
- **스타일 파일**: CSS Modules 사용 시 `.module.css` (또는 `.module.scss`)
- **폴더명** 역시 kebab-case를 기본으로 합니다.

> 예외: 환경설정/최상위 파일(README.md, .eslintrc.cjs, tsconfig.json 등)은 관례 유지
> 

## 3. PR 템플릿

### 제목

Jira 티켓 제목과 일치

```
feat:[FE][JIRA-TICKET] 로그인 리다이렉트 수정
```

### 본문

```tsx
#️⃣ 관련 이슈
#이슈 번호

📝 작업 내용
- 
- 

✅ PR 체크리스트
> PR이 다음 요구 사항을 충족하는지 확인하세요.

- [ ] 커밋 메시지 컨벤션에 맞게 작성했습니다.  [SSGN-no.] 작업분류(e.g. feat):작업내용
- [ ] 변경 사항에 대한 테스트를 했습니다.(버그 수정/기능에 대한 테스트).

💬 리뷰 요구사항(선택)
> 리뷰어가 특별히 봐주었으면 하는 부분이 있다면 작성해주세요

📸 스크린샷(선택)
```

### 리뷰 승인 기준

 Approve

- PR 설명과 실제 코드가 일치
- 콘솔 에러 / 빌드 에러 없음
- 규칙 위반 없음
- 작은 개선사항만 존재하거나 이미 반영됨

Request changes

- 기능이 의도대로 동작하지 않음
- 코드 구조나 로직이 불안정
- eslint/prettier/type-check 위반 있음
- 문서/이슈 연결 누락

Comment only

- 기능엔 문제 없지만, 개선 의견 전달

### 머지 규칙

- feat → frontend 스쿼시 머지
    - 최소 1인 승인 → 본인이 PR 닫고, feature 브랜치 삭제
    - 스쿼시 머지 커밋 메시지: PR 제목과 동일하게

### MoSCoW 리뷰 컨벤션

**목적**

리뷰 과정에서

- **지금 반드시 고쳐야 하는 것**
- **지금은 안 해도 되는 것**
- **의견 차이일 뿐인 것**

을 명확히 구분해 **불필요한 논쟁을 줄이고**, **의사결정을 빠르게** 하기 위함.

---

### **MoSCoW 분류 기준**

🔴 **Must (반드시 수정 필요)**

> 이번 범위에서 해결하지 않으면 안 되는 문제
> 

**기준**

- 기능이 정상적으로 동작하지 않음
- 버그, 오류, 크래시 가능성
- 요구사항 미충족
- 보안/데이터 무결성 문제
- 배포 후 즉시 문제될 가능성

**리뷰 문구 예시**

- `[Must] 이 경우 저장이 되지 않습니다. 수정 필요합니다.`
- `[Must] 요구사항에 명시된 동작과 다릅니다.`
- `[Must] 에러 처리 누락으로 사용자 데이터 손실 가능성이 있습니다.`

👉 **Must는 합의 대상이 아님**

→ 수정 후에만 다음 단계로 진행

---

🟠 **Should (가능하면 수정 권장)**

> 지금 고치면 품질이 눈에 띄게 좋아지는 부분
> 

**기준**

- UX/가독성/유지보수성 개선
- 리팩토링 후보
- 성능 최적화 가능 포인트
- 일관성 개선 (네이밍, 구조 등)

**리뷰 문구 예시**

- `[Should] 이 로직을 분리하면 재사용성이 좋아질 것 같습니다.`
- `[Should] 사용자 입장에서 이 흐름이 조금 헷갈릴 수 있어요.`
- `[Should] 추후 확장 시를 고려하면 이 구조가 더 나아 보입니다.`

👉 **일정·우선순위에 따라 조정 가능**

---

🟡 **Could (해도 좋고 안 해도 됨)**

> 아이디어/개선 제안 수준
> 

**기준**

- 더 좋은 UX 아이디어
- 코드 스타일 선호
- 지금 당장 필요하지 않은 개선

**리뷰 문구 예시**

- `[Could] 애니메이션을 추가하면 경험이 더 좋아질 수 있어요.`
- `[Could] 이 부분은 이런 방식도 가능합니다.`

👉 **MVP/일정에 영향 주지 않음**

---

⚪ **Won’t (이번에는 하지 않음) - (선택)**

> 이번 범위에서는 명확히 제외
> 

**기준**

- 아이디어는 좋지만 MVP 범위 초과
- 기술적 리스크/비용이 큼
- 다음 단계에서 검토 예정

**리뷰 문구 예시**

- `[Won’t] 좋은 아이디어지만 MVP 범위를 벗어나서 제외합니다.`
- `[Won’t] 확장 단계에서 다시 논의하면 좋겠습니다.`

👉 **기록은 남기되, 지금 논의 종료**

---

### 리뷰 코멘트 작성 규칙 (중요)

**1️⃣ 반드시 태그부터 붙인다**

```
[Must] / [Should] / [Could] / [Won’t]
```

**2️⃣ “의견”인지 “문제”인지 명확히**

- ❌ “이건 별로인 것 같아요”
- ⭕ “[Should] 가독성 측면에서 이 방식이 더 명확해 보입니다.”

**3️⃣ Must에는 이유를 반드시 포함**

- 왜 Must인지
- 어떤 문제가 발생하는지

---

### PR 리뷰 예시 (실전)

```
[Must] 에러 발생 시 로딩 상태가 해제되지 않아 화면이 멈춥니다.
사용자가 다음 행동을 할 수 없으므로 수정이 필요합니다.

[Should] 이 로직은 공통 훅으로 분리하면 다른 화면에서도 재사용 가능할 것 같습니다.

[Could] 빈 상태 화면에 안내 문구를 추가하면 UX가 더 좋아질 것 같아요.

[Won’t] 실시간 스트리밍 방식은 MVP 범위를 벗어나 이번에는 제외합니다.
```

## 4. 폴더 구조

```
src
├─ app: 페이지 및 라우팅
│  ├─ (routes)
│  ├─ apis
│  ├─ layout.tsx
│  ├─ globals.css
│  └─ not-found.tsx
│
├─ components: 컴포넌트
│  └─ shared
│
├─ hooks: 훅
│  ├─ queries: React Query useQuery
│  └─ mutations: React Query useMutation
│
├─ stores: 상태 관리 스토어
│
├─ providers: 전역 Provider
│
├─ lib: 라이브러리와 관련된 파일
│
├─ utils: 공통 유틸
│
└─ types: 전역 타입
```

- 공통 로직은 기본적으로 **3회 이상 사용 시 분리**한다.
- 단, 정책·규칙 성격이 명확하거나 영향 범위가 큰 로직은 **2회 사용 시에도 공통화할 수 있다.**
- 파일 내 하나의 함수 존재 시 `export default` 사용
    - 예시
        
        ### 1. `export` (Named Export)
        
        **정의**
        
        ```tsx
        // math.ts
        export function add(a:number,b:number) {
        return a + b
        }
        
        export const PI =3.14
        
        ```
        
        **사용**
        
        ```tsx
        // app.ts
        import { add,PI }from'./math'
        
        add(1,2)
        ```
        
        ### 2. `export default` (Default Export)
        
        **정의**
        
        ```tsx
        // math.ts
        exportdefaultfunctionadd(a:number,b:number) {
        return a + b
        }
        ```
        
        **사용**
        
        ```tsx
        // app.ts
        import addfrom'./math'
        
        add(1,2)
        ```
        

## 5. 기술 스택

- 언어: TypeScript
- 라이브러리&프레임워크: React, Next.js
- 상태 관리: TanStack Query, Zustand
- CSS: TailwindCSS, shadcn/ui
- 기타: Zod, Husky