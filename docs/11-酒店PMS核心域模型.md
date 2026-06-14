# 11 · 酒店 PMS 核心域模型

> 范围：功能清单第 9 节「酒店 PMS」（ADR-007 自建完整 PMS / ADR-016 门锁对接二期留智能客控 / ADR-020 基础会议室预订）。
> 部署形态：延续 07 文档的「模块化单体」原则，本域新增 **hotel-pms-service** 一个 Maven 模块（房态/预订/前台账务/夜审/客房部/会议室/协议单位），内部按聚合根分包；**单一客史**作为 member-service 的扩展不新增模块；**门锁对接**作为硬件接入平台（第 13 节）的设备适配层不新增模块。
> 与门票链路（07/08）的关系：按 ADR-025，房晚库存与会议室场次库存在 hotel-pms-service 内**各自独立建表**（`room_inventory_bucket` / `meeting_room_inventory_bucket`），字段范式与状态机复用 07 文档 `inventory_bucket`/`InventoryReservation` 的设计（通过共享 `inventory-kernel` 代码库），但不共用门票域的表与 inventory-service API；跨商户购物车与统一下单仍走 order-service；hotel-pms-service 在酒店域内扮演类似 07 文档中 order-service 的「编排者」角色。

---

## 一、房型与房晚库存（独立建表 + 复用防超卖设计模式，新增「多夜原子预占」）

### 1.1 房型：RoomType（商品中心 SKU，对应 07 文档的 TicketSku）

| 字段 | 类型 | 说明 |
|---|---|---|
| `sku_id` | bigint (PK) | 与门票 SKU 同一序列，库存桶以此为关联键 |
| `tenant_id` / `scenic_id` / `hotel_id`(=`merchant_id`) | bigint | 酒店即「商户」，是分账与对账的最小单位（四级租户模型） |
| `room_type_name` | varchar | 大床房/双床房/套房等 |
| `max_occupancy` | int | 最大入住人数 |
| `bed_config` | json | 床型配置 |
| `min_stay` / `max_stay` | int | 连住规则（最少/最多连住晚数，0=不限） |
| `breakfast_policy` | enum | `NONE` / `SINGLE` / `DOUBLE`（含早政策，随 RatePlan 可覆盖） |
| `cancellation_policy` | json | 取消政策（如「入住前 24h 免费取消，否则收首晚房费」），下单时快照到 `ReservationRoom.cancellation_policy_snapshot` |

### 1.2 房晚库存：独立表 `room_inventory_bucket`（字段范式复用 07 文档 1.1 `inventory_bucket`）

> 01-产品定位与总体架构「五种库存模型」定义「房晚库存 = 房型 + 价格日历 + 连住」。按 ADR-025，hotel-pms-service 内新建 `room_inventory_bucket` 表，字段范式与 07 文档 `inventory_bucket` 一致、防超卖两道防线与预占状态机复用同一套 `inventory-kernel` 实现，但**表与服务边界独立**，不与门票域共表/共库：

| 字段 | 门票 `inventory_bucket` 对应字段 | 酒店场景含义 |
|---|---|---|
| `sku_id` | `sku_id` | 房型 SKU（即 `RoomType.sku_id`） |
| `sale_date` | `sale_date` | **入住日（stay_date）**，一晚=一个 bucket |
| `time_slot_id` | `time_slot_id` | 本域不使用，字段不引入（与门票表的差异点之一，体现"独立建表可按需精简字段"） |
| `total_quota` | `total_quota` | 该房型当晚总房量（=该房型 `RoomUnit` 总数，扣减 `OOO`/`OOS` 后的「可售房量」由定时任务同步到 `total_quota`） |
| `sold_count` / `locked_count` | `sold_count` / `locked_count` | 已确认间夜数 / 预占间夜数 |
| `version` | `version` | 乐观锁，条件更新复用 ADR-018 两道防线的 SQL 范式 |

对应的预占记录表 `room_inventory_reservation`（字段范式同 07 文档 1.2 `InventoryReservation`：`reservation_id`/`bucket_id`/`quantity`/`status`/`hold_expire_at`）同样独立建表。**一次预订需要对「连续多晚」分别预占**，详见 1.3。

### 1.3 核心难点：连住的「多夜原子预占」

一张门票订单项只对应**单一日期**的一个库存桶，预占是单桶操作。酒店预订项 `ReservationRoom` 对应 `[check_in_date, check_out_date)` 区间内的 **N 个连续夜晚 = N 个不同 `room_inventory_bucket` 行**（同房型、不同 `sale_date`），必须保证：

- **要么 N 晚全部预占成功，要么全部不生效**（否则会出现「订到一半」的房）。
- N 个桶很可能分布在不同分库分表 shard（按 `sku_id` 或 `bucket_id` 分片），无法用单库事务覆盖。

**方案：Saga 式分步预占 + 补偿释放**（hotel-pms-service 内部编排，调用 `inventory-kernel` 提供的 `CreateReservation`/`ConfirmReservation`/`ReleaseReservation` 函数，语义与 07 文档 1.4 一致，但作用于 `room_inventory_bucket`/`room_inventory_reservation` 这两张本域独立表，不发起跨服务 RPC）：

```
CreateReservationRoom(reservation_room_id, room_type_sku_id, check_in, check_out, qty=1)
  │
  ├─ for each stay_date in [check_in, check_out):
  │     reservation_id := "{reservation_room_id}_{stay_date}"   // 派生幂等键，每晚一条 room_inventory_reservation
  │     CreateReservation(reservation_id, bucket(sku_id, stay_date), qty)  // 作用于 room_inventory_bucket
  │       ├─ 成功 → 记入「已预占清单」，继续下一晚
  │       └─ 失败（该晚满房） → 跳出循环，对「已预占清单」逐条调用 ReleaseReservation 回滚
  │
  └─ 全部成功 → ReservationRoom.status = HOLDING；任一失败 → 整体返回「该房型在所选日期段内有夜晚已满房」，
                  并提示 hotel-pms-service 可尝试「拆分预订」（不同夜晚不同房型，v1 不实现，留二期）
```

- **幂等性**：`reservation_id = "{reservation_room_id}_{stay_date}"` 保证重试时 `CreateReservation` 对每晚都是幂等的（07 文档 1.4 的幂等保证按此粒度复用）。
- **确认/释放同样按晚分别下发**：`ConfirmReservation` / `ReleaseReservation` 对 N 个派生 `reservation_id` 逐一调用；hotel-pms-service 在 `ReservationRoom` 上记录 `room_night_ids: string[]`（即 N 个派生 `reservation_id` 列表）用于追踪。
- **续住/提前退房**：续住 = 对新增的夜晚追加 `CreateReservation`；提前退房 = 对未消费的剩余夜晚批量 `ReleaseReservation`（受取消政策约束是否退费，与账务无关，库存照常释放）。
- **钟点房（Day-use）**：`check_in_date == check_out_date`，按业务规则固定占用「当晚」一个 bucket（即钟点房与过夜房共享同一晚的库存配额，避免引入第六种库存模型）；同一房间同一晚不可同时接钟点房与过夜房——由 1.4 的排房环节用 `RoomUnit` 占用窗口校验。

### 1.4 排房：库存桶（房型层面）→ 具体房间（RoomUnit）

库存桶只保证「某房型某晚还有多少间可售」，**不绑定具体房间号**；具体房间号由 `RoomAssignment` 在「房型库存」之上再做一层分配，详见三、3.3。

---

## 二、预订服务（hotel-pms-service / reservation 子域）

### 2.1 聚合根：Reservation（预订单）

| 字段 | 类型 | 说明 |
|---|---|---|
| `reservation_id` | bigint (PK) | 分库分表：`tenant_id` + `hotel_id` |
| `tenant_id` / `scenic_id` / `hotel_id` | bigint | 归属 |
| `order_id` | bigint, nullable | 关联 order-service 的跨商户订单（C 端/直营下单场景）；OTA/前台直接建单场景可为空，账务独立走酒店分账 |
| `guest_profile_id` | bigint | 关联六、GuestProfile（单一客史） |
| `source` | enum | `DIRECT`(直营/小程序) / `OTA` / `CORPORATE`(协议单位) / `WALK_IN`(前台散客) |
| `reservation_type` | enum | `INDIVIDUAL`(散客) / `GROUP`(团队) / `DAY_USE`(钟点房) |
| `corporate_account_id` | bigint, nullable | 协议单位（关联五、CorporateAccount），影响价格与是否可挂账 |
| `status` | enum | 见 2.4 |
| `total_amount` / `paid_amount` / `deposit_amount` | decimal | 应付/已付/押金（押金明细在 Folio） |
| `hold_expire_at` | datetime | `PENDING` 状态超时时间 |
| `version` | int | 乐观锁 |

### 2.2 实体：ReservationRoom（预订房间项）

| 字段 | 类型 | 说明 |
|---|---|---|
| `reservation_room_id` | bigint (PK) | 同时作为 1.3 派生 `reservation_id` 的前缀 |
| `reservation_id` | bigint | 所属预订单 |
| `room_type_sku_id` | bigint | 对应 1.1 房型 |
| `check_in_date` / `check_out_date` | date | 入住/离店日（钟点房二者相等） |
| `guest_count` | int | 入住人数（校验 ≤ `RoomType.max_occupancy`） |
| `rate_plan_snapshot` | json | 下单时快照：每晚单价（价格中心 `PriceCalendar`，酒店场景 `price_type` 含 `CORPORATE` 协议价）、含早政策、取消政策 |
| `room_night_ids` | json (string[]) | 1.3 中 N 个派生 `reservation_id` 列表 |
| `room_id` | bigint, nullable | 排房后绑定的具体 `RoomUnit`，见 3.3 |
| `status` | enum | `HOLDING` / `CONFIRMED` / `CHECKED_IN` / `CHECKED_OUT` / `RELEASED` |

### 2.3 团队订单

- `Reservation.reservation_type = GROUP` 时关联多条 `ReservationRoom`（每间房一条），并关联「游客名单」（订单中心第 6 节团队订单能力，名单与导游带队凭证由 order-service 管理，hotel-pms-service 仅持有 `reservation_room_id ↔ guest_name` 的轻量映射用于排房和入住登记）。
- 团队订单的房晚预占按 `ReservationRoom` 逐条走 1.3 流程，互不影响（某一间订不到不影响其他间，由前台与客户协商是否整单取消）。

### 2.4 预订状态机（Reservation.status）

```
                    ┌───────────────────┐
   创建预订+多夜预占  │      PENDING       │
   ────────────────▶│ (待确认/支付,可超时) │
                    └─────────┬──────────┘
              确认成功(支付/协议单位免预付)│        │ 超时/取消
                                ▼        ▼
                    ┌───────────────────┐  ┌───────────┐
                    │      CONFIRMED     │  │ CANCELLED │ (终态，释放各间夜预占)
                    └─────────┬──────────┘  └───────────┘
                  入住登记(任一ReservationRoom)│       │ 抵达日已过 + 未入住
                                ▼               ▼
                    ┌───────────────────┐  ┌───────────┐
                    │     CHECKED_IN     │  │  NO_SHOW  │ (终态，释放未入住间夜预占，
                    └─────────┬──────────┘  └───────────┘  按取消政策可扣首晚房费)
                       全部 ReservationRoom │
                       退房结账完成          │
                                ▼
                    ┌───────────────────┐
                    │    CHECKED_OUT     │ (账务待结)
                    └─────────┬──────────┘
                     Folio.balance == 0 │
                                ▼
                    ┌───────────────────┐
                    │     COMPLETED      │ (终态)
                    └────────────────────┘
```

- `PENDING → CONFIRMED`：触发 1.3 中 N×M 个间夜预占的 `ConfirmReservation`（M=`ReservationRoom` 数量）；协议单位订单（`source=CORPORATE`）可直接 `PENDING→CONFIRMED` 免支付，账务走挂账（见 4.5）。
- `PENDING → CANCELLED`：释放全部间夜预占（`ReleaseReservation`）。
- `CONFIRMED → NO_SHOW`：由定时任务扫描 `check_in_date` 当日 24:00（或酒店自定义保留时间）仍未入住的 `ReservationRoom`，释放对应间夜预占；按 `cancellation_policy_snapshot` 决定是否从 `deposit_amount` 扣首晚房费（生成 `FolioItem`，见 4.2）。
- `CONFIRMED → CHECKED_IN`：见三、入住登记。
- `CHECKED_IN → CHECKED_OUT → COMPLETED`：见四、退房结账与夜审。

---

## 三、房态与客房部

### 3.1 聚合根：RoomUnit（具体房间）

| 字段 | 类型 | 说明 |
|---|---|---|
| `room_id` | bigint (PK) | |
| `tenant_id` / `hotel_id` | bigint | |
| `room_type_sku_id` | bigint | 所属房型 |
| `room_no` | varchar | 房号 |
| `floor` | varchar | 楼层 |
| `status` | enum | 见 3.2 房态机 |
| `current_reservation_room_id` | bigint, nullable | 在住状态下指向当前占用的 `ReservationRoom` |

### 3.2 房态机（RoomUnit.status）

```
              ┌──────┐  排房+入住登记   ┌──────┐
   ┌─────────▶│  VC  │────────────────▶│  OC  │
   │ 查房通过  │空净   │                  │在住   │
   │          └──┬───┘                  └───┬──┘
   │             │ 标记维修                   │ 退房
   │             ▼                           ▼
   │          ┌──────┐  维修完成        ┌──────┐
   │   ┌──────│ OOO  │◀────────────────│  VD  │
   │   │维修完成 │维修  │  标记维修       │空脏   │
   │   │       └──────┘                 └──┬───┘
   │   │                                    │ 打扫完成
   │   │                                    ▼
   │   │                              ┌──────────┐
   │   └─────────────────────────────▶│INSPECTING│
   │                  查房不通过(回VD)   │  待查房   │
   └──────────────────────────────────└──────────┘

任意状态 ──锁房──▶ OOS(锁房，不计入 1.2 total_quota) ──解锁──▶ VD(需重新走打扫流程)
```

- `VC`(空净)：唯一可排房/计入可售 `total_quota` 的状态。
- `OC`(在住) / `OOO`(维修) / `OOS`(锁房)：均**不计入** 1.2 `room_inventory_bucket.total_quota`；`RoomUnit` 状态变化时，hotel-pms-service 定时（或事件触发）重新统计各房型各日的 `VC+OC+将于该日退房转为可售` 数量，同步更新 `room_inventory_bucket.total_quota`。
- 锁房（OOS）常用于：装修、长期停售、临时管控（如重大活动期间预留）。

### 3.3 排房（Room Assignment）

| 字段 | 类型 | 说明 |
|---|---|---|
| `assignment_id` | bigint (PK) | |
| `reservation_room_id` | bigint | |
| `room_id` | bigint | |
| `assigned_at` | datetime | |

- **校验**：`room_id` 在 `[check_in_date, check_out_date)` 区间内无其他 `CONFIRMED`/`CHECKED_IN` 状态的排房记录与之冲突（唯一约束：同房间同日期区间不可重叠分配）。
- **时机**：可在预订时（C 端选房）或入住前由前台排房；预订时未排房的 `ReservationRoom.room_id` 为空，入住登记时必须完成排房才能 `CHECKED_IN`。
- **换房（Room Move）**：变更 `room_id`（原 `assignment` 置失效 + 新建一条），原房间转 `OC→VD`（如提前退房）或保持 `OC`（如仍由其他预订占用，理论上不应发生），新房间 `VC→OC`；若新旧房型价格不同，Folio 按变更后实际占用天数分段计费（见 4.4）。

### 3.4 客房部：HousekeepingTask

| 字段 | 类型 | 说明 |
|---|---|---|
| `task_id` | bigint (PK) | |
| `room_id` | bigint | |
| `task_type` | enum | `CHECKOUT_CLEAN`(退房清扫) / `STAYOVER_CLEAN`(在住整理) / `INSPECTION`(查房) / `MAINTENANCE`(维修) |
| `status` | enum | `PENDING` → `IN_PROGRESS` → `DONE`；`INSPECTION` 类型额外有 `PASS`/`FAIL` |
| `assigned_to` | bigint | 客房服务员/工程部人员 |
| `triggered_by` | enum | `AUTO_CHECKOUT`(退房自动生成) / `MANUAL`(前台手动指派) |

- 退房（`RoomUnit: OC→VD`）自动生成 `CHECKOUT_CLEAN` 任务；任务 `DONE` 后自动生成 `INSPECTION` 任务；`INSPECTION.PASS` → `RoomUnit: VD→VC`（计入可售），`FAIL` → 退回 `VD` 并重新生成 `CHECKOUT_CLEAN`。
- `MAINTENANCE` 任务 `DONE` → `RoomUnit: OOO→VD`。

---

## 四、前台账务服务（Folio）

### 4.1 聚合根：Folio（账单）

| 字段 | 类型 | 说明 |
|---|---|---|
| `folio_id` | bigint (PK) | |
| `reservation_id` | bigint | 与 `Reservation` 一一对应（v1，团队订单的多间房共享一个 Folio，按 `ReservationRoom` 维度可在 `FolioItem` 上区分） |
| `guest_profile_id` | bigint | |
| `status` | enum | `OPEN` → `CLOSED` |
| `balance` | decimal | 应付余额 = `Σ(费用类 FolioItem) - Σ(支付类 FolioItem)`，`CLOSED` 时必须为 0 |

### 4.2 实体：FolioItem（账目明细）

| 字段 | 类型 | 说明 |
|---|---|---|
| `folio_item_id` | bigint (PK) | |
| `folio_id` | bigint | |
| `reservation_room_id` | bigint, nullable | 团队订单区分房间 |
| `item_type` | enum | `ROOM_CHARGE`(房费) / `DEPOSIT`(押金) / `DEPOSIT_REFUND`(押金退还) / `ROOM_SERVICE`(客房送餐) / `MEETING_ROOM`(会议室) / `MISC`(杂项) / `PAYMENT`(收款) / `CITY_LEDGER`(转挂账) |
| `amount` | decimal | 费用类为正，支付/退款类为负（统一以「对 balance 的影响」为符号） |
| `posted_at` | datetime | |
| `posted_by` | enum | `SYSTEM`(夜审自动入账) / `STAFF`(前台手工) |
| `source_ref` | varchar, nullable | 关联来源单据（如 Room Service 的餐饮订单号、会议室 `MeetingRoomBooking.booking_id`） |

### 4.3 入住登记（Check-in）

1. 校验 `Reservation.status = CONFIRMED`，所有 `ReservationRoom` 已完成排房（3.3）。
2. ⚠️ **公安旅业治安管理系统对接**：采集身份证信息（读卡器/拍照 OCR），调用治安系统住宿登记接口；失败时按本地合规策略决定是否阻断入住（v1 默认不阻断，记录异常待补录）。
3. 收取押金：生成 `FolioItem(DEPOSIT)`；押金支付走 payment-service（07/08 文档的支付分账体系，押金本质是一笔 `PaymentOrder`，渠道为微信/支付宝预授权或现金/银行卡刷卡，**纳入第 7 节统一对账**）。
4. 门锁发卡：调用硬件接入平台门锁适配层 `IssueRoomKey(room_id, valid_from=check_in, valid_to=check_out)`（详见七）。
5. 状态更新：`ReservationRoom: CONFIRMED→CHECKED_IN`；`RoomUnit: VC→OC`，`current_reservation_room_id` 置位；若该 `Reservation` 下所有 `ReservationRoom` 均已入住（或第一间入住即可，按酒店策略），`Reservation: CONFIRMED→CHECKED_IN`。
6. 创建 `Folio(status=OPEN)`（若不存在）。

### 4.4 夜审（Night Audit）

> 酒店 PMS 的标志性批处理：每个「营业日」结束时，将当晚所有在住房间的房费计入账单，并将系统「营业日」推进到下一天。

| 字段（Hotel 配置） | 类型 | 说明 |
|---|---|---|
| `business_date` | date | 当前营业日，夜审完成后 +1 |

**夜审流程**（定时任务，凌晨业务低谷期执行）：

```
for each RoomUnit where status = OC:
    reservation_room := current_reservation_room_id 对应的 ReservationRoom
    rate := rate_plan_snapshot 中 business_date 当晚单价
    生成 FolioItem(ROOM_CHARGE, amount=rate, posted_by=SYSTEM, posted_at=business_date)
    若 1.3 中对应间夜的 InventoryReservation 状态为 CONFIRMED → 标记为「已消耗」（仅用于审计，不改变库存语义）

business_date += 1
```

- 夜审是 `ROOM_CHARGE` 唯一的入账来源（不允许前台手工补录房费，避免与夜审重复计费；若需调整以 `MISC` 类型走调账审批，见 4.6 移动管理端）。
- 夜审失败（如某间房状态异常）不阻塞整体批处理，逐房记录异常，进入次日「异常订单处理工作台」（功能清单第 6 节）人工处理。

### 4.5 退房结账（Check-out）

1. 触发最后一晚的夜审（若退房时间早于次日夜审批处理，需即时为「今晚」补一条 `ROOM_CHARGE`）。
2. 汇总 `Folio.balance`：
   - `balance > 0`：现结（POS/扫码，走 payment-service）或对协议单位 `CorporateAccount` **挂账**（生成 `FolioItem(CITY_LEDGER)`，`balance` 清零，应收账款转入 `CorporateAccount` 周期账单，见五、5.1）。
   - `balance < 0`（押金多收）：生成 `FolioItem(DEPOSIT_REFUND)`，原路退回。
3. `Folio.status = CLOSED`；`ReservationRoom: CHECKED_IN→CHECKED_OUT`；门锁吊销（`RevokeRoomKey`）；`RoomUnit: OC→VD` 并自动生成 `HousekeepingTask(CHECKOUT_CLEAN)`（3.4）；1.3 中该 `ReservationRoom` 对应间夜预占在夜审中已逐晚 `ConfirmReservation`（即"已售"），无需释放。
4. 全部 `ReservationRoom` 均 `CHECKED_OUT` 且 `Folio.balance == 0` → `Reservation: CHECKED_OUT→COMPLETED`。

### 4.6 移动管理端（别样红「微 PMS」对标）

- 不新增聚合，是 4.1~4.5 各操作（改价、调账/冲账、审批挂账、查看经营日报）的**移动端入口**；调账/冲账操作生成 `FolioItem(item_type=MISC)` 并要求 `posted_by=STAFF` + 审批人字段（审批流复用订单中心「异常订单处理工作台」的通用工单机制，不在本服务重复建模）。

---

## 五、协议单位与单一客史

### 5.1 聚合根：CorporateAccount（协议单位）

| 字段 | 类型 | 说明 |
|---|---|---|
| `corporate_account_id` | bigint (PK) | |
| `tenant_id` / `hotel_id` | bigint | |
| `name` | varchar | 企业/旅行社名称 |
| `contract_rate_plan_id` | bigint | 关联价格中心协议价（`PriceCalendar.price_type = CORPORATE`，08 文档 2.1 在 v1 基础上新增的价格类型） |
| `credit_limit` | decimal | 挂账信用额度 |
| `current_balance` | decimal | 当前挂账余额（Σ未结算的 `FolioItem(CITY_LEDGER)`） |
| `billing_cycle` | enum | `MONTHLY` / `WEEKLY`，定期生成账单供对账 |

- `current_balance` 超过 `credit_limit` 时，新预订的 `source=CORPORATE` 订单不允许 `PENDING→CONFIRMED` 免预付（需现付或人工审批放行）。
- 周期账单生成后进入第 7 节「三方日对账」体系，作为协议单位的应收账款。

### 5.2 GuestProfile（单一客史，member-service 扩展）

| 字段 | 类型 | 说明 |
|---|---|---|
| `guest_profile_id` | bigint (PK) | |
| `tenant_id` | bigint | 集团级（与 ADR-009 集团通会员一致维度） |
| `user_id` | bigint, nullable | 关联 08 文档 `MemberAccount.user_id`（C 端注册用户）；前台登记的散客若未注册会员可为空，后续可「认领」合并 |
| `id_card_no_encrypted` | varchar | 加密存储（个保法合规） |
| `preferences` | json | 偏好（楼层、床型、枕头类型等） |
| `name` / `phone` | varchar | |

- **跨业态聚合视图**（非独立表，查询时聚合）：`stay_history`（本服务 `Reservation` 列表）+ 餐饮消费记录（第 10 节）+ 门票购买记录（07 文档 `Order`），均以 `guest_profile_id ↔ user_id ↔ MemberAccount.user_id` 关联，前台/餐饮/会员触点共享同一档案，对外提供 `GetGuestProfile(guest_profile_id) -> {基础信息, 偏好, 跨业态消费摘要}` 聚合查询接口（v1 简单聚合查询，不做实时数据同步，二期视性能需要引入只读视图/数据中台）。

---

## 六、会议室预订（基础，ADR-020）

### 6.1 聚合根：MeetingRoomResource

| 字段 | 类型 | 说明 |
|---|---|---|
| `meeting_room_sku_id` | bigint (PK) | 商品中心 SKU（复用「场次库存」模型，01 文档五种库存模型之一） |
| `tenant_id` / `hotel_id` | bigint | |
| `name` / `capacity` | - | |
| `hourly_rate` | decimal | 价格中心维护，按小时/半天/全天定价 |

### 6.2 实体：MeetingRoomBooking

| 字段 | 类型 | 说明 |
|---|---|---|
| `booking_id` | bigint (PK) | |
| `meeting_room_sku_id` | bigint | |
| `start_time` / `end_time` | datetime | |
| `status` | enum | `HOLDING` / `CONFIRMED` / `CANCELLED` / `COMPLETED` |
| `reservation_id` | bigint, nullable | 若由入住客人预订并挂房账，关联 `Reservation`；独立客户（非住客）预订则为空，独立结算（生成独立 `Folio`） |
| `folio_item_id` | bigint, nullable | 挂账后对应 4.2 的 `FolioItem(MEETING_ROOM)` |

- 库存模型为「场次库存」：按 ADR-025，独立建表 `meeting_room_inventory_bucket`，维度为 `(meeting_room_sku_id, date, time_slot_id)`，时段颗粒度可配置（如 1 小时一档），字段范式与预占/确认/释放复用 07 文档 `inventory_bucket` 设计（`inventory-kernel`），**不涉及 1.3 的多夜原子预占**（场次库存天然是单桶操作）。
- v1 不含完整 MICE（销售线索、报价审批、活动执行单），仅资源建模 + 预订下单。

---

## 七、门锁对接与 Room Service 集成（轻量，防腐层）

### 7.1 门锁对接（硬件接入平台适配层，ADR-016/ADR-022）

- 接口（hotel-pms-service → 硬件接入平台门锁适配层）：
  - `IssueRoomKey(room_id, reservation_room_id, valid_from, valid_to) -> key_credential`：入住登记时调用，多品牌门锁厂商通过适配器模式屏蔽协议差异。
  - `RevokeRoomKey(room_id, reservation_room_id)`：退房/换房时调用，旧凭证失效。
  - `IssueMeetingRoomKey`（可选，复用同一接口，`valid_from/valid_to` 为会议时段）。
- 门锁设备本身遵循 ADR-022 SN 白名单机制，由硬件接入平台统一管理设备身份；hotel-pms-service 不感知设备协议细节。

### 7.2 客房送餐（Room Service，绿云 iCater 对标）

```
住客在客房通过小程序/H5 下单（餐饮零售收银，第 10 节）
        │ 下单时携带 reservation_room_id（房间号校验在住状态）
        ▼
餐饮零售收银生成订单并出餐 ──完成事件(订单号, 金额)──▶ hotel-pms-service
        │
        ▼
生成 FolioItem(ROOM_SERVICE, amount, source_ref=餐饮订单号)，挂入对应 Folio（4.2）
```

- 餐饮零售收银侧仍按其自身订单状态机闭环（出餐/结账），「挂房账」仅是其结账方式之一（类比微信/支付宝，新增 `ROOM_CHARGE_TO_FOLIO` 支付方式，金额置 0 实收、由 hotel-pms-service 侧入账）。
- 若退房时该笔 Room Service 尚未出餐完成（如刚下单即办退房），由前台人工核实是否允许退房或要求现结，v1 不做自动阻断。

---

## 八、跨服务集成时序图（预订 → 入住 → 在住 → 退房 → 夜审）

```
C端/OTA/前台      hotel-pms-service     room_inventory(内部表) payment-service   housekeeping  hardware(门锁)
 │  创建预订          │                       │                  │               │             │
 ├──────────────────▶│ 1.3 多夜原子预占       │                  │               │             │
 │                    ├──────────────────────▶│ N×CreateReservation              │             │
 │                    │◀──────────────────────┤ 全部HOLDING(或失败回滚)           │             │
 │  预订(PENDING)     │                       │                  │               │             │
 │◀───────────────────┤                       │                  │               │             │
 │  支付/协议单位免预付  │                       │                  │               │             │
 ├────────────────────┼───────────────────────┼─────────────────▶│ 创建支付单     │             │
 │                    │◀──────────────PaymentSucceeded(event)──────┤              │             │
 │                    │ N×ConfirmReservation  │                  │               │             │
 │                    ├──────────────────────▶│ 全部CONFIRMED     │               │             │
 │  预订(CONFIRMED)    │                       │                  │               │             │
 │◀───────────────────┤                       │                  │               │             │
 │  抵达办理入住        │                       │                  │               │             │
 ├───────────────────▶│ 公安系统上报+收押金     │                  │               │             │
 │                    ├───────────────────────┼─────────────────▶│ 押金PaymentOrder│            │
 │                    ├──────────────────────────────────────────┼──────────────┼────────────▶│ IssueRoomKey
 │  预订(CHECKED_IN)   │  RoomUnit: VC→OC      │                  │               │             │
 │◀───────────────────┤                       │                  │               │             │
 │ (在住期间，每日凌晨)  │                       │                  │               │             │
 │                    │ 4.4 夜审：生成ROOM_CHARGE，business_date+=1                              │
 │                    │ (期间 Room Service 下单 → FolioItem挂房账，见7.2)                         │
 │  退房               │                       │                  │               │             │
 ├───────────────────▶│ 4.5 结清/挂账           │                  │               │             │
 │                    ├───────────────────────┼─────────────────▶│ 现结/退押金     │             │
 │                    ├──────────────────────────────────────────┼──────────────┼────────────▶│ RevokeRoomKey
 │                    │  RoomUnit: OC→VD        │                  │              │             │
 │                    ├──────────────────────────────────────────┼──────────────▶│ CHECKOUT_CLEAN任务
 │  预订(CHECKED_OUT   │                       │                  │               │             │
 │   →COMPLETED)      │                       │                  │               │             │
 │◀───────────────────┤                       │                  │               │             │
```

**关键设计原则**（呼应 07 文档）：
1. **多夜预占在创建预订时同步完成**，且必须保证「全有或全无」（1.3 Saga 补偿），避免半成功的预订占用部分库存却无法成立。
2. **夜审是房费入账的唯一来源**，将「按日计费」的酒店业务转化为离散批处理事件，是 PMS 区别于门票/餐饮订单模型的核心特征。
3. **房态机（3.2）与房晚库存（1.2 `total_quota`）联动但不等价**：房态机管理「物理房间当前能否使用」，库存管理「未来某天该房型还能卖多少」——`OOO`/`OOS` 影响未来库存可售量，`OC`/`VD` 不直接影响（已售）。
4. **离线场景**：门锁发卡/吊销若网络异常，by 设备侧本地白名单兜底（ADR-022），事后补同步，前台入住/退房流程本身不因门锁通信失败而阻断（与核销中心的离线设计原则一致）。

---

## 九、服务依赖关系总览

```
                    ┌──────────────┐
                    │ member-service│◀─────────────────────┐ (GuestProfile 扩展)
                    └──────────────┘                       │
┌───────────────┐   ┌──────────────────┐   ┌──────────────┴────┐
│ product-service│◀──┤                   │   │                    │
└───────────────┘   │ hotel-pms-service  ├──▶│ pricing-service    │
┌───────────────┐   │ (Reservation/      │   │ (含 CORPORATE 价格) │
│ inventory-kernel│ ⇢│  ReservationRoom/  │   └────────────────────┘
│ (共享代码库，    │   │  room_inventory_   │
│  ADR-025；非RPC)│   │  bucket/meeting_   │
└───────────────┘   │  room_inventory_   ├──▶┌────────────────────┐
                     │  bucket/RoomUnit/  │   │ payment-service     │
                     │  Folio/Housekeeping│◀──┤ (押金/房费/挂账对账) │
┌───────────────┐   │  /CorporateAccount)│   └────────────────────┘
│  order-service │◀──┤                   │
│ (跨商户购物车)   │   │                   ├──▶┌────────────────────┐
└───────────────┘   │                   │◀──┤ 餐饮零售收银(第10节) │
┌───────────────┐   │                   │   │ (Room Service)      │
│硬件接入平台门锁  │◀──┤                   │   └────────────────────┘
│适配层(第13节)   │   └──────────────────┘
└───────────────┘
```

`inventory-kernel` 不是一个独立的可调用服务，而是被 inventory-service（门票域）与 hotel-pms-service（房晚/会议室库存）共同依赖的代码库（防超卖两道防线 + 预占状态机的可复用实现，ADR-025）；图中以 `⇢` 区分于服务间 RPC 调用。

`hotel-pms-service` 在酒店域内是编排中心，承担类似 07 文档 order-service 的角色；与 order-service 的关系是**平级协作**（C 端跨业态组合下单时，order-service 拆单后把酒店子项交给 hotel-pms-service 处理预占/预订）。

---

## 十、待下一步细化

- `CreateReservationRoom`（含 1.3 多夜 Saga）/ `AssignRoom` / `CheckIn` / `CheckOut` / `RunNightAudit` 的 OpenAPI 契约
- `ReservationConfirmed` / `RoomStatusChanged` / `FolioItemPosted` 的 MQ 事件 Schema
- `reservation` / `reservation_room` / `room_unit` / `housekeeping_task` / `folio` / `folio_item` / `corporate_account` / `meeting_room_booking` 表结构 DDL（含分库分表 key、`room_id` 时间区间不重叠的唯一约束实现方式）
- 1.3 中「N 晚部分失败」场景下，是否向用户提示「可选邻近房型」的体验细化（v1 仅返回失败，二期增强）
