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
로컬에서 MySQL Server를 사용했기 때문에 실행전 application.yml 수정 필요

``` yaml
spring:
   datasource:
      driver-class-name: com.mysql.cj.jdbc.Driver        # mysql 8 미만일 시 com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost:3306/triple_mileage    # 필요시 수정
      username: <username>
      password: <password>
```

``` bash
$ git clone https://github.com/tkppp-dev/triple-club-mileage.git
$ cd triple-club-mileage
$ ./gradlew build
$ java -jar './build/libs/triple-club-mileage-0.0.1.jar'
```

# API 명세
http://localhost:8080/swagger-ui/index.html

# Schema
 - mileage: 사용자의 마일리지 정보를 저장하기 위한 테이블
 - mileage-log: 마일리지 증감이 있을 때마다 이력을 남기기 위한 테이블

## DDL
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
  `variation` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `ml_i_userid` (`user_id`),
  KEY `ml_i_placeid` (`place_id`),
  KEY `m1_i_reviewid` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
```

## mileage
### column
 - id: UUID
 - point: int
   - 제약조건: not null, default 0
 - user_id: UUID
   - 제약조건: unique, not null
   - 외래키(from user.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현
   - index

## mileage_log
### column
 - id: UUID
 - user_id: UUID
   - 제약조건: not null
   - 외래키(from user.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현
   - index
 - place_id: UUID
   - 제약조건: not null
   - 외래키(from place.id)이어야하나 프로젝트 특성상 일반 컬럼으로 구현
   - index
 - review_id: UUID
   - 제약조건: not null
   - 한 장소에 여러개의 리뷰를 남길 수 있기 때문에 이를 유저마다 구분해야하기에 필요
   - index
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
 