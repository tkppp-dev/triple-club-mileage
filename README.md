# triple-club-mileage

# 기술 스택
 - Kotlin
 - SpringBoot
 - Spring Data Jpa
 - MockK

# 개발환경
 - MacOS BigSur
 - Intellij Ultimate
 - Spring Boot 2.7
 - MySQL 8.0.28(Local)

# 실행 방법
## 실행 명령어
``` bash
$ git clone https://github.com/tkppp-dev/triple-club-mileage.git
$ cd triple-club-mileage
$ ./gradlew build
$ java -jar './build/libs/triple-club-mileage-0.0.1.jar'
```

## 유의사항
로컬에서 MySQL Server를 사용했기 때문에 실행전 application.yml 수정 필요  
``` yaml
spring:
   datasource:
      driver-class-name: com.mysql.cj.jdbc.Driver        # mysql 8 미만일 시 com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost:3306/triple_mileage    # 필요시 수정
      username: <username>
      password: <password>
```
ddl-auto = none 으로 설정되어 있기때문에 빌드 전 아래 DDL로 데이터베이스 생성 및 테이블 생성 필요

``` mysql
DROP DATABASE IF EXISTS `triple_mileage`;
CREATE SCHEMA `triple_mileage` DEFAULT CHARACTER SET utf8 ;
USE `triple_mileage`;

DROP TABLE IF EXISTS `mileage`;
CREATE TABLE `mileage` (
  `id` binary(16) NOT NULL,
  `point` int NOT NULL,
  `user_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_s6nuljl2xxw684h2ms89lulw4` (`user_id`),
  KEY `m_i_userid` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

DROP TABLE IF EXISTS `mileage_log`;
CREATE TABLE `mileage_log` (
  `id` binary(16) NOT NULL,
  `action` varchar(255) NOT NULL,
  `bonus_point` tinyint NOT NULL DEFAULT '0',
  `content_point` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL,
  `image_point` tinyint NOT NULL DEFAULT '0',
  `place_id` binary(16) NOT NULL,
  `review_id` binary(16) NOT NULL,
  `status` varchar(255) NOT NULL,
  `user_id` binary(16) NOT NULL,
  `variation` tinyint NOT NULL DEAULT '0',
  PRIMARY KEY (`id`),
  KEY `ml_i_userid` (`user_id`),
  KEY `ml_i_placeid` (`place_id`),
  KEY `m1_i_reviewid` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
```

# API 명세
**POST** /events : 리뷰 작성, 수정, 삭제 이벤트 발생시 마일리지 포인트 부여  
**GET** /api/mileage/{userId} : 유저의 현재 포인트 정보 반환  
**GET** /api/mileage/all/{userId} : 유저의 포인트 증감 이력 반환  

자세한 사항은 프로젝트 실행 후 Swagger 문서 확인  
http://localhost:8080/swagger-ui/index.html

# Schema
 - mileage: 사용자의 마일리지 정보를 저장하기 위한 테이블
 - mileage-log: 마일리지 증감이 있을 때마다 이력을 남기기 위한 테이블

## mileage
### column
 - id: UUID
 - point: int
   - 제약조건: not null, default 0
 - user_id: UUID
   - 제약조건: unique, not null
   - index

## mileage_log
### column
 - id: UUID
 - user_id: UUID
   - 제약조건: not null
   - index
 - place_id: UUID
   - 제약조건: not null
   - index
 - review_id: UUID
   - 제약조건: not null
   - index
 - action: varchar(Enum)
   - 제약조건: not null
   - 리뷰가 생성, 수정, 삭제되었는지를 나타냄
 - status: varchar(Enum)
   - 제약조건: not null
   - 전과 비교해 마일리지의 증감 상태를 나타냄
 - content_point: tiny_int
   - 제약조건: not null, default 0
 - image_point: tiny_int
   - 제약조건: not null, default 0
 - bonus_point: tiny_int
   - 제약조건: not null, default 0
 - created_at: timestamp
 
