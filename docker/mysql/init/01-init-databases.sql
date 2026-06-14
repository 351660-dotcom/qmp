-- 门票全链路 7 个服务各自独立 database（同 MySQL 实例），对应 docs/10-门票全链路数据库设计.md「一、通用约定」
-- 各服务通过 Flyway（db/migration/V1__init.sql）在各自 database 内建表，此处只负责建库。
CREATE DATABASE IF NOT EXISTS product_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pricing_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS member_db              DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db             DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ticket_verification_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db               DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 其它业态链路（按 ADR-025 各自独立建库，复用 inventory-kernel 设计模式）
CREATE DATABASE IF NOT EXISTS hotel_db               DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS marketing_db           DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dining_db              DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS supply_chain_db        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS performance_db         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
