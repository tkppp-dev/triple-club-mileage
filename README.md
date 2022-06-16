# triple-club-mileage

# Schema
 - mileage: 사용자의 마일리지 정보를 저장하기 위한 테이블
 - mileage-log: 마일리지 증감이 있을 때마다 이력을 남기기 위한 테이블

## mileage schema
### column
 - id: UUID
 - point: int
   - 제약조건: not null, default 0
 - user_id: UUID
   - 제약조건: unique, not null
   - 외래키(from user.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현

## mileage-log
### column
 - id: UUID
 - user_id: UUID
   - 제약조건: not null
   - 외래키(from user.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현
 - place_id: UUID
   - 제약조건: not null
   - 외래키(from place.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현
 - action: varchar(Enum)
   - 리뷰가 생성, 수정, 삭제되었는지를 나타냄
 - status: varchar(Enum)
   - 전과 비교해 마일리지의 증감 상태를 나타냄
 - content_point: tiny_int
   - 제약조건: not null, default 0
 - image_point: tiny_int
   - 제약조건: not null, default 0
 - bonus_point: tiny_int
   - 제약조건: not null, default 0
 - created_at: timestamp
 