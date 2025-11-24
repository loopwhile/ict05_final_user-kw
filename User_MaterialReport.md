# 재료 분석 (MaterialReport) 상세 분석

이 문서는 재료 분석 페이지(`MaterialReport.tsx`)의 데이터 흐름과 백엔드 로직(`AnalyticsRepositoryImpl.java`), 그리고 관련된 데이터베이스 엔티티 스키마를 상세하게 분석합니다.

## 1. Frontend (`MaterialReport.tsx`)

재료 분석 페이지의 UI와 API 호출을 담당하는 React 컴포넌트입니다.

### 주요 기능

- **기간 설정**: 사용자는 `시작일`과 `종료일`을 선택하여 분석 기간을 지정할 수 있습니다.
- **뷰 전환**: `일별` 또는 `월별` 버튼을 통해 데이터 집계 단위를 변경할 수 있습니다.
- **데이터 조회**: 설정된 필터(기간, 뷰)에 따라 백엔드에 데이터를 요청하고 화면에 표시합니다.
- **요약 정보**: 상단에 주요 지표(KPI)를 카드로 표시합니다.
- **상세 내역**: 하단에 테이블 형태로 일별 또는 월별 재료 사용 내역을 표시합니다.
- **PDF 다운로드**: 현재 조회 조건으로 재료 분석 리포트를 PDF 파일로 다운로드합니다.

### API 호출 흐름

컴포넌트는 `api.get` (axios 인스턴스)을 사용하여 다음 4개의 백엔드 엔드포인트를 호출합니다.

#### 1. 상단 요약 카드 데이터 조회

- **엔드포인트**: `GET /api/analytics/materials/summary`
- **호출 시점**: 페이지 로드 시 `loadFirst()` 함수 내부에서 호출됩니다.
- **역할**: 상단 4개 KPI 카드(`재료 TOP5`, `재료 원가율`, `재고 부족 위험`, `유통기한 임박`)에 필요한 요약 데이터를 가져옵니다.
- **반환 타입**: `MaterialSummary`
  ```typescript
  type MaterialSummary = {
    topByUsage: MaterialTopItem[];      // 사용량 기준 TOP 5 재료
    topByCost: MaterialTopItem[];       // 원가 기준 TOP 5 재료
    currentCostRate: number;            // 현재 기간 원가율 (%)
    prevCostRate: number;               // 이전 기간 원가율 (%)
    costRateDiff: number;               // 원가율 차이 (%p)
    lowStockCount: number;              // 재고 부족 위험 재료 수
    expireSoonCount: number;            // 유통기한 임박 재료 수
  };
  ```

#### 2. 재료 분석 일별 데이터 조회

- **엔드포인트**: `GET /api/analytics/materials/day-rows`
- **호출 시점**: `viewBy`가 `DAY`일 때 `loadFirst()` (최초) 또는 `loadMore()` (더보기) 함수에서 호출됩니다.
- **파라미터**:
  - `start`: 조회 시작일 (`YYYY-MM-DD`)
  - `end`: 조회 종료일 (`YYYY-MM-DD`)
  - `size`: 페이지 크기 (한 번에 가져올 행 수)
  - `cursor`: 다음 페이지를 위한 커서 값 (최초 로드 시 `null`)
- **역할**: 일별 재료 사용 내역 테이블 데이터를 가져옵니다.
- **반환 타입**: `PageResp<MaterialDailyRow>`

#### 3. 재료 분석 월별 데이터 조회

- **엔드포인트**: `GET /api/analytics/materials/month-rows`
- **호출 시점**: `viewBy`가 `MONTH`일 때 `loadFirst()` 또는 `loadMore()` 함수에서 호출됩니다.
- **파라미터**: `day-rows`와 동일합니다.
- **역할**: 월별 재료 사용 내역 테이블 데이터를 가져옵니다.
- **반환 타입**: `PageResp<MaterialMonthlyRow>`

#### 4. PDF 리포트 다운로드

- **엔드포인트**: `GET /api/analytics/materials/report`
- **호출 시점**: `리포트 다운로드` 버튼 클릭 시 `handleDownloadReport()` 함수에서 호출됩니다.
- **파라미터**:
  - `start`: 조회 시작일 (`YYYY-MM-DD`)
  - `end`: 조회 종료일 (`YYYY-MM-DD`)
  - `viewBy`: `DAY` 또는 `MONTH`
- **역할**: 현재 조회 조건에 맞는 분석 리포트를 PDF 형식의 Blob 데이터로 받아 다운로드합니다.

---

## 2. Backend (`AnalyticsRepositoryImpl.java`)

프론트엔드에서 호출된 API에 대한 실제 데이터베이스 조회를 담당하는 QueryDSL 구현체입니다.

### 핵심 Q-Type (엔티티)

재료 분석 쿼리에서 주로 사용되는 Q-Type과 해당 엔티티는 다음과 같습니다.

| Q-Type 변수 | 엔티티 클래스               | 테이블명                    | 주요 역할                                                    |
| :------------ | :-------------------------- | :-------------------------- | :----------------------------------------------------------- |
| `co`          | `CustomerOrder`             | `customer_order`            | 주문 필터링 (완료 상태, 기간), 매출액(`totalPrice`) 집계     |
| `log`         | `MenuUsageMaterialLog`      | `menu_usage_material_log`   | **재료 분석의 핵심**. 재료 사용량(`count`)의 원천 데이터.    |
| `sm`          | `StoreMaterial`             | `store_material`            | **재료 마스터**. 재료명, 단위, 매입가, 변환비율 등 정보 제공. |
| `material`    | `Material`                  | `material`                  | `StoreMaterial`의 이름이 없을 경우 대체 이름 제공 (본사 재료) |
| `inv`         | `StoreInventory`            | `store_inventory`           | 재고 부족 상태(`status`) 확인                                |
| `batch`       | `StoreInventoryBatch`       | `store_inventory_batch`     | 유통기한(`expirationDate`), 최근 입고일(`receivedDate`) 확인 |

### 주요 메서드 분석

#### 1. `fetchMaterialSummary(Long storeId, LocalDate today)`

- **연결된 API**: `GET /api/analytics/materials/summary`
- **동작**:
  1.  **Top 5 재료 조회**: `findMaterialTopByUsage`와 `findMaterialTopByCost`를 호출하여 이번 달 1일부터 어제까지(MTD)의 사용량/원가 기준 Top 5 재료를 조회합니다.
  2.  **원가율 계산**:
      - `fetchMaterialCostTotal`을 호출하여 현재 기간(MTD)과 이전 월 동기간의 총재료비를 각각 구합니다.
      - `fetchSalesTotal`을 호출하여 각 기간의 총매출을 구합니다.
      - `원가율 = (총재료비 / 총매출) * 100` 공식을 사용하여 `currentCostRate`와 `prevCostRate`를 계산합니다.
  3.  **재고 위험 조회**:
      - `fetchLowStockCount`: `StoreInventory` 테이블에서 `status`가 `LOW` 또는 `SHORTAGE`인 재료의 수를 셉니다.
      - `fetchExpireSoonCount`: `StoreInventoryBatch` 테이블에서 유통기한(`expirationDate`)이 오늘부터 `EXPIRE_SOON_DAYS` (상수, 3일) 이내인 재료의 수를 셉니다.

#### 2. `fetchMaterialDailyRows(Long storeId, AnalyticsSearchDto cond)`

- **연결된 API**: `GET /api/analytics/materials/day-rows`
- **동작**:
  - **GROUP BY**: `co.orderedAt`의 날짜(`YYYY-MM-DD`)와 `sm.id` (점포 재료 ID)를 기준으로 그룹화합니다.
  - **주요 집계 로직**:
    - **`useDate`**: `DATE_FORMAT(co.orderedAt, '%Y-%m-%d')`를 사용하여 날짜 문자열을 생성합니다.
    - **`materialName`**: `IFNULL(sm.name, material.name)`을 사용하여 `StoreMaterial`의 이름을 우선 사용하고, 없으면 `Material`의 이름을 사용합니다.
    - **`usedQuantity`**: `SUM(log.count)`를 통해 재료 사용량을 집계합니다. `log.count`는 메뉴가 판매될 때 소진된 재료의 양입니다.
    - **`unitName`**: `sm.baseUnit` (소진 단위)을 사용합니다.
    - **`cost`**: `materialCostSumExpr()` 헬퍼 메서드를 호출하여 계산합니다.
    - **`salesShare`**: Java 코드에서 계산됩니다. `fetchSalesByDayForMaterials`로 일별 총매출 맵을 미리 가져온 뒤, `(일별 재료 원가 / 일별 총매출) * 100`으로 계산합니다.
    - **`lastInboundDate`**: `fetchLastInboundDateByStoreMaterial`로 재료별 최근 입고일 맵을 미리 가져와 매핑합니다.
  - **커서**: `YYYY-MM-DD|storeMaterialId` 형식의 문자열을 사용하여 페이징을 처리합니다.

#### 3. `fetchMaterialMonthlyRows(Long storeId, AnalyticsSearchDto cond)`

- **연결된 API**: `GET /api/analytics/materials/month-rows`
- **동작**: `fetchMaterialDailyRows`와 유사하지만, 집계 기준이 다릅니다.
  - **GROUP BY**: `co.orderedAt`의 연월(`YYYY-MM`)과 `sm.id`를 기준으로 그룹화합니다.
  - **주요 집계 로직**:
    - **`yearMonth`**: `DATE_FORMAT(co.orderedAt, '%Y-%m')`를 사용합니다.
    - **`costRate`**: Java 코드에서 계산됩니다. `fetchSalesByMonthForMaterials`로 월별 총매출 맵을 가져온 뒤, `(월별 재료 원가 / 월별 총매출) * 100`으로 계산합니다.
    - **`lastInboundMonth`**: `fetchLastInboundDateByStoreMaterial`의 결과에서 연월(`YYYY-MM`) 부분만 추출하여 사용합니다.

#### 4. 핵심 헬퍼 메서드: `materialCostSumExpr()`

- **역할**: 재료 원가 계산의 핵심 로직을 정의하는 재사용 가능한 QueryDSL 표현식입니다.
- **계산 공식**: `SUM( (log.count / sm.conversionRate) * sm.purchasePrice )`
- **상세 설명**:
  - `log.count`: 메뉴 판매로 인해 소진된 재료의 양 (소진 단위 기준).
  - `sm.conversionRate`: 입고 단위를 소진 단위로 변환하는 비율 (예: 1박스 = 1000g일 때 1000).
  - `sm.purchasePrice`: 입고 단위 기준의 최근 매입 단가.
  - `log.count / sm.conversionRate`: 소진된 양을 입고 단위로 환산합니다.
  - `* sm.purchasePrice`: 환산된 입고 단위 수량에 단가를 곱하여 원가를 계산합니다.
- **안전장치**: `conversionRate`가 0 또는 NULL이거나 `purchasePrice`가 NULL일 경우를 대비하여 `COALESCE`, `NULLIF`를 사용하여 SQL 레벨에서 오류를 방지합니다.

---

## 3. 관련 엔티티(Entity) 상세 스키마

쿼리에 사용된 주요 엔티티와 컬럼 정보입니다.

### 1. `MenuUsageMaterialLog` (menu_usage_material_log)

| 컬럼명                             | Java 필드         | 타입           | 설명                                   |
| :--------------------------------- | :---------------- | :------------- | :------------------------------------- |
| `menu_usage_material_log_id`       | `id`              | `Long`         | PK                                     |
| `customer_order_id_fk`             | `customerOrderFk` | `CustomerOrder`| 어떤 주문에서 발생했는지 (FK)          |
| `menu_id_fk`                       | `menuFk`          | `Menu`         | 어떤 메뉴 때문인지 (FK)                |
| `store_material_id_fk`             | `storeMaterialFk` | `StoreMaterial`| 어떤 재료가 소진됐는지 (FK)            |
| `menu_usage_material_log_count`    | `count`           | `BigDecimal`   | **소진된 재료의 양 (소진 단위 기준)**  |
| `menu_usage_material_log_unit`     | `unit`            | `String`       | 소진 단위 (예: g, 개)                  |
| `menu_usage_material_log_date`     | `logDate`         | `LocalDateTime`| 기록 일시                              |

### 2. `StoreMaterial` (store_material)

| 컬럼명                              | Java 필드         | 타입          | 설명                                                         |
| :---------------------------------- | :---------------- | :------------ | :----------------------------------------------------------- |
| `store_material_id`                 | `id`              | `Long`        | PK                                                           |
| `store_id_fk`                       | `store`           | `Store`       | 소속 매장 (FK)                                               |
| `material_id_fk`                    | `material`        | `Material`    | 본사 재료인 경우 연결 (FK)                                   |
| `store_material_name`               | `name`            | `String`      | 재료명                                                       |
| `store_material_base_unit`          | `baseUnit`        | `String`      | **소진 단위** (예: g, ml, 개)                                |
| `store_material_sales_unit`         | `salesUnit`       | `String`      | **입고 단위** (예: Box, kg)                                  |
| `material_conversion_rate`          | `conversionRate`  | `Integer`     | **변환 비율** (입고단위 1개당 소진단위 수량, 예: 1000)       |
| `store_material_purchase_price`     | `purchasePrice`   | `BigDecimal`  | **최근 매입 단가** (입고 단위 기준)                          |
| `store_material_optimal_quantity`   | `optimalQuantity` | `BigDecimal`  | 적정 재고 수량                                               |
| `store_material_status`             | `status`          | `Enum`        | 재료 사용 상태 (USE/STOP)                                    |

### 3. `StoreInventory` (store_inventory) & `InventoryBase` (상속)

| 컬럼명                     | Java 필드         | 타입         | 설명                               |
| :------------------------- | :---------------- | :----------- | :--------------------------------- |
| `store_inventory_id`       | `id`              | `Long`       | PK                                 |
| `store_material_id_fk`     | `storeMaterial`   | `StoreMaterial`| 어떤 재료의 재고인지 (FK)          |
| `inventory_quantity`       | `quantity`        | `BigDecimal` | 현재 재고 수량                     |
| `inventory_optimal_quantity` | `optimalQuantity` | `BigDecimal` | 적정 재고 수량                     |
| `inventory_status`         | `status`          | `Enum`       | **재고 상태** (SUFFICIENT/LOW/SHORTAGE) |
| `inventory_update_date`    | `updateDate`      | `LocalDateTime`| 마지막 업데이트 일시               |

### 4. `StoreInventoryBatch` (store_inventory_batch)

| 컬럼명                                | Java 필드        | 타입            | 설명                          |
| :------------------------------------ | :--------------- | :-------------- | :---------------------------- |
| `store_inventory_batch_id`            | `id`             | `Long`          | PK                            |
| `store_inventory_id_fk`               | `storeInventory` | `StoreInventory`| 어떤 재고에 속하는지 (FK)     |
| `store_inventory_batch_received_date` | `receivedDate`   | `LocalDateTime` | **입고 일시**                 |
| `store_inventory_batch_expiration_date` | `expirationDate` | `LocalDate`     | **유통기한**                  |
| `store_inventory_batch_quantity`      | `quantity`       | `BigDecimal`    | 해당 배치의 현재 잔량         |

### 5. `CustomerOrder` (customer_order)

| 컬럼명                         | Java 필드     | 타입          | 설명                          |
| :----------------------------- | :------------ | :------------ | :---------------------------- |
| `customer_order_id`            | `id`          | `Long`        | PK                            |
| `store_id_fk`                  | `store`       | `Store`       | 주문이 발생한 매장 (FK)       |
| `customer_order_status`        | `status`      | `Enum`        | 주문 상태 (COMPLETED만 분석)  |
| `customer_order_total_price`   | `totalPrice`  | `BigDecimal`  | **주문 총매출액**             |
| `customer_order_date`          | `orderedAt`   | `LocalDateTime` | **주문 발생 일시** (기간 필터링 기준) |

---

## 4. 전체 도메인 엔티티 분석

### 4.1 `domain/inventory` 패키지

재고 및 원자재 관리를 위한 핵심 엔티티들이 포함되어 있습니다.

#### `Material`
본사에서 관리하는 원자재의 마스터 정보입니다. 모든 재료의 기준이 됩니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `material_id` | `id` | `Long` | PK |
| `material_code` | `code` | `String` | 본사 재료 코드 (고유) |
| `material_name` | `name` | `String` | 재료명 |
| `material_category` | `materialCategory` | `Enum` | 재료 카테고리 (BASE, SIDE 등) |
| `material_base_unit` | `baseUnit` | `String` | 기본 단위 (소진 단위) |
| `material_sales_unit` | `salesUnit` | `String` | 판매 단위 (입고 단위) |
| `material_conversion_rate` | `conversionRate` | `Integer` | 판매단위 → 기본단위 변환 비율 |
| `material_supplier` | `supplier` | `String` | 대표 공급업체명 |
| `material_temperature` | `materialTemperature` | `Enum` | 보관 온도 (상온, 냉장, 냉동) |
| `material_status` | `materialStatus` | `Enum` | 재료 상태 (USE/STOP) |

#### `StoreMaterial`
`Material`을 각 가맹점에서 어떻게 사용하는지에 대한 상세 정보입니다. 가맹점별로 다른 단가, 다른 이름을 가질 수 있습니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `store_material_id` | `id` | `Long` | PK |
| `store_id_fk` | `store` | `Store` | 가맹점 (FK) |
| `material_id_fk` | `material` | `Material` | 본사 재료 (FK, 가맹점 자체 재료는 NULL) |
| `store_material_code` | `code` | `String` | 가맹점별 재료 코드 (가맹점 내 고유) |
| `store_material_name` | `name` | `String` | 가맹점에서의 재료명 |
| `store_material_base_unit` | `baseUnit` | `String` | 소진 단위 (분석 시 사용량의 단위) |
| `material_conversion_rate` | `conversionRate` | `Integer` | 변환 비율 (원가 계산 시 사용) |
| `store_material_purchase_price` | `purchasePrice` | `BigDecimal` | 최근 매입 단가 (원가 계산 시 사용) |

#### `StoreInventory` & `InventoryBase`
`StoreInventory`는 특정 가맹점의 특정 `StoreMaterial`에 대한 현재 재고 정보를 나타냅니다. `InventoryBase`는 재고의 공통 속성(현재수량, 적정수량, 상태)을 정의하는 상위 클래스입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `store_inventory_id` | `id` | `Long` | PK (`StoreInventory`) |
| `store_id_fk` | `store` | `Store` | 가맹점 (FK) |
| `store_material_id_fk` | `storeMaterial` | `StoreMaterial` | 어떤 재료의 재고인지 (FK, OneToOne) |
| `inventory_quantity` | `quantity` | `BigDecimal` | 현재 재고 수량 (소진 단위 기준) |
| `inventory_optimal_quantity` | `optimalQuantity` | `BigDecimal` | 가맹점 설정 적정 재고 수량 |
| `inventory_status` | `status` | `Enum` | 재고 상태 (SUFFICIENT, LOW, SHORTAGE) |
| `inventory_update_date` | `updateDate` | `LocalDateTime` | 마지막 재고 변경일 |

#### `StoreInventoryBatch`
개별 입고 건(LOT)을 관리하여 유통기한 추적(선입선출)을 가능하게 합니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `store_inventory_batch_id` | `id` | `Long` | PK |
| `store_inventory_id_fk` | `storeInventory` | `StoreInventory` | 상위 재고 정보 (FK) |
| `store_material_id_fk` | `storeMaterial` | `StoreMaterial` | 재료 정보 (FK) |
| `store_inventory_batch_quantity` | `quantity` | `BigDecimal` | 해당 배치의 현재 남은 수량 |
| `store_inventory_batch_received_date` | `receivedDate` | `LocalDateTime` | **입고 일시** (최근 입고일 분석에 사용) |
| `store_inventory_batch_expiration_date` | `expirationDate` | `LocalDate` | **유통기한** (유통기한 임박 분석에 사용) |

#### `StoreInventoryIn` / `StoreInventoryOut` / `StoreInventoryAdjustment`
재고의 모든 변동(입고, 출고, 조정)을 기록하는 이력 테이블입니다.
- `StoreInventoryIn`: 입고 기록 (수량: `+`)
- `StoreInventoryOut`: 출고/소진 기록 (수량: `+` 이지만 의미상 재고 감소)
- `StoreInventoryAdjustment`: 재고 실사 등으로 인한 수량 조정 기록 (차이: `difference`)

#### `StoreInventoryLogView`
입고/출고/조정 이력을 하나의 타임라인으로 보여주는 읽기 전용 DB 뷰입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `row_id` | `rowId` | `Long` | 뷰를 위한 가상 PK |
| `log_id` | `logId` | `Long` | 원본 이벤트 테이블의 PK |
| `log_date` | `date` | `LocalDateTime` | 이벤트 발생 시각 |
| `log_type` | `type` | `String` | 이벤트 타입 (INCOME, OUTGO, ADJUST) |
| `quantity` | `quantity` | `BigDecimal` | 변경 수량 (+/-) |
| `stock_after` | `stockAfter` | `BigDecimal` | 변경 후 재고 수량 |

#### `UnitPrice` / `StoreUnitPrice`
본사 및 가맹점의 재료 단가 변경 이력을 관리합니다.
- `UnitPrice`: 본사 기준 (`material_id_fk`, 매입가/공급가)
- `StoreUnitPrice`: 가맹점 기준 (`store_material_id_fk`, 매입가/판매가)

---

### 4.2 `domain/purchaseOrder` 패키지

가맹점의 발주 요청을 관리합니다.

#### `PurchaseOrder`
발주 한 건에 대한 헤더 정보입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `purchase_order_id` | `id` | `Long` | PK |
| `store_id_fk` | `store` | `Store` | 발주한 가맹점 (FK) |
| `purchase_order_code` | `orderCode` | `String` | 발주 코드 (고유) |
| `purchase_order_date` | `orderDate` | `LocalDate` | 발주 요청일 |
| `purchase_order_total_price` | `totalPrice` | `BigDecimal` | 발주 총액 |
| `purchase_order_status` | `status` | `Enum` | 발주 상태 (PENDING, RECEIVED 등) |

#### `PurchaseOrderDetail`
발주에 포함된 개별 품목 정보입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `purchase_order_detail_id` | `id` | `Long` | PK |
| `purchase_order_id_fk` | `purchaseOrder` | `PurchaseOrder` | 상위 발주 헤더 (FK) |
| `material_id_fk` | `material` | `StoreMaterial` | 발주한 재료 (FK) |
| `purchase_order_detail_count` | `count` | `Integer` | 발주 수량 |
| `purchase_order_detail_unit_price` | `unitPrice` | `BigDecimal` | 발주 시점의 단가 |
| `purchase_order_detail_total_price` | `totalPrice` | `BigDecimal` | 품목 총액 |

---

### 4.3 `domain/menu` 패키지

판매 메뉴와 레시피 정보를 관리합니다.

#### `Menu`
본사에서 관리하는 메뉴 마스터 정보입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `menu_id` | `menuId` | `Long` | PK |
| `menu_name` | `menuName` | `String` | 메뉴명 |
| `menu_price` | `menuPrice` | `BigDecimal` | 메뉴 판매 가격 |
| `menu_category_id_fk` | `menuCategory` | `MenuCategory` | 메뉴 카테고리 (FK) |
| `menu_show` | `menuShow` | `Enum` | 판매 상태 (SHOW/HIDE) |

#### `MenuCategory`
메뉴를 그룹화하는 카테고리 정보입니다. 대/중/소 분류가 가능합니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `menu_category_id` | `menuCategoryId` | `Long` | PK |
| `menu_category_parent_id` | `menuCategoryParentId` | `MenuCategory` | 상위 카테고리 (Self-Join) |
| `menu_category_name` | `menuCategoryName` | `String` | 카테고리명 |
| `menu_category_level` | `menuCategoryLevel` | `Short` | 카테고리 레벨 (1, 2, 3) |

#### `MenuRecipe`
하나의 메뉴를 만들기 위해 어떤 재료가 얼마나 필요한지 정의합니다. 메뉴와 재료를 연결하는 중요한 엔티티입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `menu_recipe_id` | `menuRecipeId` | `Long` | PK |
| `menu_id_fk` | `menu` | `Menu` | 레시피가 속한 메뉴 (FK) |
| `material_id_fk` | `material` | `Material` | 사용되는 본사 재료 (FK) |
| `recipe_qty` | `recipeQty` | `BigDecimal` | 필요한 재료의 양 |
| `recipe_unit` | `recipeUnit` | `Enum` | 재료 양의 단위 (g, ml, 개 등) |

#### `StoreMenu`
가맹점에서 특정 메뉴를 판매할지, 품절 처리할지를 관리합니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `store_menu_id` | `storeMenuId` | `Long` | PK |
| `menu_id_fk` | `menu` | `Menu` | 메뉴 (FK) |
| `store_id_fk` | `store` | `Store` | 가맹점 (FK) |
| `store_menu_soldout` | `storeMenuSoldout` | `Enum` | 가맹점 내 품절 상태 (ON_SALE/SOLD_OUT) |

#### `MenuUsageMaterialLog`
**재료 분석의 가장 핵심적인 데이터 소스**입니다. 고객 주문이 발생했을 때, 어떤 메뉴로 인해 어떤 재료가 얼마나 소진되었는지를 기록합니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `menu_usage_material_log_id` | `id` | `Long` | PK |
| `customer_order_id_fk` | `customerOrderFk` | `CustomerOrder` | 관련 고객 주문 (FK) |
| `menu_id_fk` | `menuFk` | `Menu` | 판매된 메뉴 (FK) |
| `store_material_id_fk` | `storeMaterialFk` | `StoreMaterial` | **소진된 가맹점 재료 (FK)** |
| `menu_usage_material_log_count` | `count` | `BigDecimal` | **소진된 수량 (소진 단위 기준)** |

---

### 4.4 `domain/order` 패키지

고객의 주문 정보를 관리합니다.

#### `CustomerOrder`
고객 주문 한 건에 대한 헤더 정보입니다. 재료 분석에서는 이 테이블을 통해 주문 시간과 완료 상태를 필터링합니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `customer_order_id` | `id` | `Long` | PK |
| `store_id_fk` | `store` | `Store` | 주문이 발생한 가맹점 (FK) |
| `customer_order_status` | `status` | `Enum` | 주문 상태 (분석 시 `COMPLETED`만 사용) |
| `customer_order_total_price` | `totalPrice` | `BigDecimal` | 주문 총액 (원가율 계산 시 분모) |
| `customer_order_date` | `orderedAt` | `LocalDateTime` | 주문 일시 (기간 필터링 기준) |
| `customer_order_type` | `orderType` | `Enum` | 주문 유형 (VISIT, TAKEOUT, DELIVERY) |

#### `CustomerOrderDetail`
주문에 포함된 개별 메뉴 항목입니다.
| 컬럼명 | Java 필드 | 타입 | 설명 |
|---|---|---|---|
| `customer_order_detail_id` | `id` | `Long` | PK |
| `customer_order_id_fk` | `order` | `CustomerOrder` | 상위 주문 헤더 (FK) |
| `menu_id_fk` | `menuIdFk` | `Menu` | 주문된 메뉴 (FK) |
| `customer_order_detail_quantity` | `quantity` | `Integer` | 주문 수량 |
| `customer_order_detail_total` | `lineTotal` | `BigDecimal` | 해당 메뉴 항목의 총액 |